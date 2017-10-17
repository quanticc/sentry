package top.quantic.sentry.web.rest.vm;

import java.time.ZonedDateTime;

public class UgcTeamTicket {

	private Long id;
	private Long playerCommunityId;
	private Long battleNetId;
	private String createdBy;
	private Integer divisionId;
	private Integer ladderId;
	private Integer type;
	private String subject;
	private String body;
	private ZonedDateTime created;
	private String status;
	private ZonedDateTime updated;
	private String updatedBy;
	private String lastAction;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPlayerCommunityId() {
		return playerCommunityId;
	}

	public void setPlayerCommunityId(Long playerCommunityId) {
		this.playerCommunityId = playerCommunityId;
	}

	public Long getBattleNetId() {
		return battleNetId;
	}

	public void setBattleNetId(Long battleNetId) {
		this.battleNetId = battleNetId;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Integer getDivisionId() {
		return divisionId;
	}

	public void setDivisionId(Integer divisionId) {
		this.divisionId = divisionId;
	}

	public Integer getLadderId() {
		return ladderId;
	}

	public void setLadderId(Integer ladderId) {
		this.ladderId = ladderId;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public ZonedDateTime getCreated() {
		return created;
	}

	public void setCreated(ZonedDateTime created) {
		this.created = created;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public ZonedDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(ZonedDateTime updated) {
		this.updated = updated;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getLastAction() {
		return lastAction;
	}

	public void setLastAction(String lastAction) {
		this.lastAction = lastAction;
	}
}
