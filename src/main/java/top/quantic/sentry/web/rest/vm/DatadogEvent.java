package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatadogEvent implements Serializable {

    /**
     * title [required]
     * The event title. Limited to 100 characters.
     */
    private String title = "Untitled";

    /**
     * text [required]
     * The body of the event. Limited to 4000 characters. The text supports markdown.
     */
    private String text = "";

    /**
     * date_happened [optional, default=now]
     * POSIX timestamp of the event.
     */
    @JsonProperty("date_happened")
    private Long dateHappened;

    /**
     * priority [optional, default='normal']
     * The priority of the event ('normal' or 'low').
     */
    private String priority;

    /**
     * host [optional, default=None]
     * Host name to associate with the event.
     */
    private String host;

    /**
     * tags [optional, default=None]
     * A list of tags to apply to the event.
     */
    private List<String> tags;

    /**
     * alert_type [optional, default='info']
     * "error", "warning", "info" or "success".
     */
    @JsonProperty("alert_type")
    private String alertType;

    /**
     * aggregation_key [optional, default=None]
     * An arbitrary string to use for aggregation, max length of 100 characters. If you specify a key, all events using
     * that key will be grouped together in the Event Stream.
     */
    @JsonProperty("aggregation_key")
    private String aggregationKey;

    /**
     * source_type_name [optional, default=None]
     * The type of event being posted.
     * Options: nagios, hudson, jenkins, user, my apps, feed, chef, puppet, git, bitbucket, fabric, capistrano
     */
    @JsonProperty("source_type_name")
    private String sourceTypeName;

    public DatadogEvent() {

    }

    public DatadogEvent(String title, String text, Long dateHappened, String priority, String host, List<String> tags,
                        String alertType, String aggregationKey, String sourceTypeName) {
        this.title = title;
        this.text = text;
        this.dateHappened = dateHappened;
        this.priority = priority;
        this.host = host;
        this.tags = tags;
        this.alertType = alertType;
        this.aggregationKey = aggregationKey;
        this.sourceTypeName = sourceTypeName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getDateHappened() {
        return dateHappened;
    }

    public void setDateHappened(Long dateHappened) {
        this.dateHappened = dateHappened;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getAggregationKey() {
        return aggregationKey;
    }

    public void setAggregationKey(String aggregationKey) {
        this.aggregationKey = aggregationKey;
    }

    public String getSourceTypeName() {
        return sourceTypeName;
    }

    public void setSourceTypeName(String sourceTypeName) {
        this.sourceTypeName = sourceTypeName;
    }
}
