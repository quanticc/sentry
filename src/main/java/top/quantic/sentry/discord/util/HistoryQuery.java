package top.quantic.sentry.discord.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.impl.obj.Channel;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.MessageHistory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static top.quantic.sentry.discord.util.DiscordUtil.humanize;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

public class HistoryQuery {

    private static final Logger log = LoggerFactory.getLogger(HistoryQuery.class);

    private final IChannel channel;

    private int queryChunkSize = Channel.MESSAGE_CHUNK_COUNT;

    private Integer depth;
    private Integer limit;
    private Boolean includeLatest;
    private Boolean excludePinned;
    private ZonedDateTime before;
    private ZonedDateTime after;
    private String matching;
    private String like;
    private Collection<IUser> authors;

    private boolean reverseResults = false;

    private int traversed = 0;

    private HistoryQuery(IChannel channel) {
        this.channel = channel;
    }

    public static HistoryQuery of(IChannel channel) {
        return new HistoryQuery(channel);
    }

    public static HistoryQuery between(IChannel channel, ZonedDateTime start, ZonedDateTime end) {
        return of(channel)
            .after(start)
            .before(end);
    }

    public int getTraversed() {
        return traversed;
    }

    public List<IMessage> find() {
        List<IMessage> found = new ArrayList<>();
        traversed = 0;
        int index = 0;
        log.debug("Searching for {} and matching {} from {}",
            depth == null ? "all messages" : "up to " + inflect(depth, "message"),
            limit == null ? "as many as possible" : "at most " + limit,
            humanize(channel));
        MessageHistory history = channel.getMessageHistory(queryChunkSize);
        while ((depth == null || traversed < Math.max(1, depth)) && (limit == null || found.size() < Math.max(1, limit))) {
            if (index >= history.size()) {
                history = channel.getMessageHistoryFrom(history.getEarliestMessage().getLongID(), queryChunkSize);
                index = 1; // we already went through index 0
                if (index >= history.size()) {
                    break; // beginning of the channel reached
                }
            }
            IMessage msg = history.get(index++);
            traversed++;
            // skip the first message if it wasn't included
            if (includeLatest != null && !includeLatest && traversed == 1) {
                continue;
            }
            // exclude pinned messages
            if (excludePinned != null && excludePinned && msg.isPinned()) {
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
            // only do these checks if message has text content
            // TODO: handle embed content
            if (msg.getContent() != null) {
                // exclude by content (.matches)
                if (matching != null && !msg.getContent().matches(matching)) {
                    continue;
                }
                // exclude by content (.contains)
                if (like != null && !msg.getContent().contains(like)) {
                    continue;
                }
            }
            // exclude by author
            if (authors != null && !authors.isEmpty() && !authors.contains(msg.getAuthor())) {
                continue;
            }
            found.add(msg);
        }
        if (reverseResults) {
            Collections.reverse(found);
        }
        return found;
    }

    public HistoryQuery queryChunkSize(int queryChunkSize) {
        if (queryChunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        this.queryChunkSize = queryChunkSize;
        return this;
    }

    public HistoryQuery depth(Integer depth) {
        this.depth = depth;
        return this;
    }

    public HistoryQuery limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public HistoryQuery includeLatest(Boolean includeLatest) {
        this.includeLatest = includeLatest;
        return this;
    }

    public HistoryQuery excludePinned(Boolean excludePinned) {
        this.excludePinned = excludePinned;
        return this;
    }

    public HistoryQuery before(ZonedDateTime before) {
        this.before = before;
        return this;
    }

    public HistoryQuery after(ZonedDateTime after) {
        this.after = after;
        return this;
    }

    public HistoryQuery matching(String matching) {
        this.matching = matching;
        return this;
    }

    public HistoryQuery like(String like) {
        this.like = like;
        return this;
    }

    public HistoryQuery authors(Collection<IUser> authors) {
        this.authors = authors;
        return this;
    }

    public HistoryQuery reverseResults(boolean reverseResults) {
        this.reverseResults = reverseResults;
        return this;
    }

    public IChannel getChannel() {
        return channel;
    }

    public int getQueryChunkSize() {
        return queryChunkSize;
    }

    public void setQueryChunkSize(int queryChunkSize) {
        this.queryChunkSize = queryChunkSize;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Boolean getIncludeLatest() {
        return includeLatest;
    }

    public void setIncludeLatest(Boolean includeLatest) {
        this.includeLatest = includeLatest;
    }

    public Boolean getExcludePinned() {
        return excludePinned;
    }

    public void setExcludePinned(Boolean excludePinned) {
        this.excludePinned = excludePinned;
    }

    public ZonedDateTime getBefore() {
        return before;
    }

    public void setBefore(ZonedDateTime before) {
        this.before = before;
    }

    public ZonedDateTime getAfter() {
        return after;
    }

    public void setAfter(ZonedDateTime after) {
        this.after = after;
    }

    public String getMatching() {
        return matching;
    }

    public void setMatching(String matching) {
        this.matching = matching;
    }

    public String getLike() {
        return like;
    }

    public void setLike(String like) {
        this.like = like;
    }

    public Collection<IUser> getAuthors() {
        return authors;
    }

    public void setAuthors(Collection<IUser> authors) {
        this.authors = authors;
    }

    public boolean isReverseResults() {
        return reverseResults;
    }

    public void setReverseResults(boolean reverseResults) {
        this.reverseResults = reverseResults;
    }
}
