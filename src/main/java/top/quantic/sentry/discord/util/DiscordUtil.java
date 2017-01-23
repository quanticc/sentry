package top.quantic.sentry.discord.util;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.discord.core.Command;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.quantic.sentry.discord.util.DiscordLimiter.acquire;
import static top.quantic.sentry.discord.util.DiscordLimiter.acquireDelete;

public class DiscordUtil {

    private static final Logger log = LoggerFactory.getLogger(DiscordUtil.class);

    public static Set<String> getRoles(IUser user) {
        return getRolesWithGuild(user, null);
    }

    /**
     * Get all the role-ids assignable to a user from a Message.
     *
     * @param message the Message to get the ids from
     * @return a Set of role-ids assignable from the Message's author
     */
    public static Set<String> getRolesFromMessage(IMessage message) {
        return getRolesWithChannel(message.getAuthor(), message.getChannel());
    }

    private static Set<String> getRolesWithChannel(IUser user, IChannel channel) {
        if (user == null) {
            return Collections.emptySet();
        } else if (channel == null || channel.isPrivate()) {
            return Collections.singleton(user.getID());
        } else {
            Set<String> roleSet = getRolesWithGuild(user, channel.getGuild());
            roleSet.add(channel.getID());
            return roleSet;
        }
    }

    private static Set<String> getRolesWithGuild(IUser user, IGuild guild) {
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

    /**
     * Get all resource-ids assignable to this Command. This includes the wildcard "*" resource.
     *
     * @param command the Command to get the ids from
     * @return a List of resource-ids assignable to the Command
     */
    public static List<String> getResourcesFromCommand(Command command) {
        if (isBlank(command.getCategory())) {
            return Collections.singletonList(command.getName());
        } else {
            return Arrays.asList(Constants.ANY, command.getName(), command.getCategory());
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

    public static boolean equalsAnyName(IUser user, String name, IGuild guild) {
        Objects.requireNonNull(user, "User must not be null");
        if (name == null) {
            return false;
        }
        boolean equalsNickname = false;
        if (guild != null) {
            String nickname = user.getNicknameForGuild(guild).orElse(null);
            equalsNickname = name.equalsIgnoreCase(nickname);
        }
        return equalsNickname
            || name.equalsIgnoreCase(user.getName())
            || name.equalsIgnoreCase(user.getName() + "#" + user.getDiscriminator());
    }

    public static void deleteMessage(IMessage message) {
        RequestBuffer.request(() -> {
            try {
                acquireDelete();
                message.delete();
            } catch (MissingPermissionsException | DiscordException e) {
                log.warn("Could not delete message", e);
            }
        });
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

    public static String ourBotId(IDiscordClient client) {
        return client.getOurUser().getID();
    }

    public static StringBuilder appendOrAnswer(IMessage message, StringBuilder builder, String content) {
        if (content != null) {
            if (shouldSplit(builder, content)) {
                answer(message, builder.toString());
                builder = new StringBuilder();
            }
            builder.append(content);
        }
        return builder;
    }

    private static boolean shouldSplit(StringBuilder builder, String content) {
        return builder.length() + content.length() > MessageSplitter.LENGTH_LIMIT;
    }

    public static void answer(IMessage to, String content) {
        answer(to, content, false);
    }

    public static void answer(IMessage to, String content, boolean tts) {
        answerToChannel(to.getChannel(), content, tts);
    }

    public static void reply(IMessage to, String content) {
        if (content.length() > MessageSplitter.LENGTH_LIMIT) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(MessageSplitter.LENGTH_LIMIT);
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
        if (content.length() > MessageSplitter.LENGTH_LIMIT) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(MessageSplitter.LENGTH_LIMIT);
            for (int i = 0; i < splits.size() - 1; i++) {
                sendMessage(to.getChannel(), splits.get(i), null, false);
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

    public static void answerPrivatelyWithFile(IMessage to, String content, InputStream stream, String fileName) {
        RequestBuffer.request(() -> {
            try {
                answerToChannelWithFile(to.getAuthor().getOrCreatePMChannel(), content, stream, fileName);
            } catch (DiscordException e) {
                log.warn("[{}] Failed to send PM to {}: {}", to.getClient().getOurUser().getName(),
                    humanize(to.getAuthor()), e);
            }
        });
    }

    private static void answerToChannel(IChannel channel, String content, boolean tts) {
        if (content.length() > MessageSplitter.LENGTH_LIMIT) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(MessageSplitter.LENGTH_LIMIT);
            for (String split : splits) {
                sendMessage(channel, split, null, false);
            }
        } else {
            sendMessage(channel, content, null, tts);
        }
    }

    private static void answerToChannelWithFile(IChannel channel, String content, File file) {
        if (content.length() > MessageSplitter.LENGTH_LIMIT) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(MessageSplitter.LENGTH_LIMIT);
            for (int i = 0; i < splits.size() - 1; i++) {
                sendMessage(channel, splits.get(i), null, false);
            }
            sendFile(channel, splits.get(splits.size() - 1), file);
        } else {
            sendFile(channel, content, file);
        }
    }

    private static void answerToChannelWithFile(IChannel channel, String content, InputStream stream, String fileName) {
        if (content.length() > MessageSplitter.LENGTH_LIMIT) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(MessageSplitter.LENGTH_LIMIT);
            for (int i = 0; i < splits.size() - 1; i++) {
                sendMessage(channel, splits.get(i), null, false);
            }
            sendFile(channel, splits.get(splits.size() - 1), stream, fileName);
        } else {
            sendFile(channel, content, stream, fileName);
        }
    }

    public static <T> CompletableFuture<RequestBuffer.RequestFuture<T>> request(IChannel channel, RequestBuffer.IRequest<T> request) {
        return CompletableFuture.supplyAsync(() -> {
            acquire(channel);
            return RequestBuffer.request(request);
        });
    }

    public static RequestBuffer.RequestFuture<IMessage> sendMessage(IChannel channel, String content) {
        return sendMessage(channel, content, null, false);
    }

    public static RequestBuffer.RequestFuture<IMessage> sendMessage(IChannel channel, EmbedObject embedObject) {
        return sendMessage(channel, null, embedObject, false);
    }

    public static RequestBuffer.RequestFuture<IMessage> sendMessage(IChannel channel, String content, EmbedObject embedObject) {
        return sendMessage(channel, content, embedObject, false);
    }

    public static RequestBuffer.RequestFuture<IMessage> sendMessage(IChannel channel, String content, EmbedObject embedObject, boolean tts) {
        acquire(channel);
        return RequestBuffer.request(() -> {
            try {
                return channel.sendMessage(content, embedObject, tts);
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
        if (!content.isEmpty()) {
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

    private static RequestBuffer.RequestFuture<IMessage> sendFile(IChannel channel, String content, InputStream stream, String fileName) {
        acquire(channel);
        return RequestBuffer.request(() -> {
            try {
                return channel.sendFile(content, false, stream, fileName);
            } catch (MissingPermissionsException e) {
                log.warn("[{}] Missing permissions in {}: {}", channel.getClient().getOurUser().getName(),
                    humanize(channel), e);
            } catch (DiscordException e) {
                log.warn("[{}] Failed to send file to {}: {}", channel.getClient().getOurUser().getName(),
                    humanize(channel), e);
            }
            return null;
        });
    }


    private DiscordUtil() {
    }
}
