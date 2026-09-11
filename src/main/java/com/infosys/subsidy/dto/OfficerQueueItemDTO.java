package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.VerificationLevel;
import com.infosys.subsidy.enums.VerificationRoute;
import java.time.LocalDateTime;

public class OfficerQueueItemDTO {
    private Long id;
    private LocalDateTime applicationDate;
    private ApplicationStatus status;
    private VerificationLevel currentVerificationLevel;
    private VerificationRoute verificationRoute;
    private Integer eligibilityScore;
    
    // Additional context
    private String beneficiaryName;
    private String schemeName;
    private LocalDateTime verificationAssignedAt;
    private LocalDateTime verificationDueDate;
    private String remarks;

    public OfficerQueueItemDTO() {
    }

    public OfficerQueueItemDTO(Long id, LocalDateTime applicationDate, ApplicationStatus status,
                               VerificationLevel currentVerificationLevel, VerificationRoute verificationRoute,
                               Integer eligibilityScore, String beneficiaryName, String schemeName,
                               LocalDateTime verificationAssignedAt, LocalDateTime verificationDueDate, String remarks) {
        this.id = id;
        this.applicationDate = applicationDate;
        this.status = status;
        this.currentVerificationLevel = currentVerificationLevel;
        this.verificationRoute = verificationRoute;
        this.eligibilityScore = eligibilityScore;
        this.beneficiaryName = beneficiaryName;
        this.schemeName = schemeName;
        this.verificationAssignedAt = verificationAssignedAt;
        this.verificationDueDate = verificationDueDate;
        this.remarks = remarks;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDateTime applicationDate) {
        this.applicationDate = applicationDate;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public VerificationLevel getCurrentVerificationLevel() {
        return currentVerificationLevel;
    }

    public void setCurrentVerificationLevel(VerificationLevel currentVerificationLevel) {
        this.currentVerificationLevel = currentVerificationLevel;
    }

    public VerificationRoute getVerificationRoute() {
        return verificationRoute;
    }

    public void setVerificationRoute(VerificationRoute verificationRoute) {
        this.verificationRoute = verificationRoute;
    }

    public Integer getEligibilityScore() {
        return eligibilityScore;
    }

    public void setEligibilityScore(Integer eligibilityScore) {
        this.eligibilityScore = eligibilityScore;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }

    public LocalDateTime getVerificationAssignedAt() {
        return verificationAssignedAt;
    }

    public void setVerificationAssignedAt(LocalDateTime verificationAssignedAt) {
        this.verificationAssignedAt = verificationAssignedAt;
    }

    public LocalDateTime getVerificationDueDate() {
        return verificationDueDate;
    }

    public void setVerificationDueDate(LocalDateTime verificationDueDate) {
        this.verificationDueDate = verificationDueDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
