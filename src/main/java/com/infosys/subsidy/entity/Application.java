package com.infosys.subsidy.entity;

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

    @Column(name = "status")
    private String status;

    public Application() {
    }

    public Application(
            LocalDateTime applicationDate,
            Long beneficiaryId,
            Long schemeId,
            Integer eligibilityScore,
            String status) {

        this.applicationDate = applicationDate;
        this.beneficiaryId = beneficiaryId;
        this.schemeId = schemeId;
        this.eligibilityScore = eligibilityScore;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}