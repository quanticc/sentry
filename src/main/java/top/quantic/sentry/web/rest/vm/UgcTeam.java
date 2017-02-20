package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static top.quantic.sentry.config.Constants.UGC_DATE_FORMAT;
import static top.quantic.sentry.service.util.DateUtil.parseLongDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UgcTeam {

    private Long clanId;
    private String clanTag;
    private String clanName;
    private String status;
    private String clanDes;
    private String clanTitles;
    private String clanSteampage;
    private String clanIrc;
    private String clanUrl;
    private String clanAvatar;
    private String clanServer1;
    private String clanServer2;
    private String clanTimezone;
    private String ladShort;
    private String divName;
    private List<RosteredPlayer> roster = new ArrayList<>();

    public Long getClanId() {
        return clanId;
    }

    public void setClanId(Long clanId) {
        this.clanId = clanId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClanDes() {
        return clanDes;
    }

    public void setClanDes(String clanDes) {
        this.clanDes = clanDes;
    }

    public String getClanTitles() {
        return clanTitles;
    }

    public void setClanTitles(String clanTitles) {
        this.clanTitles = clanTitles;
    }

    public String getClanSteampage() {
        return clanSteampage;
    }

    public void setClanSteampage(String clanSteampage) {
        this.clanSteampage = clanSteampage != null ? clanSteampage.replace("\\", "") : null;
    }

    public String getClanIrc() {
        return clanIrc;
    }

    public void setClanIrc(String clanIrc) {
        this.clanIrc = clanIrc;
    }

    public String getClanUrl() {
        return clanUrl;
    }

    public void setClanUrl(String clanUrl) {
        this.clanUrl = clanUrl;
    }

    public String getClanAvatar() {
        return clanAvatar;
    }

    public void setClanAvatar(String clanAvatar) {
        this.clanAvatar = clanAvatar != null ? clanAvatar.replace("\\", "") : null;
    }

    public String getClanServer1() {
        return clanServer1;
    }

    public void setClanServer1(String clanServer1) {
        this.clanServer1 = clanServer1;
    }

    public String getClanServer2() {
        return clanServer2;
    }

    public void setClanServer2(String clanServer2) {
        this.clanServer2 = clanServer2;
    }

    public String getClanTimezone() {
        return clanTimezone;
    }

    public void setClanTimezone(String clanTimezone) {
        this.clanTimezone = clanTimezone;
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

    public List<RosteredPlayer> getRoster() {
        return roster;
    }

    public void setRoster(List<RosteredPlayer> roster) {
        this.roster = roster;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RosteredPlayer {

        private String memName;
        private String memType;
        private Instant dtAdded;
        private Instant dtUpdated;
        private String memSteam;
        private Long sid;
        private String playerAvatar;

        public String getMemName() {
            return memName;
        }

        public void setMemName(String memName) {
            this.memName = memName;
        }

        public String getMemType() {
            return memType;
        }

        public void setMemType(String memType) {
            this.memType = memType;
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

        public Instant getDtUpdated() {
            return dtUpdated;
        }

        public void setDtUpdated(String dtUpdated) {
            this.dtUpdated = parseLongDate(dtUpdated, UGC_DATE_FORMAT);
        }

        public void setDtUpdated(Instant dtUpdated) {
            this.dtUpdated = dtUpdated;
        }

        public String getMemSteam() {
            return memSteam;
        }

        public void setMemSteam(String memSteam) {
            this.memSteam = memSteam;
        }

        public Long getSid() {
            return sid;
        }

        public void setSid(Long sid) {
            this.sid = sid;
        }

        public String getPlayerAvatar() {
            return playerAvatar;
        }

        public void setPlayerAvatar(String playerAvatar) {
            this.playerAvatar = playerAvatar != null ? playerAvatar.replace("\\", "") : null;
        }
    }
}
