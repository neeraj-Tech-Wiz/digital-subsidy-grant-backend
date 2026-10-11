package com.infosys.subsidy.dto;

import java.time.LocalDateTime;

public class VerificationHistoryDTO {

    private String officerName;
    private String officerRole;
    private String verificationLevel;
    private String action;
    private String remarks;
    private LocalDateTime actionTimestamp;

    private Long milestoneId;
    private Long fundReleaseId;
    private java.math.BigDecimal amount;
    private String previousStatus;
    private String newStatus;

    public VerificationHistoryDTO() {}

    public VerificationHistoryDTO(String officerName, String officerRole, String verificationLevel, String action, String remarks, LocalDateTime actionTimestamp, Long milestoneId, Long fundReleaseId, java.math.BigDecimal amount, String previousStatus, String newStatus) {
        this.officerName = officerName;
        this.officerRole = officerRole;
        this.verificationLevel = verificationLevel;
        this.action = action;
        this.remarks = remarks;
        this.actionTimestamp = actionTimestamp;
        this.milestoneId = milestoneId;
        this.fundReleaseId = fundReleaseId;
        this.amount = amount;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public String getOfficerName() {
        return officerName;
    }

    public void setOfficerName(String officerName) {
        this.officerName = officerName;
    }

    public String getOfficerRole() {
        return officerRole;
    }

    public void setOfficerRole(String officerRole) {
        this.officerRole = officerRole;
    }

    public String getVerificationLevel() {
        return verificationLevel;
    }

    public void setVerificationLevel(String verificationLevel) {
        this.verificationLevel = verificationLevel;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(LocalDateTime actionTimestamp) {
        this.actionTimestamp = actionTimestamp;
    }

    public Long getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(Long milestoneId) {
        this.milestoneId = milestoneId;
    }

    public Long getFundReleaseId() {
        return fundReleaseId;
    }

    public void setFundReleaseId(Long fundReleaseId) {
        this.fundReleaseId = fundReleaseId;
    }

    public java.math.BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(java.math.BigDecimal amount) {
        this.amount = amount;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
}
