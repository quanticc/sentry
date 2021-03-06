package top.quantic.sentry.discord.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.service.SettingService;
import top.quantic.sentry.service.util.Result;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.truncate;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

public class DiscordUtil {

    private static final Logger log = LoggerFactory.getLogger(DiscordUtil.class);

    /**
     * Get all the role-ids assignable to a user from a Message.
     *
     * @param message the Message to get the ids from
     * @param deep    whether to retrieve roles from all guilds when invoked from a private channel
     * @return a Set of role-ids assignable from the Message's author
     */
    public static Set<String> getRolesFromMessage(IMessage message, boolean deep) {
        return getRolesWithChannel(message.getAuthor(), message.getChannel(), deep);
    }

    public static long snowflake(String id) {
    	return Long.parseUnsignedLong(id);
    }

    private static Set<String> getRolesWithChannel(IUser user, IChannel channel, boolean deep) {
        if (user == null) {
            return Collections.emptySet();
        } else if (channel == null || channel.isPrivate()) {
            if (deep) {
                // get all belonging guilds of this user - expensive!
                return user.getClient().getGuilds().stream()
                    .filter(guild -> guild.getUserByID(user.getLongID()) != null)
                    .map(guild -> getRolesWithGuild(user, guild))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            } else {
                return Sets.newHashSet(user.getStringID());
            }
        } else {
            Set<String> roleSet = getRolesWithGuild(user, channel.getGuild());
            roleSet.add(channel.getStringID());
            return roleSet;
        }
    }

    private static Set<String> getRolesWithGuild(IUser user, IGuild guild) {
        if (user == null) {
            return Sets.newHashSet();
        } else if (guild == null) {
            return Sets.newHashSet(user.getStringID());
        } else {
            Set<String> roleSet = Sets.newHashSet(user.getStringID());
            roleSet.addAll(user.getRolesForGuild(guild).stream()
                .map(IDiscordObject::getStringID)
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
    public static Set<String> getResourcesFromCommand(Command command) {
        if (isBlank(command.getCategory())) {
            return Collections.singleton(command.getName());
        } else {
            return ImmutableSet.of(command.getName(), command.getCategory());
        }
    }

    public static String humanize(IMessage message) {
        return String.format("[%s] %s: %s", humanize(message.getChannel()), humanize(message.getAuthor()), message.getContent());
    }

    public static String humanize(IUser user) {
        return String.format("%s#%s (%s)", user.getName(), user.getDiscriminator(), user.getStringID());
    }

    public static String humanizeAll(Collection<IUser> users, String delimiter) {
        return users.stream().map(DiscordUtil::humanize).collect(Collectors.joining(delimiter));
    }

    public static String withDiscriminator(IUser user) {
        return String.format("%s#%s", user.getName(), user.getDiscriminator());
    }

    public static String humanize(IChannel channel) {
        if (channel.isPrivate()) {
            return String.format("*PM* (%s)", channel.getStringID());
        } else {
            return String.format("%s/%s (%s)", channel.getGuild().getName(), channel.getName(), channel.getStringID());
        }
    }

    public static String humanizeShort(IChannel channel) {
        if (channel.isPrivate()) {
            return "*PM*";
        } else {
            return String.format("%s (#%s)", channel.getName(), channel.getStringID());
        }
    }

    public static String humanize(IGuild guild) {
        return String.format("%s (%s)", guild.getName(), guild.getStringID());
    }

    public static String humanize(IRole role) {
        return String.format("%s/%s (%s)", role.getGuild().getName(), role.getName().replace("@", "@\u200B"), role.getStringID());
    }

    public static String humanizeShort(IUser user) {
        if (user == null) {
            return "";
        }
        return "• " + user.getName() + " <" + user.getStringID() + ">\n";
    }

    public static boolean equalsAnyName(IUser user, String name, IGuild guild) {
        Objects.requireNonNull(user, "User must not be null");
        if (name == null) {
            return false;
        }
        boolean equalsNickname = false;
        if (guild != null) {
            String nickname = user.getNicknameForGuild(guild);
            equalsNickname = name.equalsIgnoreCase(nickname);
        }
        return equalsNickname
            || name.equalsIgnoreCase(user.getName())
            || name.equalsIgnoreCase(user.getName() + "#" + user.getDiscriminator());
    }

    public static List<IUser> awareUserList(boolean aware, IMessage message) {
        IDiscordClient client = message.getClient();
        IChannel channel = message.getChannel();
        if (aware) {
            return client.getUsers();
        } else if (!channel.isPrivate()) {
            return channel.getGuild().getUsers();
        } else {
            return asList(message.getAuthor(), client.getOurUser());
        }
    }

    public static RequestBuffer.RequestFuture<Result<Integer>> deleteMessage(IMessage message) {
        return RequestBuffer.request(() -> {
            try {
                message.delete();
                return Result.ok(1, "Message #" + message.getStringID() + " was deleted");
            } catch (MissingPermissionsException | DiscordException e) {
                log.warn("Could not delete message", e);
                return Result.error("Could not delete message: " + e.getMessage());
            }
        });
    }

    public static Result<Integer> deleteInBatch(IChannel channel, List<IMessage> toDelete) {
        if (toDelete.isEmpty()) {
            log.info("No messages to delete");
            return Result.ok(0, "No messages to delete");
        } else {
            log.info("Preparing to delete {} messages from {}", toDelete.size(), humanize(channel));
            int total = 0;
            Result<Integer> result;
            for (int x = 0; x < (toDelete.size() / 100) + 1; x++) {
                List<IMessage> subList = toDelete.subList(x * 100, Math.min(toDelete.size(), (x + 1) * 100));
                result = RequestBuffer.request(() -> {
                    try {
                        channel.bulkDelete(subList);
                        return Result.ok(subList.size());
                    } catch (MissingPermissionsException | DiscordException e) {
                        log.warn("Failed to delete messages", e);
                        return Result.<Integer>error("Failed to delete messages: " + e.getMessage());
                    }
                }).get();
                if (!result.isSuccessful()) {
                    return Result.error(total, result.getMessage(), null);
                } else {
                    total += result.getContent();
                }
            }
            return Result.ok(total, "Bulk deleted " + inflect(total, "message"));
        }
    }

    public static String messageSummary(List<IMessage> messages, int maxChars) {
        return messages.stream()
            .map(message -> String.format("• [%s] %s: %s", humanizeShort(message.getChannel()), humanize(message.getAuthor()),
                truncate(MarkdownUtil.strip(message.getContent()), maxChars)))
            .collect(Collectors.joining("\n"));
    }

    public static String ourBotName(Event event) {
        return ourBotName(event.getClient());
    }

    public static String ourBotId(Event event) {
        return ourBotId(event.getClient());
    }

    public static String ourBotName(IDiscordClient client) {
        return client.getOurUser().getName();
    }

    public static String ourBotId(IDiscordClient client) {
        return client.getOurUser().getStringID();
    }

    public static String[] safeSplit(String[] args, int limit) {
        return args == null ? null : args[0].split(" ", limit);
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
        return builder.length() + content.length() > Message.MAX_MESSAGE_LENGTH;
    }

    public static CompletableFuture<RequestBuffer.RequestFuture<IMessage>> updateMessage(RequestBuffer.RequestFuture<IMessage> message, String content) {
        return CompletableFuture.supplyAsync(() -> RequestBuffer.request(() -> (IMessage) message.get().edit(content)));
    }

    /**
     * Retrieve a trusted channel from this message, or a PM channel if it's not possible.
     *
     * @param settingService a SettingService instance to retrieve trusted channel configuration
     * @param message        the message to obtain the trusted channel from
     * @return a channel defined as 'trusted' via settings
     */
    public static IChannel getTrustedChannel(SettingService settingService, IMessage message) {
        IChannel channel = message.getChannel();
        String channelId = channel.getStringID();
        if (channel.isPrivate() || !settingService.findSetting(channel.getGuild().getStringID(), channelId, Constants.TRUSTED).isEmpty()) {
            return message.getChannel();
        } else {
            return message.getAuthor().getOrCreatePMChannel();
        }
    }

    public static RequestBuffer.RequestFuture<IMessage> trustedAnswer(SettingService service, IMessage to, String content) {
        return answerToChannel(getTrustedChannel(service, to), content, false);
    }

    public static RequestBuffer.RequestFuture<IMessage> answer(IMessage to, String content) {
        return answerToChannel(to.getChannel(), content, false);
    }

    public static RequestBuffer.RequestFuture<IMessage> answer(IMessage to, String content, boolean tts) {
        return answerToChannel(to.getChannel(), content, tts);
    }

    public static RequestBuffer.RequestFuture<IMessage> answerPrivately(IMessage to, String content) {
        return answerPrivately(to, content, false);
    }

    public static RequestBuffer.RequestFuture<IMessage> answerPrivately(IMessage to, String content, boolean tts) {
        try {
            return answerToChannel(to.getAuthor().getOrCreatePMChannel(), content, tts);
        } catch (DiscordException e) {
            log.warn("[{}] Failed to send PM to {}: {}", to.getClient().getOurUser().getName(),
                humanize(to.getAuthor()), e);
        }
        return RequestBuffer.request(() -> null);
    }

    public static RequestBuffer.RequestFuture<IMessage> answerWithFile(IMessage to, String content, File file) {
        try {
            return answerToChannelWithFile(to.getChannel(), content, file);
        } catch (DiscordException e) {
            log.warn("[{}] Failed to send PM to {}: {}", to.getClient().getOurUser().getName(),
                humanize(to.getAuthor()), e);
        }
        return null;
    }

    public static RequestBuffer.RequestFuture<IMessage> answerPrivatelyWithFile(IMessage to, String content, File file) {
        try {
            return answerToChannelWithFile(to.getAuthor().getOrCreatePMChannel(), content, file);
        } catch (DiscordException e) {
            log.warn("[{}] Failed to send PM to {}: {}", to.getClient().getOurUser().getName(),
                humanize(to.getAuthor()), e);
        }
        return null;
    }

    public static RequestBuffer.RequestFuture<IMessage> answerPrivatelyWithFile(IMessage to, String content, InputStream stream, String fileName) {
        try {
            return answerToChannelWithFile(to.getAuthor().getOrCreatePMChannel(), content, stream, fileName);
        } catch (DiscordException e) {
            log.warn("[{}] Failed to send PM to {}: {}", to.getClient().getOurUser().getName(),
                humanize(to.getAuthor()), e);
        }
        return RequestBuffer.request(() -> null);
    }

    public static RequestBuffer.RequestFuture<IMessage> answerToChannel(IChannel channel, String content) {
        return answerToChannel(channel, content, false);
    }

    public static RequestBuffer.RequestFuture<IMessage> answerToChannel(IChannel channel, String content, boolean tts) {
        if (content.length() > Message.MAX_MESSAGE_LENGTH) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(Message.MAX_MESSAGE_LENGTH);
            return RequestBuffer.request(() -> {
                IMessage last = null;
                for (String split : splits) {
                    last = sendMessage(channel, split, null, tts).get();
                }
                return last;
            });
        } else {
            return sendMessage(channel, content, null, tts);
        }
    }

    public static RequestBuffer.RequestFuture<IMessage> answerToChannelWithFile(IChannel channel, String content, File file) {
        if (content.length() > Message.MAX_MESSAGE_LENGTH) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(Message.MAX_MESSAGE_LENGTH);
            return RequestBuffer.request(() -> {
                for (int i = 0; i < splits.size() - 1; i++) {
                    sendMessage(channel, splits.get(i), null, false).get();
                }
                return sendFile(channel, splits.get(splits.size() - 1), file).get();
            });
        } else {
            return sendFile(channel, content, file);
        }
    }

    public static RequestBuffer.RequestFuture<IMessage> answerToChannelWithFile(IChannel channel, String content, InputStream stream, String fileName) {
        if (content.length() > Message.MAX_MESSAGE_LENGTH) {
            MessageSplitter messageSplitter = new MessageSplitter(content);
            List<String> splits = messageSplitter.split(Message.MAX_MESSAGE_LENGTH);
            return RequestBuffer.request(() -> {
                for (int i = 0; i < splits.size() - 1; i++) {
                    sendMessage(channel, splits.get(i), null, false).get();
                }
                return sendFile(channel, splits.get(splits.size() - 1), stream, fileName).get();
            });
        } else {
            return sendFile(channel, content, stream, fileName);
        }
    }

    public static RequestBuffer.RequestFuture<IMessage> sendPrivately(IMessage to, String content) {
        return sendMessage(to.getAuthor().getOrCreatePMChannel(), content, null, false);
    }

    public static RequestBuffer.RequestFuture<IMessage> sendPrivately(IMessage to, EmbedObject embedObject) {
        return sendMessage(to.getAuthor().getOrCreatePMChannel(), null, embedObject, false);
    }

    public static RequestBuffer.RequestFuture<IMessage> sendPrivately(IMessage to, String content, EmbedObject embedObject) {
        return sendMessage(to.getAuthor().getOrCreatePMChannel(), content, embedObject, false);
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
        return RequestBuffer.request(() -> (IMessage) sendUnbufferedMessage(channel, content, embedObject, tts));
    }

    private static RequestBuffer.RequestFuture<IMessage> sendFile(IChannel channel, String content, File file) {
        return RequestBuffer.request(() -> (IMessage) sendUnbufferedFile(channel, content, file));
    }

    private static RequestBuffer.RequestFuture<IMessage> sendFile(IChannel channel, String content, InputStream stream, String fileName) {
        return RequestBuffer.request(() -> (IMessage) sendUnbufferedFile(channel, content, stream, fileName));
    }

    private static IMessage sendUnbufferedMessage(IChannel channel, String content, EmbedObject embedObject, boolean tts) {
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
    }

    private static IMessage sendUnbufferedFile(IChannel channel, String content, File file) {
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
    }

    private static IMessage sendUnbufferedFile(IChannel channel, String content, InputStream stream, String fileName) {
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
    }

    public static Result<Void> request(Runnable runnable) {
        return RequestBuffer.request(() -> {
            try {
                runnable.run();
                return Result.<Void>ok(null);
            } catch (RateLimitException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Request failed due to an exception", e);
                return Result.<Void>error(e.getMessage(), e);
            }
        }).get();
    }

    public static <T> Result<T> request(Callable<T> callable) {
        return RequestBuffer.request(() -> {
            try {
                return Result.ok(callable.call());
            } catch (RateLimitException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Request failed due to an exception", e);
                return Result.<T>error(e.getMessage(), e);
            }
        }).get();
    }

    public static EmbedBuilder authoredEmbed(IMessage message) {
        return new EmbedBuilder()
            .setLenient(true)
            .withFooterIcon(message.getAuthor().getAvatarURL())
            .withFooterText("Requested by " + withDiscriminator(message.getAuthor()));
    }

    public static EmbedBuilder lenientEmbed() {
        return new EmbedBuilder()
            .setLenient(true);
    }

    public static EmbedBuilder authoredSuccessEmbed(IMessage message) {
        return authoredEmbed(message)
            .withColor(new Color(0x00aa00));
    }

    public static EmbedBuilder authoredInfoEmbed(IMessage message) {
        return authoredEmbed(message)
            .withColor(new Color(0x0000aa));
    }

    public static EmbedBuilder authoredWarningEmbed(IMessage message) {
        return authoredEmbed(message)
            .withColor(new Color(0xaaaa00));
    }

    public static EmbedBuilder authoredErrorEmbed(IMessage message) {
        return authoredEmbed(message)
            .withColor(new Color(0xaa0000));
    }

    public static EmbedBuilder warningEmbed() {
        return lenientEmbed()
            .withColor(new Color(0xaaaa00));
    }

    public static EmbedBuilder errorEmbed() {
        return lenientEmbed()
            .withColor(new Color(0xaa0000));
    }

    public static String emoji(String alias) {
        return emoji(alias, "");
    }

    public static String emoji(String alias, String fallback) {
        Emoji emoji = EmojiManager.getForAlias(alias);
        return emoji != null ? emoji.getUnicode() : fallback;
    }

    private DiscordUtil() {
    }
}
