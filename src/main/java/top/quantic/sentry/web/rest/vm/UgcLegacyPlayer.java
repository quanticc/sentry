package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static top.quantic.sentry.config.Constants.UGC_DATE_FORMAT;
import static top.quantic.sentry.service.util.DateUtil.parseLongDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UgcLegacyPlayer {

    private Long id;
    private List<Membership> teams = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Membership> getTeams() {
        return teams;
    }

    public void setTeams(List<Membership> teams) {
        this.teams = teams;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Membership {

        private Long memId;
        private Long clanId;
        private String memName;
        private String playerSteamid; // deprecated - contained steamId32
        private Long playerCommid; // id64
        private String deleted; // "N" for not deleted
        private Instant dtAdded;
        private String clanTag;
        private String clanName;
        private String ladShort;
        private String divName;

        public Long getMemId() {
            return memId;
        }

        public void setMemId(Long memId) {
            this.memId = memId;
        }

        public Long getClanId() {
            return clanId;
        }

        public void setClanId(Long clanId) {
            this.clanId = clanId;
        }

        public String getMemName() {
            return memName;
        }

        public void setMemName(String memName) {
            this.memName = memName;
        }

        public String getPlayerSteamid() {
            return playerSteamid;
        }

        public void setPlayerSteamid(String playerSteamid) {
            this.playerSteamid = playerSteamid;
        }

        public Long getPlayerCommid() {
            return playerCommid;
        }

        public void setPlayerCommid(Long playerCommid) {
            this.playerCommid = playerCommid;
        }

        public String getDeleted() {
            return deleted;
        }

        public void setDeleted(String deleted) {
            this.deleted = deleted;
        }

        public Instant getDtAdded() {
            return dtAdded;
        }

        public void setDtAdded(String dtAdded) {
            this.dtAdded = parseLongDate(dtAdded, UGC_DATE_FORMAT);
        }

        public void setDtAdded(Instant dtAdded) {
            this.dtAdded = dtAdded;
        }

        public String getClanTag() {
            return clanTag;
        }

        public void setClanTag(String clanTag) {
            this.clanTag = clanTag;
        }

        public String getClanName() {
            return clanName;
        }

        public void setClanName(String clanName) {
            this.clanName = clanName;
        }

        public String getLadShort() {
            return ladShort;
        }

        public void setLadShort(String ladShort) {
            this.ladShort = ladShort;
        }

        public String getDivName() {
            return divName;
        }

        public void setDivName(String divName) {
            this.divName = divName;
        }
    }
}
