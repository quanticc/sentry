package top.quantic.sentry.service.dto;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;


/**
 * A DTO for the Streamer entity.
 */
public class StreamerDTO implements Serializable {

    private String id;

    @NotNull
    private String provider;

    @NotNull
    private String name;

    private String league;

    private String division;

    private String titleFilter;

    private String announcement;

    private ZonedDateTime lastAnnouncement;

    @NotNull
    private Boolean enabled;

    @NotNull
    private Map<String, Object> embedFields;

    private Long lastStreamId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLeague() {
        return league;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getTitleFilter() {
        return titleFilter;
    }

    public void setTitleFilter(String titleFilter) {
        this.titleFilter = titleFilter;
    }

    public String getAnnouncement() {
        return announcement;
    }

    public void setAnnouncement(String announcement) {
        this.announcement = announcement;
    }

    public ZonedDateTime getLastAnnouncement() {
        return lastAnnouncement;
    }

    public void setLastAnnouncement(ZonedDateTime lastAnnouncement) {
        this.lastAnnouncement = lastAnnouncement;
    }

    public Boolean getEnabled() {
        return enabled;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StreamerDTO streamerDTO = (StreamerDTO) o;

        return Objects.equals(id, streamerDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "StreamerDTO{" +
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
            '}';
    }

    public Long getLastStreamId() {
        return lastStreamId;
    }

    public void setLastStreamId(Long lastStreamId) {
        this.lastStreamId = lastStreamId;
    }
}
