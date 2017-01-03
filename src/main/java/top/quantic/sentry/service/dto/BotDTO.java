package top.quantic.sentry.service.dto;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;


/**
 * A DTO for the Bot entity.
 */
public class BotDTO implements Serializable {

    private String id;

    private String name;

    @NotNull
    private String token;

    private Boolean autoLogin;

    private Boolean daemon;

    private Integer maxMissedPings;

    private Integer maxReconnectAttempts;

    private Integer shardCount;

    private Boolean primary;

    private String tags;

    // Decorated fields
    private boolean created = false;
    private boolean loggedIn = false;
    private boolean ready = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
    public Boolean getAutoLogin() {
        return autoLogin;
    }

    public void setAutoLogin(Boolean autoLogin) {
        this.autoLogin = autoLogin;
    }
    public Boolean getDaemon() {
        return daemon;
    }

    public void setDaemon(Boolean daemon) {
        this.daemon = daemon;
    }
    public Integer getMaxMissedPings() {
        return maxMissedPings;
    }

    public void setMaxMissedPings(Integer maxMissedPings) {
        this.maxMissedPings = maxMissedPings;
    }
    public Integer getMaxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    public void setMaxReconnectAttempts(Integer maxReconnectAttempts) {
        this.maxReconnectAttempts = maxReconnectAttempts;
    }
    public Integer getShardCount() {
        return shardCount;
    }

    public void setShardCount(Integer shardCount) {
        this.shardCount = shardCount;
    }
    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }
    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BotDTO botDTO = (BotDTO) o;

        if ( ! Objects.equals(id, botDTO.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "BotDTO{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", token='******'" +
            ", autoLogin=" + autoLogin +
            ", daemon=" + daemon +
            ", maxMissedPings=" + maxMissedPings +
            ", maxReconnectAttempts=" + maxReconnectAttempts +
            ", shardCount=" + shardCount +
            ", primary=" + primary +
            ", tags='" + tags + '\'' +
            ", created=" + created +
            ", loggedIn=" + loggedIn +
            ", ready=" + ready +
            '}';
    }
}
