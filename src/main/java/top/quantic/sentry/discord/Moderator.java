package top.quantic.sentry.discord;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.MessageList;
import top.quantic.sentry.discord.command.Command;
import top.quantic.sentry.discord.command.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static top.quantic.sentry.discord.util.DiscordUtil.*;

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
        OptionSpec<Integer> lastSpec = parser.accepts("last", "Limit deletion to the latest N messages").withRequiredArg().ofType(Integer.class).describedAs("N").defaultsTo(100);
        OptionSpec<String> matchingSpec = parser.accepts("matching", "Delete messages matching this regex").withRequiredArg().describedAs("regex");
        OptionSpec<String> likeSpec = parser.accepts("like", "Delete messages containing this string").withRequiredArg().describedAs("string");
        OptionSpec<String> fromSpec = parser.accepts("from", "Delete messages from this user (@mention, name or ID)").withRequiredArg().describedAs("user");
        OptionSpec<String> beforeSpec = parser.acceptsAll(asList("before", "until"), "Delete messages before this temporal expression").withRequiredArg().describedAs("timex");
        OptionSpec<String> afterSpec = parser.acceptsAll(asList("after", "since"), "Delete messages after this temporal expression").withRequiredArg().describedAs("timex");
        return CommandBuilder.of("delete")
            .describedAs("Delete messages on this channel")
            .in("Moderation")
            .parsedBy(parser)
            .secured()
            .onExecute(context -> {
                OptionSet o = context.getOptionSet();
                IMessage message = context.getMessage();
                IChannel channel = message.getChannel();
                if (channel.isPrivate()) {
                    answerPrivately(message, "This command does not work for private messages, use `unsay`");
                }
                if (!o.has(lastSpec)
                    && !o.has(matchingSpec)
                    && !o.has(likeSpec)
                    && !o.has(fromSpec)
                    && !o.has(beforeSpec)
                    && !o.has(afterSpec)
                    && o.valuesOf(nonOptSpec).isEmpty()) {
                    answerPrivately(message, "Please specify at least one deletion criteria: last, matching, like, from, before, after");
                }
                MessageList messages = channel.getMessages();
                int capacity = messages.getCacheCapacity();
                messages.setCacheCapacity(MessageList.UNLIMITED_CAPACITY);
                ZonedDateTime before = null;
                ZonedDateTime after = null;
                if (o.has(beforeSpec)) {
                    before = parseTimeDate(o.valueOf(beforeSpec));
                }
                if (o.has(afterSpec)) {
                    after = parseTimeDate(o.valueOf(afterSpec));
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

                // collect all offending messages
                List<IMessage> toDelete = new ArrayList<>();
                int i = 0;
                int max = !o.has(lastSpec) ? 100 : Math.max(1, o.valueOf(lastSpec));
                log.debug("Searching for up to {} messages from {}", max, humanize(channel));
                while (i < max) {
                    try {
                        IMessage msg = messages.get(i++);
                        // continue if we are after "--before" timex
                        if (before != null && msg.getTimestamp().isAfter(before.toLocalDateTime())) {
                            continue;
                        }
                        // break if we reach "--after" timex
                        if (after != null && msg.getTimestamp().isBefore(after.toLocalDateTime())) {
                            log.debug("Search interrupted after hitting date constraint");
                            break;
                        }
                        // exclude by content (.matches)
                        if (o.has(matchingSpec) &&
                            !msg.getContent().matches(o.valueOf(matchingSpec))) {
                            continue;
                        }
                        // exclude by content (.contains)
                        if (o.has(likeSpec) &&
                            !msg.getContent().contains(o.valueOf(likeSpec))) {
                            continue;
                        }
                        // exclude by author
                        if (!authorsToMatch.isEmpty() && !authorsToMatch.contains(msg.getAuthor())) {
                            continue;
                        }
                        toDelete.add(msg);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // we reached the end apparently
                        log.warn("Could not retrieve messages to delete", e);
                        break;
                    }
                }
                // bulk delete requires at least 2 messages
                if (toDelete.size() == 1) {
                    deleteMessage(toDelete.get(0));
                } else {
                    deleteInBatch(channel, toDelete);
                }
                messages.setCacheCapacity(capacity);
                answerPrivately(message, (toDelete.size() == 0 ? "No messages were deleted" : "Deleting "
                    + toDelete.size() + " message" + (toDelete.size() == 1 ? "" : "s")));
            }).build();
    }

    private ZonedDateTime parseTimeDate(String timex) {
        List<Date> parsed = new PrettyTimeParser().parse(timex); // never null, can be empty
        if (!parsed.isEmpty()) {
            Date first = parsed.get(0);
            return ZonedDateTime.ofInstant(first.toInstant(), ZoneId.systemDefault());
        }
        log.warn("Could not parse a valid date from input: {}", timex);
        return null;
    }
}
