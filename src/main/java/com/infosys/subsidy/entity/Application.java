package com.infosys.subsidy.entity;

import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.VerificationLevel;
import com.infosys.subsidy.enums.VerificationRoute;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_date")
    private LocalDateTime applicationDate;

    @Column(name = "beneficiary_id")
    private Long beneficiaryId;

    @Column(name = "scheme_id")
    private Long schemeId;

    @Column(name = "eligibility_score")
    private Integer eligibilityScore;

    // ==========================================
    // APPLICATION STATUS
    // ==========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ApplicationStatus status;


    // ==========================================
    // CURRENT VERIFICATION LEVEL
    // ==========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "current_verification_level")
    private VerificationLevel currentVerificationLevel;


    @Enumerated(EnumType.STRING)
    @Column(name = "verification_route")
    private VerificationRoute verificationRoute;


    // ==========================================
    // VERIFICATION REMARKS
    // ==========================================

    @Column(name = "remarks", length = 2000)
    private String remarks;


    // ==========================================
    // VERIFICATION TIMESTAMPS
    // ==========================================

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
    
    @jakarta.persistence.Transient
    private LocalDateTime cooldownExpiresAt;
    
    @jakarta.persistence.Transient
    private Boolean cooldownActive;

    @Column(name = "verification_assigned_at")
    private LocalDateTime verificationAssignedAt;

    @Column(name = "verification_due_date")
    private LocalDateTime verificationDueDate;

    @Column(name = "verification_completed_at")
    private LocalDateTime verificationCompletedAt;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Application() {
    }


    public Application(
            LocalDateTime applicationDate,
            Long beneficiaryId,
            Long schemeId,
            Integer eligibilityScore,
            ApplicationStatus status) {

        this.applicationDate = applicationDate;
        this.beneficiaryId = beneficiaryId;
        this.schemeId = schemeId;
        this.eligibilityScore = eligibilityScore;
        this.status = status;
    }


    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

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


    public Long getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(Long beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }


    public Long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(Long schemeId) {
        this.schemeId = schemeId;
    }


    public Integer getEligibilityScore() {
        return eligibilityScore;
    }

    public void setEligibilityScore(Integer eligibilityScore) {
        this.eligibilityScore = eligibilityScore;
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

    public void setCurrentVerificationLevel(
            VerificationLevel currentVerificationLevel) {

        this.currentVerificationLevel =
                currentVerificationLevel;
    }

    public VerificationRoute getVerificationRoute() {
        return verificationRoute;
    }

    public void setVerificationRoute(
            VerificationRoute verificationRoute) {

        this.verificationRoute = verificationRoute;
    }


    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }


    public LocalDateTime getVerificationAssignedAt() {
        return verificationAssignedAt;
    }

    public void setVerificationAssignedAt(
            LocalDateTime verificationAssignedAt) {

        this.verificationAssignedAt =
                verificationAssignedAt;
    }


    public LocalDateTime getVerificationDueDate() {
        return verificationDueDate;
    }

    public void setVerificationDueDate(
            LocalDateTime verificationDueDate) {

        this.verificationDueDate =
                verificationDueDate;
    }


    public LocalDateTime getVerificationCompletedAt() {
        return verificationCompletedAt;
    }

    public void setVerificationCompletedAt(
            LocalDateTime verificationCompletedAt) {

        this.verificationCompletedAt =
                verificationCompletedAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public LocalDateTime getCooldownExpiresAt() {
        return cooldownExpiresAt;
    }

    public void setCooldownExpiresAt(LocalDateTime cooldownExpiresAt) {
        this.cooldownExpiresAt = cooldownExpiresAt;
    }

    public Boolean getCooldownActive() {
        return cooldownActive;
    }

    public void setCooldownActive(Boolean cooldownActive) {
        this.cooldownActive = cooldownActive;
    }
}