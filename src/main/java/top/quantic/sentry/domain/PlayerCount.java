package top.quantic.sentry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A PlayerCount.
 */

@Document(collection = "player_count")
public class PlayerCount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("region")
    private String region;

    @NotNull
    @Min(value = 0)
    @Field("value")
    private Long value;

    @NotNull
    @Field("timestamp")
    private ZonedDateTime timestamp = ZonedDateTime.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegion() {
        return region;
    }

    public PlayerCount region(String region) {
        this.region = region;
        return this;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Long getValue() {
        return value;
    }

    public PlayerCount value(Long value) {
        this.value = value;
        return this;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public PlayerCount timestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PlayerCount playerCount = (PlayerCount) o;
        if (playerCount.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, playerCount.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "PlayerCount{" +
            "id=" + id +
            ", region='" + region + "'" +
            ", value='" + value + "'" +
            ", timestamp='" + timestamp + "'" +
            '}';
    }
}
