package top.quantic.sentry.discord;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageList;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.service.util.Result;

import java.awt.*;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.DateUtil.parseTimeDate;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Component
public class Moderator implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Moderator.class);

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(delete());
    }

    private Command delete() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nonOptSpec = parser.nonOptions("Series of users to delete messages from: accepts IDs, mentions and names").ofType(String.class);
        OptionSpec<Integer> lastSpec = parser.acceptsAll(asList("limit", "last"), "Only match up to the latest N messages").withRequiredArg().ofType(Integer.class).describedAs("N");
        OptionSpec<Integer> depthSpec = parser.acceptsAll(asList("depth", "history"), "Limit scanning to the latest N messages").withRequiredArg().ofType(Integer.class).describedAs("N").defaultsTo(1000);
        OptionSpec<String> matchingSpec = parser.accepts("matching", "Delete messages matching this regex").withRequiredArg().describedAs("regex");
        OptionSpec<String> likeSpec = parser.accepts("like", "Delete messages containing this string").withRequiredArg().describedAs("string");
        OptionSpec<String> fromSpec = parser.acceptsAll(asList("from", "user"), "Delete messages from this user (@mention, name or ID)").withRequiredArg().describedAs("user");
        OptionSpec<String> beforeSpec = parser.acceptsAll(asList("before", "until"), "Delete messages before this temporal expression").withRequiredArg().describedAs("timex");
        OptionSpec<String> afterSpec = parser.acceptsAll(asList("after", "since"), "Delete messages after this temporal expression").withRequiredArg().describedAs("timex");
        OptionSpec<Void> testSpec = parser.acceptsAll(asList("preview", "test"), "Don't delete any messages");
        OptionSpec<Void> includeRequestSpec = parser.acceptsAll(asList("include-this", "include-request", "with-request"), "Include this message on the deletion candidates");
        return CommandBuilder.of("delete")
            .describedAs("Delete messages on this channel")
            .in("Moderation")
            .parsedBy(parser)
            .secured()
            .onExecute(context -> {
                OptionSet o = context.getOptionSet();
                IMessage message = context.getMessage();
                IChannel channel = message.getChannel();

                String content = message.getContent();
                content = content.contains(" ") ? content.split(" ", 2)[1] : content;

                EmbedBuilder builder = new EmbedBuilder()
                    .setLenient(true)
                    .withTitle("Delete")
                    .withDescription("`" + content + "`")
                    .withFooterIcon(message.getAuthor().getAvatarURL())
                    .withFooterText("Requested by " + withDiscriminator(message.getAuthor()));

                if (channel.isPrivate()) {
                    EmbedObject embed = builder
                        .withColor(new Color(0xaa0000))
                        .appendField("Error", "This command does not work for private messages, use `unsay`", false)
                        .build();
                    sendPrivately(message, embed);
                    return;
                }
                if (!o.has(lastSpec)
                    && !o.has(matchingSpec)
                    && !o.has(likeSpec)
                    && !o.has(fromSpec)
                    && !o.has(beforeSpec)
                    && !o.has(afterSpec)
                    && o.valuesOf(nonOptSpec).isEmpty()) {
                    EmbedObject embed = builder
                        .withColor(new Color(0xaa0000))
                        .appendField("Error", "Please specify at least one deletion criteria: `last, matching, like, from, before, after`", false)
                        .build();
                    sendPrivately(message, embed);
                    return;
                }
                MessageList messages = channel.getMessages();
                int capacity = messages.getCacheCapacity();
                messages.setCacheCapacity(MessageList.UNLIMITED_CAPACITY);
                ZonedDateTime before = null;
                ZonedDateTime after = null;
                if (o.has(beforeSpec)) {
                    before = parseTimeDate(o.valueOf(beforeSpec));
                    if (before == null) {
                        EmbedObject embed = builder
                            .withColor(new Color(0xaa0000))
                            .appendField("Error", "Could not parse a date from the expression: `" + o.valueOf(beforeSpec) + "`", false)
                            .build();
                        sendPrivately(message, embed);
                        return;
                    }
                    builder.appendField("Before", before.toOffsetDateTime().toString(), true);
                }
                if (o.has(afterSpec)) {
                    after = parseTimeDate(o.valueOf(afterSpec));
                    if (after == null) {
                        EmbedObject embed = builder
                            .withColor(new Color(0xaa0000))
                            .appendField("Error", "Could not parse a date from the expression: " + o.valueOf(afterSpec), false)
                            .build();
                        sendPrivately(message, embed);
                        return;
                    }
                    builder.appendField("After", after.toOffsetDateTime().toString(), true);
                }
                Set<IUser> authorsToMatch = new HashSet<>();
                List<String> keys = new ArrayList<>(o.valuesOf(nonOptSpec));
                if (o.has(fromSpec)) {
                    keys.add(o.valueOf(fromSpec));
                }
                if (!keys.isEmpty()) {
                    for (String key : keys) {
                        String id = key.replaceAll("<@!?([0-9]+)>", "$1");
                        List<IUser> matching = channel.getGuild().getUsers().stream()
                            .filter(u -> u.getID().equals(id) || equalsAnyName(u, id, channel.getGuild()))
                            .distinct().collect(Collectors.toList());
                        if (!matching.isEmpty()) {
                            authorsToMatch.addAll(matching);
                        } else {
                            answerPrivately(message, "No users matching " + key);
                        }
                    }
                }

                if (authorsToMatch.isEmpty()) {
                    builder.appendField("Authors", "Anyone", false);
                } else {
                    builder.appendField("Authors", authorsToMatch.stream()
                        .map(user -> "• " + humanize(user))
                        .collect(Collectors.joining("\n")), false);
                }

                // collect all offending messages
                List<IMessage> toDelete = new ArrayList<>();
                int i = 0;
                int depth = Math.max(1, o.valueOf(depthSpec));
                int limit = o.has(lastSpec) ? Math.max(1, o.valueOf(lastSpec)) : 100;
                builder.appendField("Search Depth", "Last " + inflect(depth, "message"), true);
                builder.appendField("Match Limit", "Up to " + inflect(limit, "message"), true);
                builder.appendField("Include Request", o.has(includeRequestSpec) ? "Yes" : "No", true);
                log.debug("Searching for up to {} and matching at most {} from {}", inflect(depth, "message"), limit, humanize(channel));
                while (i < depth && toDelete.size() < limit) {
                    try {
                        IMessage msg = messages.get(i++);
                        // skip the first message if it wasn't included
                        if (!o.has(includeRequestSpec) && i == 1) {
                            continue;
                        }
                        // continue (skip) if we are after "--before" timex
                        if (before != null && msg.getTimestamp().isAfter(before.toLocalDateTime())) {
                            continue;
                        }
                        // break if we reach "--after" timex
                        if (after != null && msg.getTimestamp().isBefore(after.toLocalDateTime())) {
                            log.debug("Search interrupted after hitting date constraint");
                            break;
                        }
                        // exclude by content (.matches)
                        if (o.has(matchingSpec) && !msg.getContent().matches(o.valueOf(matchingSpec))) {
                            continue;
                        }
                        // exclude by content (.contains)
                        if (o.has(likeSpec) && !msg.getContent().contains(o.valueOf(likeSpec))) {
                            continue;
                        }
                        // exclude by author
                        if (!authorsToMatch.isEmpty() && !authorsToMatch.contains(msg.getAuthor())) {
                            continue;
                        }
                        toDelete.add(msg);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // we reached the end apparently
                        log.warn("Could not retrieve messages to delete: {}", e.getMessage());
                        break;
                    }
                }

                if (toDelete.size() > 1) {
                    if (before != null && before.isBefore(ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusWeeks(2))) {
                        EmbedObject embed = builder
                            .withColor(new Color(0xaaaa00))
                            .appendField("Warning", "Can't bulk delete messages from before 2 weeks ago!\n" +
                                "Delete with `last 1` or set your date to `before \"2 weeks ago\"` or earlier", false)
                            .build();
                        sendPrivately(message, embed);
                        return;
                    }
                    if (after != null && after.isBefore(ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusWeeks(2))) {
                        EmbedObject embed = builder
                            .withColor(new Color(0xaaaa00))
                            .appendField("Warning", "Can't bulk delete messages from before 2 weeks ago!\n" +
                                "Delete with `last 1` or set your date to `after \"2 weeks ago\"` or earlier", false)
                            .build();
                        sendPrivately(message, embed);
                        return;
                    }
                }

                builder.appendField("Deleting", inflect(toDelete.size(), "message"), true);
                builder.appendField("Searched", inflect(i, "message"), true);

                sendPrivately(message, builder
                    .withColor(new Color(0x00aa00))
                    .build());

                if (o.has(testSpec)) {
                    answerPrivately(message, (toDelete.size() == 0 ? "Dry run: No messages would be deleted" :
                        "Dry run: Would be deleting " + inflect(toDelete.size(), "message") + "\n" + messageSummary(toDelete, 50)));
                    messages.setCacheCapacity(capacity);
                } else {
                    // bulk delete requires at least 2 messages
                    CompletableFuture<Result<Integer>> future;
                    if (toDelete.size() == 1) {
                        future = CompletableFuture.supplyAsync(() -> deleteMessage(toDelete.get(0)).get());
                    } else {
                        future = CompletableFuture.supplyAsync(() -> deleteInBatch(channel, toDelete));
                    }
                    future.whenComplete((result, error) -> {
                        if (result != null) {
                            if (!result.isSuccessful()) {
                                answerPrivately(message, "Error: " + result.getMessage());
                            } else {
                                answerPrivately(message, (toDelete.size() == 0 ? "No messages were deleted" :
                                    "Deleting " + inflect(toDelete.size(), "message") + "\n" + messageSummary(toDelete, 50)));
                            }
                        }
                        if (error != null) {
                            answerPrivately(message, "Error while deleting messages");
                        }
                    }).thenRun(() -> messages.setCacheCapacity(capacity));
                }
            }).build();
    }
}
