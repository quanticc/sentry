package top.quantic.sentry.discord;

import com.google.common.collect.Lists;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static top.quantic.sentry.discord.util.DiscordUtil.*;

@Component
public class Moderator implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Moderator.class);

    @Override
    public List<Command> getCommands() {
        return Lists.newArrayList(delete());
    }

    private Command delete() {
        OptionParser parser = new OptionParser();
        OptionSpec<Integer> deleteLastSpec = parser.accepts("last", "Limit deletion to the latest N messages").withRequiredArg().ofType(Integer.class).describedAs("N").defaultsTo(100);
        OptionSpec<String> deleteMatchingSpec = parser.accepts("matching", "Delete messages matching this regex").withRequiredArg().describedAs("regex");
        OptionSpec<String> deleteLikeSpec = parser.accepts("like", "Delete messages containing this string").withRequiredArg().describedAs("string");
        OptionSpec<String> deleteFromSpec = parser.accepts("from", "Delete messages from this user (@mention, name or ID)").withRequiredArg().describedAs("user");
        OptionSpec<String> deleteBeforeSpec = parser.acceptsAll(asList("before", "until"), "Delete messages before this temporal expression").withRequiredArg().describedAs("timex");
        OptionSpec<String> deleteAfterSpec = parser.acceptsAll(asList("after", "since"), "Delete messages after this temporal expression").withRequiredArg().describedAs("timex");
        return CommandBuilder.of("delete")
            .describedAs("Delete messages on this channel")
            .in("Moderation")
            .parsedBy(parser)
            .onExecute(context -> {
                OptionSet o = context.getOptionSet();
                IMessage message = context.getMessage();
                IChannel channel = message.getChannel();
                if (channel.isPrivate()) {
                    answerPrivately(message, "This command does not work for private messages, use `unsay`");
                }
                if (!o.has(deleteLastSpec)
                    && !o.has(deleteMatchingSpec)
                    && !o.has(deleteLikeSpec)
                    && !o.has(deleteFromSpec)
                    && !o.has(deleteBeforeSpec)
                    && !o.has(deleteAfterSpec)) {
                    answerPrivately(message, "Please specify at least one deletion criteria: last, matching, like, from, before, after");
                }
                MessageList messages = channel.getMessages();
                int capacity = messages.getCacheCapacity();
                messages.setCacheCapacity(MessageList.UNLIMITED_CAPACITY);
                ZonedDateTime before = null;
                ZonedDateTime after = null;
                if (o.has(deleteBeforeSpec)) {
                    before = parseTimeDate(o.valueOf(deleteBeforeSpec));
                }
                if (o.has(deleteAfterSpec)) {
                    after = parseTimeDate(o.valueOf(deleteAfterSpec));
                }
                // TODO: allow matching multiple authors
                IUser authorToMatch = null;
                if (o.has(deleteFromSpec)) {
                    String key = o.valueOf(deleteFromSpec).replaceAll("<@!?([0-9]+)>", "$1");
                    List<IUser> matching = channel.getGuild().getUsers().stream()
                        .filter(u -> u.getName().equalsIgnoreCase(key) || u.getID().equals(key) || key.equals(u.getName() + u.getDiscriminator()))
                        .distinct().collect(Collectors.toList());
                    if (matching.size() > 1) {
                        StringBuilder builder = new StringBuilder("Multiple users matched, please narrow down search or use ID\n");
                        for (IUser user : matching) {
                            builder.append(user.getName()).append(" has id `").append(user.getID()).append("`\n");
                        }
                        answerPrivately(message, builder.toString());
                    } else if (matching.isEmpty()) {
                        answerPrivately(message, "User " + key + " not found in cache");
                    } else {
                        authorToMatch = matching.get(0);
                    }
                }
                // collect all offending messages
                List<IMessage> toDelete = new ArrayList<>();
                int i = 0;
                int max = !o.has(deleteLastSpec) ? 100 : Math.max(1, o.valueOf(deleteLastSpec));
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
                        if (o.has(deleteMatchingSpec) &&
                            !msg.getContent().matches(o.valueOf(deleteMatchingSpec))) {
                            continue;
                        }
                        // exclude by content (.contains)
                        if (o.has(deleteLikeSpec) &&
                            !msg.getContent().contains(o.valueOf(deleteLikeSpec))) {
                            continue;
                        }
                        // exclude by author
                        if (authorToMatch != null && !msg.getAuthor().equals(authorToMatch)) {
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
