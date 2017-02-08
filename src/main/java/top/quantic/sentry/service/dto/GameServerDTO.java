package top.quantic.sentry.service.dto;

import java.time.ZonedDateTime;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;


/**
 * A DTO for the GameServer entity.
 */
public class GameServerDTO implements Serializable {

    private String id;

    @NotNull
    private String address;

    @NotNull
    private String name;

    private Integer ping;

    private Integer players;

    private Integer maxPlayers;

    private String map;

    private Integer version;

    private String rconPassword;

    private String svPassword;

    private Integer tvPort;

    @NotNull
    private Boolean expires;

    private ZonedDateTime expirationDate;

    private ZonedDateTime expirationCheckDate;

    private ZonedDateTime statusCheckDate;

    private ZonedDateTime lastValidPing;

    private ZonedDateTime lastRconDate;

    private ZonedDateTime lastGameUpdate;

    private Boolean updating;

    private Integer updateAttempts;

    private ZonedDateTime lastUpdateStart;

    private ZonedDateTime lastRconAnnounce;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public Integer getPing() {
        return ping;
    }

    public void setPing(Integer ping) {
        this.ping = ping;
    }
    public Integer getPlayers() {
        return players;
    }

    public void setPlayers(Integer players) {
        this.players = players;
    }
    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
    public String getRconPassword() {
        return rconPassword;
    }

    public void setRconPassword(String rconPassword) {
        this.rconPassword = rconPassword;
    }
    public String getSvPassword() {
        return svPassword;
    }

    public void setSvPassword(String svPassword) {
        this.svPassword = svPassword;
    }
    public Integer getTvPort() {
        return tvPort;
    }

    public void setTvPort(Integer tvPort) {
        this.tvPort = tvPort;
    }
    public Boolean getExpires() {
        return expires;
    }

    public void setExpires(Boolean expires) {
        this.expires = expires;
    }
    public ZonedDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(ZonedDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }
    public ZonedDateTime getExpirationCheckDate() {
        return expirationCheckDate;
    }

    public void setExpirationCheckDate(ZonedDateTime expirationCheckDate) {
        this.expirationCheckDate = expirationCheckDate;
    }
    public ZonedDateTime getStatusCheckDate() {
        return statusCheckDate;
    }

    public void setStatusCheckDate(ZonedDateTime statusCheckDate) {
        this.statusCheckDate = statusCheckDate;
    }
    public ZonedDateTime getLastValidPing() {
        return lastValidPing;
    }

    public void setLastValidPing(ZonedDateTime lastValidPing) {
        this.lastValidPing = lastValidPing;
    }
    public ZonedDateTime getLastRconDate() {
        return lastRconDate;
    }

    public void setLastRconDate(ZonedDateTime lastRconDate) {
        this.lastRconDate = lastRconDate;
    }
    public ZonedDateTime getLastGameUpdate() {
        return lastGameUpdate;
    }

    public void setLastGameUpdate(ZonedDateTime lastGameUpdate) {
        this.lastGameUpdate = lastGameUpdate;
    }
    public Boolean getUpdating() {
        return updating;
    }

    public void setUpdating(Boolean updating) {
        this.updating = updating;
    }
    public Integer getUpdateAttempts() {
        return updateAttempts;
    }

    public void setUpdateAttempts(Integer updateAttempts) {
        this.updateAttempts = updateAttempts;
    }
    public ZonedDateTime getLastUpdateStart() {
        return lastUpdateStart;
    }

    public void setLastUpdateStart(ZonedDateTime lastUpdateStart) {
        this.lastUpdateStart = lastUpdateStart;
    }
    public ZonedDateTime getLastRconAnnounce() {
        return lastRconAnnounce;
    }

    public void setLastRconAnnounce(ZonedDateTime lastRconAnnounce) {
        this.lastRconAnnounce = lastRconAnnounce;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GameServerDTO gameServerDTO = (GameServerDTO) o;

        return Objects.equals(id, gameServerDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "GameServerDTO{" +
            "id=" + id +
            ", address='" + address + "'" +
            ", name='" + name + "'" +
            ", ping='" + ping + "'" +
            ", players='" + players + "'" +
            ", maxPlayers='" + maxPlayers + "'" +
            ", map='" + map + "'" +
            ", version='" + version + "'" +
            ", rconPassword='" + rconPassword + "'" +
            ", svPassword='" + svPassword + "'" +
            ", tvPort='" + tvPort + "'" +
            ", expires='" + expires + "'" +
            ", expirationDate='" + expirationDate + "'" +
            ", expirationCheckDate='" + expirationCheckDate + "'" +
            ", statusCheckDate='" + statusCheckDate + "'" +
            ", lastValidPing='" + lastValidPing + "'" +
            ", lastRconDate='" + lastRconDate + "'" +
            ", lastGameUpdate='" + lastGameUpdate + "'" +
            ", updating='" + updating + "'" +
            ", updateAttempts='" + updateAttempts + "'" +
            ", lastUpdateStart='" + lastUpdateStart + "'" +
            ", lastRconAnnounce='" + lastRconAnnounce + "'" +
            '}';
    }
}
