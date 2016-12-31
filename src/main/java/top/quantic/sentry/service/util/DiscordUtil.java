package top.quantic.sentry.service.util;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static top.quantic.sentry.service.util.DiscordLimiter.acquire;
import static top.quantic.sentry.service.util.DiscordLimiter.acquireDelete;
import static top.quantic.sentry.service.util.MessageSplitter.LENGTH_LIMIT;

public class DiscordUtil {

    private static final Logger log = LoggerFactory.getLogger(DiscordUtil.class);

    public static Set<String> getRoles(IUser user) {
        return getRoles(user, null);
    }

    public static Set<String> getRoles(IUser user, IGuild guild) {
        if (user == null) {
            return Collections.emptySet();
        } else if (guild == null) {
            return Collections.singleton(user.getID());
        } else {
            Set<String> roleSet = Sets.newHashSet(user.getID());
            roleSet.addAll(user.getRolesForGuild(guild).stream()
                .map(IDiscordObject::getID)
                .collect(Collectors.toList()));
            return roleSet;
        }
    }

    public static String humanize(IMessage message) {
        return String.format("[%s] %s: %s", humanize(message.getChannel()), humanize(message.getAuthor()), message.getContent());
    }

    public static String humanize(IUser user) {
        return String.format("%s#%s (%s)", user.getName(), user.getDiscriminator(), user.getID());
    }

    public static String humanize(IChannel channel) {
        if (channel.isPrivate()) {
            return String.format("*PM* (%s)", channel.getID());
        } else {
            return String.format("%s/%s (%s)", channel.getGuild().getName(), channel.getName(), channel.getID());
        }
    }

    public static String humanize(IGuild guild) {
        return String.format("%s (%s)", guild.getName(), guild.getID());
    }

    public static String humanize(IRole role) {
        return String.format("%s/%s (%s)", role.getGuild().getName(), role.getName().replace("@", "@\u200B"), role.getID());
    }

    public static void deleteInBatch(IChannel channel, List<IMessage> toDelete) {
        if (toDelete.isEmpty()) {
            log.info("No messages to delete");
        } else {
            log.info("Preparing to delete {} messages from {}", toDelete.size(), humanize(channel));
            for (int x = 0; x < (toDelete.size() / 100) + 1; x++) {
                List<IMessage> subList = toDelete.subList(x * 100, Math.min(toDelete.size(), (x + 1) * 100));
                RequestBuffer.request(() -> {
                    try {
                        acquireDelete();
                        channel.getMessages().bulkDelete(subList);
                    } catch (MissingPermissionsException | DiscordException e) {
                        log.warn("Failed to delete message", e);
                    }
                    return null;
                });
            }
        }
    }

    public static void answer(IMessage to, String content) {
        answer(to, content, false);
    }

    public static void answer(IMessage to, String content, boolean tts) {
        // TODO: In D4J v2.7, using RequestBuffer does not allow us to reliably retrieve the resulting IMessage
        // if that behavior is needed, don't use this convenience method
        answerToChannel(to.getChannel(), content, tts);
    }

    public static void reply(IMessage to, String content) {
        if (content.length() > LENGTH_LIMIT) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(LENGTH_LIMIT);
            for (String split : splits) {
                innerReply(to, split);
            }
        } else {
            innerReply(to, content);
        }
    }

    public static void answerPrivately(IMessage to, String content) {
        answerPrivately(to, content, false);
    }

    public static void answerPrivately(IMessage to, String content, boolean tts) {
        RequestBuffer.request(() -> {
            try {
                answerToChannel(to.getAuthor().getOrCreatePMChannel(), content, tts);
            } catch (DiscordException e) {
                log.warn("[{}] Failed to send PM to {}: {}", to.getClient().getOurUser().getName(),
                    humanize(to.getAuthor()), e);
            }
        });
    }

    public static void answerWithFile(IMessage to, String content, File file) {
        if (content.length() > LENGTH_LIMIT) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(LENGTH_LIMIT);
            for (int i = 0; i < splits.size() - 1; i++) {
                sendMessage(to.getChannel(), splits.get(i), false);
            }
            sendFile(to.getChannel(), splits.get(splits.size() - 1), file);
        } else {
            sendFile(to.getChannel(), content, file);
        }
    }

    public static void answerPrivatelyWithFile(IMessage to, String content, File file) {
        RequestBuffer.request(() -> {
            try {
                answerToChannelWithFile(to.getAuthor().getOrCreatePMChannel(), content, file);
            } catch (DiscordException e) {
                log.warn("[{}] Failed to send PM to {}: {}", to.getClient().getOurUser().getName(),
                    humanize(to.getAuthor()), e);
            }
        });
    }

    private static void answerToChannel(IChannel channel, String content, boolean tts) {
        if (content.length() > LENGTH_LIMIT) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(LENGTH_LIMIT);
            for (String split : splits) {
                sendMessage(channel, split, false);
            }
        } else {
            sendMessage(channel, content, tts);
        }
    }

    private static void answerToChannelWithFile(IChannel channel, String content, File file) {
        if (content.length() > LENGTH_LIMIT) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(LENGTH_LIMIT);
            for (int i = 0; i < splits.size() - 1; i++) {
                sendMessage(channel, splits.get(i), false);
            }
            sendFile(channel, splits.get(splits.size() - 1), file);
        } else {
            sendFile(channel, content, file);
        }
    }

    private static RequestBuffer.RequestFuture<IMessage> sendMessage(IChannel channel, String content, boolean tts) {
        acquire(channel);
        return RequestBuffer.request(() -> {
            try {
                return channel.sendMessage(content, tts);
            } catch (MissingPermissionsException e) {
                log.warn("[{}] Missing permissions in {}: {}", channel.getClient().getOurUser().getName(),
                    humanize(channel), e);
            } catch (DiscordException e) {
                log.warn("[{}] Failed to send message to {}: {}", channel.getClient().getOurUser().getName(),
                    humanize(channel), e);
            }
            return null;
        });
    }

    private static void innerReply(IMessage message, String content) {
        acquire(message);
        RequestBuffer.request(() -> {
            try {
                message.reply(content);
            } catch (MissingPermissionsException e) {
                log.warn("[{}] Missing permissions in {}: {}", message.getClient().getOurUser().getName(),
                    humanize(message.getChannel()), e);
            } catch (DiscordException e) {
                log.warn("[{}] Failed to send message to {}: {}", message.getClient().getOurUser().getName(),
                    humanize(message.getChannel()), e);
            }
        });
    }

    private static RequestBuffer.RequestFuture<IMessage> sendFile(IChannel channel, String content, File file) {
        acquire(channel);
        return RequestBuffer.request(() -> {
            try {
                return channel.sendFile(content, file);
            } catch (MissingPermissionsException e) {
                log.warn("[{}] Missing permissions in {}: {}", channel.getClient().getOurUser().getName(),
                    humanize(channel), e);
            } catch (DiscordException | FileNotFoundException e) {
                log.warn("[{}] Failed to send file to {}: {}", channel.getClient().getOurUser().getName(),
                    humanize(channel), e);
            }
            return null;
        });
    }


    private DiscordUtil() {
    }
}
