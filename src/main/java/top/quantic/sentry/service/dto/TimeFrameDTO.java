package top.quantic.sentry.service.dto;

import java.time.ZonedDateTime;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;


/**
 * A DTO for the TimeFrame entity.
 */
public class TimeFrameDTO implements Serializable {

    private String id;

    @NotNull
    private String subscriber;

    @NotNull
    private ZonedDateTime start;

    @NotNull
    private ZonedDateTime end;

    @NotNull
    private Boolean inclusive;

    @NotNull
    private String recurrenceValue;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }
    public ZonedDateTime getStart() {
        return start;
    }

    public void setStart(ZonedDateTime start) {
        this.start = start;
    }
    public ZonedDateTime getEnd() {
        return end;
    }

    public void setEnd(ZonedDateTime end) {
        this.end = end;
    }
    public Boolean getInclusive() {
        return inclusive;
    }

    public void setInclusive(Boolean inclusive) {
        this.inclusive = inclusive;
    }
    public String getRecurrenceValue() {
        return recurrenceValue;
    }

    public void setRecurrenceValue(String recurrenceValue) {
        this.recurrenceValue = recurrenceValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimeFrameDTO timeFrameDTO = (TimeFrameDTO) o;

        if ( ! Objects.equals(id, timeFrameDTO.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "TimeFrameDTO{" +
            "id=" + id +
            ", subscriber='" + subscriber + "'" +
            ", start='" + start + "'" +
            ", end='" + end + "'" +
            ", inclusive='" + inclusive + "'" +
            ", recurrenceValue='" + recurrenceValue + "'" +
            '}';
    }
}
