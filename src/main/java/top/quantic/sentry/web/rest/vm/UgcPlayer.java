package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UgcPlayer {

    @JsonProperty("ugc_page")
    private String ugcPage;
    private List<Membership> team = new ArrayList<>();

    public String getUgcPage() {
        return ugcPage;
    }

    public void setUgcPage(String ugcPage) {
        this.ugcPage = ugcPage;
    }

    public List<Membership> getTeam() {
        return team;
    }

    public void setTeam(List<Membership> team) {
        this.team = team;
    }

    @Override
    public String toString() {
        return "UgcPlayer{" +
            "ugcPage='" + ugcPage + '\'' +
            ", team=" + team +
            '}';
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Membership {

        private String name;
        private String tag;
        @JsonProperty("clan_id")
        private String clanId;
        private String format;
        private String division;
        private String status;
        @JsonProperty("last_updated")
        private String lastUpdated;
        @JsonProperty("joined_team")
        private String joinedTeam;
        private String active;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getClanId() {
            return clanId;
        }

        public void setClanId(String clanId) {
            this.clanId = clanId;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getDivision() {
            return division;
        }

        public void setDivision(String division) {
            this.division = division;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(String lastUpdated) {
            this.lastUpdated = lastUpdated;
        }

        public String getJoinedTeam() {
            return joinedTeam;
        }

        public void setJoinedTeam(String joinedTeam) {
            this.joinedTeam = joinedTeam;
        }

        public String getActive() {
            return active;
        }

        public void setActive(String active) {
            this.active = active;
        }

        @Override
        public String toString() {
            return "Membership{" +
                "name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                ", clanId='" + clanId + '\'' +
                ", format='" + format + '\'' +
                ", division='" + division + '\'' +
                ", status='" + status + '\'' +
                ", lastUpdated='" + lastUpdated + '\'' +
                ", joinedTeam='" + joinedTeam + '\'' +
                ", active='" + active + '\'' +
                '}';
        }
    }
}
