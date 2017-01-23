package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import top.quantic.sentry.event.ContentSupplier;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.Integer.toHexString;
import static top.quantic.sentry.service.util.Maps.entriesToMap;
import static top.quantic.sentry.service.util.Maps.entry;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatadogPayload implements ContentSupplier, Serializable {

    private String id;

    private String title;

    @JsonProperty("last_updated")
    private String lastUpdated;

    private String date;

    @JsonProperty("event_type")
    private String eventType;

    private String body;

    private Map<String, Object> org;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, Object> getOrg() {
        return org;
    }

    public void setOrg(Map<String, Object> org) {
        this.org = org;
    }

    @Override
    public String getContentId() {
        return toHexString(hashCode());
    }

    @Override
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(
            Stream.of(
                entry("id", id),
                entry("title", title),
                entry("lastUpdated", lastUpdated),
                entry("date", date),
                entry("eventType", eventType),
                entry("body", body),
                entry("shortBody", shorten(body))
            ).collect(entriesToMap()));
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> variables) {
        // TODO: Create embed
        return null;
    }

    @Override
    public String asContent() {
        return "**" + title + "**\n" + shorten(body);
    }

    private String shorten(String body) {
        String[] splits = body.split("===", 3);
        if (splits.length >= 2) {
            return splits[1].trim();
        } else if (splits.length == 1) {
            return splits[0].trim();
        } else {
            return body.trim();
        }
    }

    @Override
    public String toString() {
        return "DatadogEvent{" +
            "id='" + id + '\'' +
            ", title='" + title + '\'' +
            ", lastUpdated='" + lastUpdated + '\'' +
            ", date='" + date + '\'' +
            ", eventType='" + eventType + '\'' +
            ", body='" + body + '\'' +
            ", org=" + org +
            '}';
    }
}
