package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

import static top.quantic.sentry.config.Constants.UGC_DATE_FORMAT;
import static top.quantic.sentry.service.util.DateUtil.parseLongDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UgcTransaction {

    private Long clanId;
    private Long playerCommid;
    private Instant dtAdded;
    private Instant dtDropped;

    public Long getClanId() {
        return clanId;
    }

    public void setClanId(Long clanId) {
        this.clanId = clanId;
    }

    public Long getPlayerCommid() {
        return playerCommid;
    }

    public void setPlayerCommid(Long playerCommid) {
        this.playerCommid = playerCommid;
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

    public Instant getDtDropped() {
        return dtDropped;
    }

    public void setDtDropped(String dtDropped) {
        this.dtDropped = parseLongDate(dtDropped, UGC_DATE_FORMAT);
    }

    public void setDtDropped(Instant dtDropped) {
        this.dtDropped = dtDropped;
    }
}
