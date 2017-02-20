package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UgcSchedule {

    private String ladder;
    private Long season;
    private Long week;
    private List<Match> schedule = new ArrayList<>();

    public String getLadder() {
        return ladder;
    }

    public void setLadder(String ladder) {
        this.ladder = ladder;
    }

    public Long getSeason() {
        return season;
    }

    public void setSeason(Long season) {
        this.season = season;
    }

    public Long getWeek() {
        return week;
    }

    public void setWeek(Long week) {
        this.week = week;
    }

    public List<Match> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<Match> schedule) {
        this.schedule = schedule;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Match {
        private Long matchId;
        private Long clanIdV;
        private Long clanIdH;
        private Long divId;
        private String divName;

        public Long getMatchId() {
            return matchId;
        }

        public void setMatchId(Long matchId) {
            this.matchId = matchId;
        }

        public Long getClanIdV() {
            return clanIdV;
        }

        public void setClanIdV(Long clanIdV) {
            this.clanIdV = clanIdV;
        }

        public Long getClanIdH() {
            return clanIdH;
        }

        public void setClanIdH(Long clanIdH) {
            this.clanIdH = clanIdH;
        }

        public Long getDivId() {
            return divId;
        }

        public void setDivId(Long divId) {
            this.divId = divId;
        }

        public String getDivName() {
            return divName;
        }

        public void setDivName(String divName) {
            this.divName = divName;
        }
    }

}
