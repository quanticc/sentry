package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import top.quantic.sentry.event.ContentSupplier;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Integer.toHexString;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatadogEvent implements ContentSupplier {

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
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("lastUpdated", lastUpdated);
        map.put("date", date);
        map.put("eventType", eventType);
        map.put("body", body);
        map.put("shortBody", shorten(body));
        return map;
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
