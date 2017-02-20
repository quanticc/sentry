package top.quantic.sentry.web.rest.vm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static top.quantic.sentry.config.Constants.UGC_DATE_FORMAT;
import static top.quantic.sentry.service.util.DateUtil.parseLongDate;

public class UgcResults {

    private String ladder;
    private Long season;
    private Long week;
    private List<Match> matches = new ArrayList<>();

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

    public List<Match> getMatches() {
        return matches;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }

    public static class Match {

        private Long matchId;
        private Long schedId;
        private Instant schedDt;
        private String mapName;
        private Long clanIdH;
        private String homeTeam;
        private Long clanIdV;
        private Integer noScoreR1H;
        private Integer noScoreR2H;
        private Integer noScoreR3H;
        private String visitingTeam;
        private Integer noScoreR1V;
        private Integer noScoreR2V;
        private Integer noScoreR3V;
        private Long winner;
        private String winningTeam;

        public Long getMatchId() {
            return matchId;
        }

        public void setMatchId(Long matchId) {
            this.matchId = matchId;
        }

        public Long getSchedId() {
            return schedId;
        }

        public void setSchedId(Long schedId) {
            this.schedId = schedId;
        }

        public Instant getSchedDt() {
            return schedDt;
        }

        public void setSchedDt(String schedDt) {
            this.schedDt = parseLongDate(schedDt, UGC_DATE_FORMAT);
        }

        public void setSchedDt(Instant schedDt) {
            this.schedDt = schedDt;
        }

        public String getMapName() {
            return mapName;
        }

        public void setMapName(String mapName) {
            this.mapName = mapName;
        }

        public Long getClanIdH() {
            return clanIdH;
        }

        public void setClanIdH(Long clanIdH) {
            this.clanIdH = clanIdH;
        }

        public String getHomeTeam() {
            return homeTeam;
        }

        public void setHomeTeam(String homeTeam) {
            this.homeTeam = homeTeam;
        }

        public Long getClanIdV() {
            return clanIdV;
        }

        public void setClanIdV(Long clanIdV) {
            this.clanIdV = clanIdV;
        }

        public Integer getNoScoreR1H() {
            return noScoreR1H;
        }

        public void setNoScoreR1H(Integer noScoreR1H) {
            this.noScoreR1H = noScoreR1H;
        }

        public Integer getNoScoreR2H() {
            return noScoreR2H;
        }

        public void setNoScoreR2H(Integer noScoreR2H) {
            this.noScoreR2H = noScoreR2H;
        }

        public Integer getNoScoreR3H() {
            return noScoreR3H;
        }

        public void setNoScoreR3H(Integer noScoreR3H) {
            this.noScoreR3H = noScoreR3H;
        }

        public String getVisitingTeam() {
            return visitingTeam;
        }

        public void setVisitingTeam(String visitingTeam) {
            this.visitingTeam = visitingTeam;
        }

        public Integer getNoScoreR1V() {
            return noScoreR1V;
        }

        public void setNoScoreR1V(Integer noScoreR1V) {
            this.noScoreR1V = noScoreR1V;
        }

        public Integer getNoScoreR2V() {
            return noScoreR2V;
        }

        public void setNoScoreR2V(Integer noScoreR2V) {
            this.noScoreR2V = noScoreR2V;
        }

        public Integer getNoScoreR3V() {
            return noScoreR3V;
        }

        public void setNoScoreR3V(Integer noScoreR3V) {
            this.noScoreR3V = noScoreR3V;
        }

        public Long getWinner() {
            return winner;
        }

        public void setWinner(Long winner) {
            this.winner = winner;
        }

        public String getWinningTeam() {
            return winningTeam;
        }

        public void setWinningTeam(String winningTeam) {
            this.winningTeam = winningTeam;
        }
    }
}
