package top.quantic.sentry.discord;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.*;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.core.CommandContext;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.util.DiscordUtil;
import top.quantic.sentry.service.PermissionService;
import top.quantic.sentry.service.SettingService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.quantic.sentry.config.Operations.QUERY_ALL_GUILDS;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Component
public class Self implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Self.class);

    private final SettingService settingService;
    private final PermissionService permissionService;

    @Autowired
    public Self(SettingService settingService, PermissionService permissionService) {
        this.settingService = settingService;
        this.permissionService = permissionService;
    }

    @Override
    public List<Command> getCommands() {
        return asList(unsay(), profile(), prefix());
    }

    private Command prefix() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nonOptSpec = parser.nonOptions("A series of prefixes to use").ofType(String.class);
        OptionSpec<Void> appendSpec = parser.accepts("append", "Append the new prefixes instead of replacing");
        return CommandBuilder.of("prefix")
            .describedAs("View or edit the prefix used in this server")
            .in("Bot")
            .parsedBy(parser)
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                OptionSet o = context.getOptionSet();
                Set<String> prefixesToSet = o.valuesOf(nonOptSpec).stream()
                    .collect(Collectors.toSet());
                if (prefixesToSet.isEmpty()) {
                    replyWithPrefixes(message);
                } else {
                    settingService.setPrefixes(message, prefixesToSet, o.has(appendSpec));
                }
            })
            .onAuthorDenied(context -> replyWithPrefixes(context.getMessage())).build();
    }

    private void replyWithPrefixes(IMessage message) {
        answerPrivately(message, "Current prefixes: " + settingService.getPrefixes(message).stream()
            .map(p -> "`" + p + "`")
            .collect(Collectors.joining(" ")));
    }

    private Command profile() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nameSpec = parser.accepts("name", "New name for this bot").withRequiredArg();
        OptionSpec<String> avatarSpec = parser.accepts("avatar", "URL or User ID, name, or mention to the new avatar for this bot").withRequiredArg();
        OptionSpec<String> formatSpec = parser.accepts("format", "Override the format of a supplied image").withRequiredArg();
        OptionSpec<String> gameSpec = parser.accepts("game", "Set a new game status message").withRequiredArg();
        OptionSpec<String> streamSpec = parser.accepts("stream", "Set a new stream status message").withRequiredArg();
        OptionSpec<String> urlSpec = parser.accepts("url", "Set a new stream status URL").requiredIf(streamSpec).withRequiredArg();
        OptionSpec<String> presenceSpec = parser.acceptsAll(asList("as", "presence"), "Set the new presence of this bot (online, idle)").withRequiredArg();
        OptionSpec<Void> emptySpec = parser.accepts("empty", "Reset the status message");
        return CommandBuilder.of("profile")
            .describedAs("Edit this bot's profile")
            .in("Bot")
            .parsedBy(parser)
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                IDiscordClient client = message.getClient();
                OptionSet o = context.getOptionSet();
                if (o.has(nameSpec)) {
                    RequestBuffer.request(() -> {
                        try {
                            client.changeUsername(o.valueOf(nameSpec));
                        } catch (DiscordException e) {
                            log.warn("Could not change username", e);
                            answerPrivately(message, "Could not change username: " + e.getErrorMessage());
                        }
                    });
                }
                if (o.has(avatarSpec)) {
                    if (client.getOurUser().getAvatar() != null) {
                        try (InputStream oldStream = avatarAsInputStream(client.getOurUser().getAvatarURL())) {
                            answerPrivatelyWithFile(message, "Old avatar for backup purposes", oldStream,
                                "avatar.png").get();
                        } catch (IOException e) {
                            log.warn("Could not send previous avatar", e);
                        }
                    } else {
                        log.debug("Default avatar found - no backup done");
                    }

                    String query = o.valueOf(avatarSpec);
                    boolean aware = permissionService.hasPermission(message, QUERY_ALL_GUILDS, "*");
                    IChannel channel = message.getChannel();

                    String id = query.replaceAll("<@!?(\\d+)>", "$1");
                    List<IUser> users = awareUserList(aware, message);
                    List<IUser> matching = users.stream()
                        .filter(u -> u.getID().equals(id) || equalsAnyName(u, query, channel.getGuild()))
                        .distinct()
                        .collect(Collectors.toList());
                    if (matching.size() == 1) {
                        IUser user = matching.get(0);
                        log.debug("Getting avatar from {}", humanize(user));
                        RequestBuffer.request(() -> {
                            try {
                                client.changeAvatar(Image.forUser(user));
                            } catch (DiscordException e) {
                                log.warn("Could not change avatar", e);
                                answerPrivately(message, "Could not change avatar: " + e.getErrorMessage());
                            }
                        });
                    } else if (matching.size() > 1) {
                        answerPrivately(message, "Multiple user matches for " + query + "\n"
                            + matching.stream()
                            .map(DiscordUtil::humanizeShort)
                            .collect(Collectors.joining("\n")));
                    } else {
                        // try as URL
                        RequestBuffer.request(() -> {
                            try {
                                String format = getFormat(query, o.valueOf(formatSpec));
                                client.changeAvatar(Image.forUrl(format, query));
                            } catch (DiscordException e) {
                                log.warn("Could not change avatar", e);
                                answerPrivately(message, "Could not change avatar: " + e.getErrorMessage());
                            }
                        });
                    }
                }
                if (o.has(gameSpec)) {
                    client.online(o.valueOf(gameSpec));
                } else if (o.has(urlSpec)) {
                    client.streaming(o.valueOf(streamSpec), o.valueOf(urlSpec));
                } else if (o.has(emptySpec)) {
                    client.online();
                }
                if (o.has(presenceSpec)) {
                    String presence = o.valueOf(presenceSpec);
                    switch (presence.toLowerCase()) {
                        case "online":
                            client.online();
                            break;
                        case "idle":
                            client.idle();
                            break;
                        default:
                            answerPrivately(message, "Invalid presence - use either online or idle");
                    }
                }
            }).build();
    }

    private String getFormat(String avatar, String format) {
        String result = format;
        if (format == null) {
            result = FilenameUtils.getExtension(avatar);
            if (isBlank(result)) {
                result = "png";
            }
        }
        return result;
    }

    private InputStream avatarAsInputStream(String urlStr) throws IOException {
        URLConnection urlConnection = new URL(urlStr).openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
        return urlConnection.getInputStream();
    }

    private Command unsay() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nonOptSpec = parser.nonOptions("Number of messages to delete").ofType(String.class);
        return CommandBuilder.of("unsay")
            .describedAs("Remove bot's latest messages in this channel")
            .in("Bot")
            .parsedBy(parser)
            .onExecute(context -> {
                List<String> nonOptions = context.getOptionSet().valuesOf(nonOptSpec);
                int limit = 1;
                if (!nonOptions.isEmpty()) {
                    String arg = nonOptions.get(0);
                    if (arg.matches("[0-9]+")) {
                        try {
                            limit = Integer.parseInt(arg);
                        } catch (NumberFormatException e) {
                            log.warn("{} tried to input {} as limit", humanize(context.getMessage().getAuthor()), arg);
                        }
                    }
                }
                deleteLastMessages(context, limit);
            }).build();
    }

    private void deleteLastMessages(CommandContext context, int limit) {
        int maxDepth = 1000;
        limit = Math.min(maxDepth, limit);
        IMessage message = context.getMessage();
        IDiscordClient client = message.getClient();
        IChannel c = client.getChannelByID(message.getChannel().getID());
        if (c != null) {
            log.info("Preparing to delete latest {} to channel {}", inflect(limit, "bot message"), c.getName());
            int cap = c.getMessages().getCacheCapacity();
            c.getMessages().setCacheCapacity(MessageList.UNLIMITED_CAPACITY);
            int deleted = 0;
            int i = 0;
            List<IMessage> toDelete = new ArrayList<>();
            while (deleted < limit && i < maxDepth) {
                try {
                    IMessage msg = c.getMessages().get(i++);
                    if (msg.getAuthor().equals(client.getOurUser())) {
                        toDelete.add(msg);
                        deleted++;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    // we reached the end apparently
                    log.warn("Could not retrieve messages to delete", e);
                    break;
                }
            }
            if (c.isPrivate() || toDelete.size() == 1) {
                AtomicBoolean abort = new AtomicBoolean(false);
                for (IMessage delete : toDelete) {
                    if (abort.get()) {
                        break;
                    }
                    RequestBuffer.request(() -> {
                        try {
                            delete.delete();
                        } catch (MissingPermissionsException | DiscordException e) {
                            log.warn("Could not delete message - aborting", e);
                            abort.set(true);
                        }
                    }).get();
                }
            } else {
                deleteInBatch(c, toDelete);
            }
            c.getMessages().setCacheCapacity(cap);
        }
    }
}
