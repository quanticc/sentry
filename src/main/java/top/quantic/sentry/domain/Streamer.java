package top.quantic.sentry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A Streamer.
 */

@Document(collection = "streamer")
public class Streamer extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("provider")
    private String provider;

    @NotNull
    @Field("name")
    private String name;

    @Field("league")
    private String league;

    @Field("division")
    private String division;

    @Field("title_filter")
    private String titleFilter;

    @Field("announcement")
    private String announcement;

    @Field("last_announcement")
    private ZonedDateTime lastAnnouncement = Instant.EPOCH.atZone(ZoneId.systemDefault());

    @NotNull
    @Field("enabled")
    private Boolean enabled = true;

    @NotNull
    @Field("embed_fields")
    private Map<String, Object> embedFields = new LinkedHashMap<>();

    @Field("last_stream_id")
    private Long lastStreamId = 0L;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public Streamer provider(String provider) {
        this.provider = provider;
        return this;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public Streamer name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLeague() {
        return league;
    }

    public Streamer league(String league) {
        this.league = league;
        return this;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public String getDivision() {
        return division;
    }

    public Streamer division(String division) {
        this.division = division;
        return this;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getTitleFilter() {
        return titleFilter;
    }

    public Streamer titleFilter(String titleFilter) {
        this.titleFilter = titleFilter;
        return this;
    }

    public void setTitleFilter(String titleFilter) {
        this.titleFilter = titleFilter;
    }

    public String getAnnouncement() {
        return announcement;
    }

    public Streamer announcement(String announcement) {
        this.announcement = announcement;
        return this;
    }

    public void setAnnouncement(String announcement) {
        this.announcement = announcement;
    }

    public ZonedDateTime getLastAnnouncement() {
        return lastAnnouncement;
    }

    public Streamer lastAnnouncement(ZonedDateTime lastAnnouncement) {
        this.lastAnnouncement = lastAnnouncement;
        return this;
    }

    public void setLastAnnouncement(ZonedDateTime lastAnnouncement) {
        this.lastAnnouncement = lastAnnouncement;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Streamer enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getEmbedFields() {
        return embedFields;
    }

    public void setEmbedFields(Map<String, Object> embedFields) {
        this.embedFields = embedFields;
    }

    public Long getLastStreamId() {
        return lastStreamId;
    }

    public void setLastStreamId(Long lastStreamId) {
        this.lastStreamId = lastStreamId;
    }

    public Streamer lastStreamId(Long lastStreamId) {
        this.lastStreamId = lastStreamId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Streamer streamer = (Streamer) o;
        if (streamer.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, streamer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String toShortString() {
        String result = "**" + provider + ":" + name + "**";
        if (league != null) {
            result += " (" + league;
            if (division != null) {
                result += " " + division;
            }
            result += ")";
        }
        if (titleFilter != null) {
            result += " with title containing: " + titleFilter;
        }
        if (!enabled) {
            result += " [disabled]";
        }
        return result;
    }

    @Override
    public String toString() {
        return "Streamer{" +
            "id=" + id +
            ", provider='" + provider + "'" +
            ", name='" + name + "'" +
            ", league='" + league + "'" +
            ", division='" + division + "'" +
            ", titleFilter='" + titleFilter + "'" +
            ", announcement='" + announcement + "'" +
            ", lastAnnouncement='" + lastAnnouncement + "'" +
            ", lastStreamId='" + lastStreamId + "'" +
            ", enabled='" + enabled + "'" +
            ", embedFields='" + embedFields + "'" +
            '}';
    }
}
