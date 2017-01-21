package top.quantic.sentry.discord;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Status;
import sx.blah.discord.util.*;
import top.quantic.sentry.discord.command.Command;
import top.quantic.sentry.discord.command.CommandBuilder;
import top.quantic.sentry.discord.command.CommandContext;
import top.quantic.sentry.discord.module.CommandSupplier;
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
import static top.quantic.sentry.discord.util.DiscordLimiter.acquireDelete;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Component
public class Self implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Self.class);

    private final SettingService settingService;

    @Autowired
    public Self(SettingService settingService) {
        this.settingService = settingService;
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
        OptionSpec<String> avatarSpec = parser.accepts("avatar", "URL to the new avatar for this bot").withRequiredArg();
        OptionSpec<String> gameSpec = parser.accepts("game", "Set a new game status message").withRequiredArg();
        OptionSpec<String> streamSpec = parser.accepts("stream", "Set a new stream status message").withRequiredArg();
        OptionSpec<String> urlSpec = parser.accepts("url", "Set a new stream status URL").requiredIf(streamSpec).withRequiredArg();
        OptionSpec<String> presenceSpec = parser.acceptsAll(asList("as", "presence"), "Set the new presence of this bot (online, idle)").withRequiredArg();
        OptionSpec<Void> emptySpec = parser.accepts("empty", "Reset the current status to empty");
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
                    try (InputStream newStream = avatarAsInputStream(o.valueOf(avatarSpec))) {
                        try (InputStream oldStream = avatarAsInputStream(client.getOurUser().getAvatarURL())) {
                            answerPrivatelyWithFile(message, "Old avatar for backup purposes", oldStream, "avatar.jpg");
                        } catch (IOException e) {
                            log.warn("Could not send previous avatar", e);
                        }
                        RequestBuffer.request(() -> {
                            try {
                                client.changeAvatar(Image.forStream("jpeg", newStream));
                            } catch (DiscordException e) {
                                log.warn("Could not change avatar", e);
                                answerPrivately(message, "Could not change avatar: " + e.getErrorMessage());
                            }
                        });
                    } catch (IOException e) {
                        log.warn("Could not set new avatar", e);
                        answerPrivately(message, "Could not change avatar: " + e.getMessage());
                    }
                }
                if (o.has(gameSpec)) {
                    client.changeStatus(Status.game(o.valueOf(gameSpec)));
                } else if (o.has(urlSpec)) {
                    client.changeStatus(Status.stream(o.valueOf(streamSpec), o.valueOf(urlSpec)));
                } else if (o.has(emptySpec)) {
                    client.changeStatus(Status.empty());
                }
                if (o.has(presenceSpec)) {
                    String presence = o.valueOf(presenceSpec);
                    switch (presence.toLowerCase()) {
                        case "online":
                            client.changePresence(false);
                            break;
                        case "idle":
                            client.changePresence(true);
                            break;
                        default:
                            answerPrivately(message, "Invalid presence - use either online or idle");
                    }
                }
            }).build();
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
                            acquireDelete();
                            delete.delete();
                        } catch (MissingPermissionsException | DiscordException e) {
                            log.warn("Could not delete message - aborting", e);
                            abort.set(true);
                        }
                    });
                }
            } else {
                deleteInBatch(c, toDelete);
            }
            c.getMessages().setCacheCapacity(cap);
        }
    }
}
