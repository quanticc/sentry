package top.quantic.sentry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A TimeFrame.
 */

@Document(collection = "time_frame")
public class TimeFrame extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("subscriber")
    private String subscriber;

    @NotNull
    @Field("start")
    private ZonedDateTime start;

    @NotNull
    @Field("end")
    private ZonedDateTime end;

    @NotNull
    @Field("inclusive")
    private Boolean inclusive;

    @Field("recurrence")
    private Period recurrence = Period.ofDays(1);

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubscriber() {
        return subscriber;
    }

    public TimeFrame subscriber(String subscriber) {
        this.subscriber = subscriber;
        return this;
    }

    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public TimeFrame start(ZonedDateTime start) {
        this.start = start;
        return this;
    }

    public void setStart(ZonedDateTime start) {
        this.start = start;
    }

    public ZonedDateTime getEnd() {
        return end;
    }

    public TimeFrame end(ZonedDateTime end) {
        this.end = end;
        return this;
    }

    public void setEnd(ZonedDateTime end) {
        this.end = end;
    }

    public Boolean isInclusive() {
        return inclusive;
    }

    public TimeFrame inclusive(Boolean inclusive) {
        this.inclusive = inclusive;
        return this;
    }

    public void setInclusive(Boolean inclusive) {
        this.inclusive = inclusive;
    }

    public Period getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Period recurrence) {
        this.recurrence = recurrence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeFrame timeFrame = (TimeFrame) o;
        if (timeFrame.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, timeFrame.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "TimeFrame{" +
            "id=" + id +
            ", subscriber='" + subscriber + "'" +
            ", start='" + start + "'" +
            ", end='" + end + "'" +
            ", inclusive='" + inclusive + "'" +
            ", recurrence='" + recurrence + "'" +
            '}';
    }
}
