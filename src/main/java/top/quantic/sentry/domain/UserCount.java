package top.quantic.sentry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A UserCount.
 */

@Document(collection = "user_count")
public class UserCount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("bot")
    private String bot;

    @NotNull
    @Field("status")
    private String status;

    @NotNull
    @Min(value = 0)
    @Field("value")
    private Long value;

    @NotNull
    @Field("guild")
    private String guild;

    @NotNull
    @Field("timestamp")
    private ZonedDateTime timestamp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBot() {
        return bot;
    }

    public UserCount bot(String bot) {
        this.bot = bot;
        return this;
    }

    public void setBot(String bot) {
        this.bot = bot;
    }

    public String getStatus() {
        return status;
    }

    public UserCount status(String status) {
        this.status = status;
        return this;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getValue() {
        return value;
    }

    public UserCount value(Long value) {
        this.value = value;
        return this;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public String getGuild() {
        return guild;
    }

    public UserCount guild(String guild) {
        this.guild = guild;
        return this;
    }

    public void setGuild(String guild) {
        this.guild = guild;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public UserCount timestamp(ZonedDateTime timestamp) {
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
        UserCount userCount = (UserCount) o;
        if (userCount.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, userCount.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "UserCount{" +
            "id=" + id +
            ", bot='" + bot + "'" +
            ", status='" + status + "'" +
            ", value='" + value + "'" +
            ", guild='" + guild + "'" +
            ", timestamp='" + timestamp + "'" +
            '}';
    }
}
