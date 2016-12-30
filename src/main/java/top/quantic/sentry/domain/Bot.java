package top.quantic.sentry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Bot.
 */

@Document(collection = "bot")
public class Bot extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Field("name")
    private String name = "";

    @NotNull
    @Field("token")
    private String token;

    @Field("auto_login")
    private Boolean autoLogin = false;

    @Field("daemon")
    private Boolean daemon = false;

    @Field("max_missed_pings")
    private Integer maxMissedPings = -1;

    @Field("max_reconnect_attempts")
    private Integer maxReconnectAttempts = 5;

    @Field("shard_count")
    private Integer shardCount = 1;

    @Field("primary")
    private Boolean primary = false;

    @Field("tags")
    private String tags = "";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Bot name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public Bot token(String token) {
        this.token = token;
        return this;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean isAutoLogin() {
        return autoLogin;
    }

    public Bot autoLogin(Boolean autoLogin) {
        this.autoLogin = autoLogin;
        return this;
    }

    public void setAutoLogin(Boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public Boolean isDaemon() {
        return daemon;
    }

    public Bot daemon(Boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    public void setDaemon(Boolean daemon) {
        this.daemon = daemon;
    }

    public Integer getMaxMissedPings() {
        return maxMissedPings;
    }

    public Bot maxMissedPings(Integer maxMissedPings) {
        this.maxMissedPings = maxMissedPings;
        return this;
    }

    public void setMaxMissedPings(Integer maxMissedPings) {
        this.maxMissedPings = maxMissedPings;
    }

    public Integer getMaxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    public Bot maxReconnectAttempts(Integer maxReconnectAttempts) {
        this.maxReconnectAttempts = maxReconnectAttempts;
        return this;
    }

    public void setMaxReconnectAttempts(Integer maxReconnectAttempts) {
        this.maxReconnectAttempts = maxReconnectAttempts;
    }

    public Integer getShardCount() {
        return shardCount;
    }

    public Bot shardCount(Integer shardCount) {
        this.shardCount = shardCount;
        return this;
    }

    public void setShardCount(Integer shardCount) {
        this.shardCount = shardCount;
    }

    public Boolean isPrimary() {
        return primary;
    }

    public Bot primary(Boolean primary) {
        this.primary = primary;
        return this;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    public String getTags() {
        return tags;
    }

    public Bot tags(String tags) {
        this.tags = tags;
        return this;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Bot bot = (Bot) o;
        if (bot.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, bot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Bot{" +
            "id=" + id +
            ", name='" + name + "'" +
            ", token='" + token + "'" +
            ", autoLogin='" + autoLogin + "'" +
            ", daemon='" + daemon + "'" +
            ", maxMissedPings='" + maxMissedPings + "'" +
            ", maxReconnectAttempts='" + maxReconnectAttempts + "'" +
            ", shardCount='" + shardCount + "'" +
            ", primary='" + primary + "'" +
            ", tags='" + tags + "'" +
            '}';
    }
}
