package com.infosys.subsidy.dto;

import java.time.LocalDateTime;

public class VerificationHistoryDTO {

    private String officerName;
    private String officerRole;
    private String verificationLevel;
    private String action;
    private String remarks;
    private LocalDateTime actionTimestamp;

    public VerificationHistoryDTO() {}

    public VerificationHistoryDTO(String officerName, String officerRole, String verificationLevel, String action, String remarks, LocalDateTime actionTimestamp) {
        this.officerName = officerName;
        this.officerRole = officerRole;
        this.verificationLevel = verificationLevel;
        this.action = action;
        this.remarks = remarks;
        this.actionTimestamp = actionTimestamp;
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
}
