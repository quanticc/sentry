package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatadogDowntime implements Serializable {

    private String scope;

    private Long start;

    private Long end;

    private String message;

    private Recurrence recurrence;

    public DatadogDowntime() {

    }

    public DatadogDowntime(String scope, Long start, Long end, String message, Recurrence recurrence) {
        this.scope = scope;
        this.start = start;
        this.end = end;
        this.message = message;
        this.recurrence = recurrence;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    public static class Recurrence {

        private String type;

        private Integer period;

        @JsonProperty("week_days")
        private List<String> weekDays;

        @JsonProperty("until_occurrences")
        private String untilOccurrences;

        @JsonProperty("until_date")
        private String untilDate;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getPeriod() {
            return period;
        }

        public void setPeriod(Integer period) {
            this.period = period;
        }

        public List<String> getWeekDays() {
            return weekDays;
        }

        public void setWeekDays(List<String> weekDays) {
            this.weekDays = weekDays;
        }

        public String getUntilOccurrences() {
            return untilOccurrences;
        }

        public void setUntilOccurrences(String untilOccurrences) {
            this.untilOccurrences = untilOccurrences;
        }

        public String getUntilDate() {
            return untilDate;
        }

        public void setUntilDate(String untilDate) {
            this.untilDate = untilDate;
        }
    }
}
