package top.quantic.sentry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.rightPad;

/**
 * A GameServer.
 */

@Document(collection = "game_server")
public class GameServer extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("address")
    private String address;

    @NotNull
    @Field("name")
    private String name;

    @Field("ping")
    private Integer ping;

    @Field("players")
    private Integer players;

    @Field("max_players")
    private Integer maxPlayers;

    @Field("map")
    private String map;

    @Field("version")
    private Integer version;

    @Field("rcon_password")
    private String rconPassword;

    @Field("sv_password")
    private String svPassword;

    @Field("tv_password")
    private String tvPassword;

    @Field("tv_port")
    private Integer tvPort;

    @NotNull
    @Field("expires")
    private Boolean expires = true;

    @Field("expiration_date")
    private ZonedDateTime expirationDate;

    @Field("expiration_check_date")
    private ZonedDateTime expirationCheckDate;

    @Field("status_check_date")
    private ZonedDateTime statusCheckDate;

    @Field("last_valid_ping")
    private ZonedDateTime lastValidPing;

    @Field("last_rcon_date")
    private ZonedDateTime lastRconDate;

    @Field("last_game_update")
    private ZonedDateTime lastGameUpdate;

    @Field("updating")
    private Boolean updating = false;

    @Field("update_attempts")
    private Integer updateAttempts = 0;

    @Field("last_update_start")
    private ZonedDateTime lastUpdateStart;

    @Field("last_rcon_announce")
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

    public GameServer address(String address) {
        this.address = address;
        return this;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public GameServer name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPing() {
        return ping;
    }

    public GameServer ping(Integer ping) {
        this.ping = ping;
        return this;
    }

    public void setPing(Integer ping) {
        this.ping = ping;
    }

    public Integer getPlayers() {
        return players;
    }

    public GameServer players(Integer players) {
        this.players = players;
        return this;
    }

    public void setPlayers(Integer players) {
        this.players = players;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public GameServer maxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
        return this;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getMap() {
        return map;
    }

    public GameServer map(String map) {
        this.map = map;
        return this;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public Integer getVersion() {
        return version;
    }

    public GameServer version(Integer version) {
        this.version = version;
        return this;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getRconPassword() {
        return rconPassword;
    }

    public GameServer rconPassword(String rconPassword) {
        this.rconPassword = rconPassword;
        return this;
    }

    public void setRconPassword(String rconPassword) {
        this.rconPassword = rconPassword;
    }

    public String getSvPassword() {
        return svPassword;
    }

    public GameServer svPassword(String svPassword) {
        this.svPassword = svPassword;
        return this;
    }

    public void setSvPassword(String svPassword) {
        this.svPassword = svPassword;
    }

    public String getTvPassword() {
        return tvPassword;
    }

    public GameServer tvPassword(String tvPassword) {
        this.tvPassword = tvPassword;
        return this;
    }

    public void setTvPassword(String tvPassword) {
        this.tvPassword = tvPassword;
    }

    public Integer getTvPort() {
        return tvPort;
    }

    public GameServer tvPort(Integer tvPort) {
        this.tvPort = tvPort;
        return this;
    }

    public void setTvPort(Integer tvPort) {
        this.tvPort = tvPort;
    }

    public Boolean isExpires() {
        return expires;
    }

    public GameServer expires(Boolean expires) {
        this.expires = expires;
        return this;
    }

    public void setExpires(Boolean expires) {
        this.expires = expires;
    }

    public ZonedDateTime getExpirationDate() {
        return expirationDate;
    }

    public GameServer expirationDate(ZonedDateTime expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    public void setExpirationDate(ZonedDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public ZonedDateTime getExpirationCheckDate() {
        return expirationCheckDate;
    }

    public GameServer expirationCheckDate(ZonedDateTime expirationCheckDate) {
        this.expirationCheckDate = expirationCheckDate;
        return this;
    }

    public void setExpirationCheckDate(ZonedDateTime expirationCheckDate) {
        this.expirationCheckDate = expirationCheckDate;
    }

    public ZonedDateTime getStatusCheckDate() {
        return statusCheckDate;
    }

    public GameServer statusCheckDate(ZonedDateTime statusCheckDate) {
        this.statusCheckDate = statusCheckDate;
        return this;
    }

    public void setStatusCheckDate(ZonedDateTime statusCheckDate) {
        this.statusCheckDate = statusCheckDate;
    }

    public ZonedDateTime getLastValidPing() {
        return lastValidPing;
    }

    public GameServer lastValidPing(ZonedDateTime lastValidPing) {
        this.lastValidPing = lastValidPing;
        return this;
    }

    public void setLastValidPing(ZonedDateTime lastValidPing) {
        this.lastValidPing = lastValidPing;
    }

    public ZonedDateTime getLastRconDate() {
        return lastRconDate;
    }

    public GameServer lastRconDate(ZonedDateTime lastRconDate) {
        this.lastRconDate = lastRconDate;
        return this;
    }

    public void setLastRconDate(ZonedDateTime lastRconDate) {
        this.lastRconDate = lastRconDate;
    }

    public ZonedDateTime getLastGameUpdate() {
        return lastGameUpdate;
    }

    public GameServer lastGameUpdate(ZonedDateTime lastGameUpdate) {
        this.lastGameUpdate = lastGameUpdate;
        return this;
    }

    public void setLastGameUpdate(ZonedDateTime lastGameUpdate) {
        this.lastGameUpdate = lastGameUpdate;
    }

    public Boolean isUpdating() {
        return updating;
    }

    public GameServer updating(Boolean updating) {
        this.updating = updating;
        return this;
    }

    public void setUpdating(Boolean updating) {
        this.updating = updating;
    }

    public Integer getUpdateAttempts() {
        return updateAttempts;
    }

    public GameServer updateAttempts(Integer updateAttempts) {
        this.updateAttempts = updateAttempts;
        return this;
    }

    public void setUpdateAttempts(Integer updateAttempts) {
        this.updateAttempts = updateAttempts;
    }

    public ZonedDateTime getLastUpdateStart() {
        return lastUpdateStart;
    }

    public GameServer lastUpdateStart(ZonedDateTime lastUpdateStart) {
        this.lastUpdateStart = lastUpdateStart;
        return this;
    }

    public void setLastUpdateStart(ZonedDateTime lastUpdateStart) {
        this.lastUpdateStart = lastUpdateStart;
    }

    public ZonedDateTime getLastRconAnnounce() {
        return lastRconAnnounce;
    }

    public GameServer lastRconAnnounce(ZonedDateTime lastRconAnnounce) {
        this.lastRconAnnounce = lastRconAnnounce;
        return this;
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
        GameServer gameServer = (GameServer) o;
        if (gameServer.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, gameServer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String getShortNameAndAddress() {
        return getShortName() + " (" + address + ")";
    }

    public String getShortName() {
        return name == null ? toFullString() :
            name.trim().replaceAll("(^[A-Za-z]{3})[^0-9]*([0-9]+).*", "$1$2").toLowerCase();
    }

    @Override
    public String toString() {
        return getShortNameAndAddress();
    }

    public String getSummary() {
        return String.format("[%s] %s %s %s v%d%s", getShortName(),
            leftPad(address, 25 - Math.min(5, getShortName().length())),
            rightPad(map, 20), leftPad(players + " / " + maxPlayers, 7),
            version, tvPort > 0 ? " with SourceTV @ port " + tvPort : "");
    }

    public String toFullString() {
        return "GameServer{" +
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
            ", tvPassword='" + tvPassword + "'" +
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
