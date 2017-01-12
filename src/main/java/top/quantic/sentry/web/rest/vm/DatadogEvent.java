package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatadogEvent {

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
