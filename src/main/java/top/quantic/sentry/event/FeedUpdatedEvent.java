package top.quantic.sentry.event;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Integer.toHexString;
import static java.util.Objects.hash;
import static top.quantic.sentry.service.util.DateUtil.formatRelative;

public class FeedUpdatedEvent extends SentryEvent {

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public FeedUpdatedEvent(SyndFeed source) {
        super(source);
    }

    @Override
    public SyndFeed getSource() {
        return (SyndFeed) super.getSource();
    }

    @Override
    public String getContentId() {
        return toHexString(hash(getSource().getLink(), getSource().getPublishedDate().toInstant()));
    }

    @Override
    public Map<String, Object> asMap(Map<String, Object> dataMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        SyndEntry entry = getSource().getEntries().stream().findFirst().orElse(null);
        if (entry != null) {
            map.put("comments", entry.getComments());
            map.put("author", entry.getAuthor());
            map.put("link", entry.getLink());
            map.put("updatedDate", entry.getUpdatedDate());
            map.put("title", entry.getTitle());
            map.put("uri", entry.getUri());
            map.put("publishedDate", entry.getPublishedDate());
        }
        return map;
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        return null;
    }

    @Override
    public String asContent(Map<String, Object> dataMap) {
        SyndEntry entry = getSource().getEntries().stream().findFirst().orElse(null);
        if (entry != null) {
            return entry.getTitle() + " " + formatRelative(entry.getPublishedDate().toInstant());
        }
        return getSource().getTitle() + " " + formatRelative(Instant.ofEpochMilli(getTimestamp()));
    }
}
