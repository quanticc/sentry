package top.quantic.sentry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.core.CommandContext;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.module.DiscordSubscriber;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.service.SettingService;
import top.quantic.sentry.service.util.DateUtil;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.quantic.sentry.discord.util.DiscordUtil.answerToChannel;
import static top.quantic.sentry.discord.util.DiscordUtil.getTrustedChannel;
import static top.quantic.sentry.discord.util.DiscordUtil.humanize;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Component
public class Slow implements CommandSupplier, DiscordSubscriber {

    private static final Logger log = LoggerFactory.getLogger(Slow.class);

    private static final String SLOW_PREFIX = "slow:";
    private static final String PREV_OVERRIDE_KEY = "previousChannelUserOverride";
    private static final String USER_LIMIT_KEY = "channelUserLimitedBefore";

    private final SettingService settingService;

    // Cache of slowed channels (channelId -> minutes)
    private final Map<String, Long> slowRateMap = new ConcurrentHashMap<>();
    // Cache of slowed users (channelId-userId -> timestamp)
    private final Map<String, Long> limitedUsersMap = new ConcurrentHashMap<>();
    // Cache of slow restore futures (channelId-userId -> future)
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public Slow(SettingService settingService) {
        this.settingService = settingService;
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(slow());
    }

    private Command slow() {
        return CommandBuilder.of("slow")
            .describedAs("Turn slow mode on/off")
            .in("Moderation")
            .withExamples("Usage: **slow** [__rate__]\n\nWhere __rate__ is the number of minutes a user has to wait to post again in this channel, 0 to disable.")
            .nonParsed()
            .secured()
            .requires(EnumSet.of(Permissions.MANAGE_PERMISSIONS))
            .onExecute(this::doSlow)
            .onAuthorDenied(CommandBuilder.noPermission())
            .build();
    }

    private synchronized void doSlow(CommandContext context) {
        IMessage message = context.getMessage();
        IChannel channel = message.getChannel();
        IChannel reply = getTrustedChannel(settingService, message);
        String content = context.getContentAfterCommand();
        if (channel.isPrivate()) {
            answerToChannel(reply, "This command does not work in private messages");
            return;
        }
        Setting currentRate = settingService.findMostRecentByGuildAndKey(channel.getGuild().getID(), SLOW_PREFIX + channel.getID())
            .orElseGet(() -> new Setting().guild(channel.getGuild().getID()).key(SLOW_PREFIX + channel.getID()).value("0"));
        long currentMinutes = slowRateMap.getOrDefault(channel.getID(), Long.parseLong(currentRate.getValue()));
        if (isBlank(content)) {
            if (currentMinutes > 0) {
                answerToChannel(reply, "Slow mode: One message to " + channel.mention() + " each " + inflect(currentMinutes, "minute") + " per user.\nUse **slow 0** to disable.");
            } else {
                answerToChannel(reply, "Slow mode is not enabled in " + channel.mention() + ". Use **slow** __minutes__ to enable.");
            }
            return;
        }

        String rate = content.split(" ", 2)[0];
        long updatedMinutes;
        if ("off".equals(rate)) {
            updatedMinutes = 0;
        } else if (rate.matches("[0-9]+")) {
            updatedMinutes = Long.parseLong(rate);
        } else {
            answerToChannel(reply, "Unsupported value, must be a number!");
            return;
        }
        if (updatedMinutes < 0) {
            answerToChannel(reply, "Unsupported value, must be a positive number!");
            return;
        }

        if (updatedMinutes > 0) {
            log.debug("Updating limits for {} to {} per message per user", humanize(channel), inflect(updatedMinutes, "minute"));
            saveLimits();
            loadLimits(message.getClient());
            answerToChannel(reply, "Slow mode: One message to " + channel.mention() + " each " + inflect(updatedMinutes, "minute") + " per user.");
        } else {
            log.debug("Cancelling current limits to {}", humanize(channel));
            futures.entrySet().stream()
                .filter(entry -> channel.getID().equals(entry.getKey().split("-")[0]))
                .forEach(entry -> {
                    String[] args = entry.getKey().split("-");
                    IUser user = channel.getClient().getUserByID(args[1]);
                    boolean result = entry.getValue().cancel(true);
                    log.debug("Future for {} in {} cancelled: {}", humanize(user), humanize(channel), result);
                    removeUserLimitIn(user, channel);
                });
            answerToChannel(reply, "Slow mode disabled for " + channel.mention());
        }
        settingService.updateValue(currentRate, updatedMinutes + "");
        slowRateMap.put(channel.getID(), updatedMinutes);
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        loadLimits(event.getClient());
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event) {
        IChannel channel = event.getChannel();
        IUser author = event.getAuthor();
        if (!channel.isPrivate()) {
            long minutes = slowRateMap.getOrDefault(channel.getID(), 0L);
            if (minutes > 0) {
                limitUserInFor(author, channel, TimeUnit.MINUTES.toMillis(minutes));
            }
        }
    }

    private void loadLimits(IDiscordClient client) {
        limitedUsersMap.clear();
        slowRateMap.clear();
        futures.clear();
        for (Setting setting : settingService.findByGuild(USER_LIMIT_KEY)) {
            String[] args = setting.getKey().split("-");
            IChannel channel = client.getChannelByID(args[0]);
            IUser user = client.getUserByID(args[1]);
            if (channel == null || user == null) {
                log.debug("User {} or channel {} does not exist anymore", args[1], args[0]);
            } else {
                long endTimestamp = Long.parseLong(setting.getValue());
                long remaining = endTimestamp - System.currentTimeMillis();
                if (remaining <= 0) {
                    removeUserLimitIn(user, channel);
                } else {
                    limitUserInFor(user, channel, remaining);
                }
            }
            settingService.delete(setting.getId());
        }
        for (Setting setting : settingService.findByKeyStartingWith(SLOW_PREFIX)) {
            String channelId = setting.getKey().substring(SLOW_PREFIX.length());
            long minutes = Long.parseLong(setting.getValue());
            log.debug("Limiting {} to {}", humanize(client.getChannelByID(channelId)), inflect(minutes, "minute"));
            slowRateMap.put(channelId, minutes);
        }
        for (Setting setting : settingService.findByGuild(PREV_OVERRIDE_KEY)) {
            String[] channelUser = setting.getKey().split("-");
            log.debug("Saving previous overrides for {} in {}", humanize(client.getUserByID(channelUser[1])), humanize(client.getChannelByID(channelUser[0])));
            settingService.delete(setting.getId());
        }
    }

    private void saveLimits() {
        limitedUsersMap.forEach((key, value) -> settingService.createSetting(USER_LIMIT_KEY, key, value + ""));
    }

    private String getChannelUserKey(IChannel channel, IUser user) {
        return channel.getID() + "-" + user.getID();
    }

    private void removeUserLimitIn(IUser user, IChannel channel) {
        try {
            RequestBuffer.request(() -> {
                String key = getChannelUserKey(channel, user);
                Optional<Setting> setting = settingService.findMostRecentByGuildAndKey(PREV_OVERRIDE_KEY, key);
                IChannel.PermissionOverride newUserOverrides = channel.getUserOverrides().get(user.getID());
                if (setting.isPresent()) {
                    String[] allowDeny = setting.get().getValue().split(";");
                    EnumSet<Permissions> previousAllow = Permissions.getAllowedPermissionsForNumber(Integer.parseInt(allowDeny[0]));
                    EnumSet<Permissions> previousDeny = Permissions.getDeniedPermissionsForNumber(Integer.parseInt(allowDeny[1]));
                    if (previousAllow.isEmpty() && previousDeny.isEmpty()) {
                        log.debug("Removing override for {} in {}", humanize(user), humanize(channel));
                        channel.removePermissionsOverride(user);
                    } else {
                        log.debug("Restoring pre-limit permission override for {} in {}", humanize(user), humanize(channel));
                        channel.overrideUserPermissions(user, previousAllow, previousDeny);
                    }
                    settingService.delete(setting.get().getId());
                } else if (newUserOverrides != null) {
                    if (newUserOverrides.allow().isEmpty()
                        && newUserOverrides.deny().size() == 1
                        && newUserOverrides.deny().contains(Permissions.SEND_MESSAGES)) {
                        log.debug("Removing empty permission override from {} in {}", humanize(user), humanize(channel));
                        channel.removePermissionsOverride(user);
                    } else {
                        EnumSet<Permissions> restoredAllow = newUserOverrides.allow().clone();
                        EnumSet<Permissions> restoredDeny = newUserOverrides.deny().clone();
                        restoredAllow.add(Permissions.SEND_MESSAGES);
                        restoredDeny.remove(Permissions.SEND_MESSAGES);
                        log.debug("Allowing send permission to {} in {}", humanize(user), humanize(channel));
                        channel.overrideUserPermissions(user, restoredAllow, restoredDeny);
                    }
                }
                limitedUsersMap.remove(getChannelUserKey(channel, user));
            }).get();
        } catch (Exception e) {
            log.warn("Could not remove permission override", e);
        }
    }

    private void limitUserInFor(IUser user, IChannel channel, long millis) {
        IGuild guild = channel.getGuild();
        if (DiscordUtils.isUserHigher(guild, channel.getClient().getOurUser(), user.getRolesForGuild(guild))) {
            log.debug("Disabling send message permissions of {} for {} in {}",
                humanize(user), DateUtil.humanizeShort(Duration.ofMillis(millis)), humanize(channel));
            // store pre-limit overrides for this user
            IChannel.PermissionOverride userOverrides = channel.getUserOverrides().get(user.getID());
            int allow = userOverrides == null ? 0 : Permissions.generatePermissionsNumber(userOverrides.allow());
            int deny = userOverrides == null ? 0 : Permissions.generatePermissionsNumber(userOverrides.deny());
            String key = getChannelUserKey(channel, user);
            settingService.createSetting(PREV_OVERRIDE_KEY, key, allow + ";" + deny);
            EnumSet<Permissions> newAllow = userOverrides == null ? EnumSet.noneOf(Permissions.class) : userOverrides.allow().clone();
            EnumSet<Permissions> newDeny = userOverrides == null ? EnumSet.noneOf(Permissions.class) : userOverrides.deny().clone();
            newAllow.remove(Permissions.SEND_MESSAGES);
            newDeny.add(Permissions.SEND_MESSAGES);
            RequestBuffer.request(() -> channel.overrideUserPermissions(user, newAllow, newDeny)).get();
            limitedUsersMap.put(key, System.currentTimeMillis() + millis);
            ScheduledFuture<?> future = executorService.schedule(() -> removeUserLimitIn(user, channel), millis, TimeUnit.MILLISECONDS);
            futures.put(key, future);
        } else {
            log.debug("Unable to limit {} because their roles are higher than mine", humanize(user));
        }
    }
}
