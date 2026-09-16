package com.infosys.subsidy.dto;

import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.enums.BeneficiaryCategory;
import com.infosys.subsidy.enums.SchemeStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class SchemeResponse {

    private Long id;
    private String schemeCode;
    private String schemeName;
    private String description;
    private BigDecimal grantAmount;
    private BigDecimal totalBudget;
    private BigDecimal disbursedAmount;
    private BigDecimal remainingBudget;
    private SchemeStatus status;
    private String applicableRegion;
    private BeneficiaryCategory beneficiaryCategory;
    private int criteriaCount;
    private int totalCriteriaWeight;
    private long mandatoryCriteriaCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EligibilityCriteriaResponse> criteriaList = new ArrayList<>();

    public SchemeResponse() {
    }

    public static SchemeResponse fromEntity(Scheme entity) {
        if (entity == null) return null;
        SchemeResponse resp = new SchemeResponse();
        resp.setId(entity.getId());
        resp.setSchemeCode(entity.getSchemeCode());
        resp.setSchemeName(entity.getSchemeName());
        resp.setDescription(entity.getDescription());
        resp.setGrantAmount(entity.getGrantAmount());
        resp.setTotalBudget(entity.getTotalBudget());
        resp.setDisbursedAmount(entity.getDisbursedAmount());
        resp.setRemainingBudget(entity.getRemainingBudget());
        resp.setStatus(entity.getStatus());
        resp.setApplicableRegion(entity.getApplicableRegion());
        resp.setBeneficiaryCategory(entity.getBeneficiaryCategory());
        resp.setCriteriaCount(entity.getCriteriaList() != null ? entity.getCriteriaList().size() : 0);
        resp.setTotalCriteriaWeight(entity.getTotalCriteriaWeight());
        resp.setMandatoryCriteriaCount(entity.getMandatoryCriteriaCount());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCriteriaList() != null) {
            List<EligibilityCriteriaResponse> critResp = entity.getCriteriaList().stream()
                    .map(EligibilityCriteriaResponse::fromEntity)
                    .toList();
            resp.setCriteriaList(critResp);
        }

        return resp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSchemeCode() {
        return schemeCode;
    }

    public void setSchemeCode(String schemeCode) {
        this.schemeCode = schemeCode;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getGrantAmount() {
        return grantAmount;
    }

    public void setGrantAmount(BigDecimal grantAmount) {
        this.grantAmount = grantAmount;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public BigDecimal getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(BigDecimal remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public SchemeStatus getStatus() {
        return status;
    }

    public void setStatus(SchemeStatus status) {
        this.status = status;
    }

    public String getApplicableRegion() {
        return applicableRegion;
    }

    public void setApplicableRegion(String applicableRegion) {
        this.applicableRegion = applicableRegion;
    }

    public BeneficiaryCategory getBeneficiaryCategory() {
        return beneficiaryCategory;
    }

    public void setBeneficiaryCategory(BeneficiaryCategory beneficiaryCategory) {
        this.beneficiaryCategory = beneficiaryCategory;
    }

    public int getCriteriaCount() {
        return criteriaCount;
    }

    public void setCriteriaCount(int criteriaCount) {
        this.criteriaCount = criteriaCount;
    }

    public int getTotalCriteriaWeight() {
        return totalCriteriaWeight;
    }

    public void setTotalCriteriaWeight(int totalCriteriaWeight) {
        this.totalCriteriaWeight = totalCriteriaWeight;
    }

    public long getMandatoryCriteriaCount() {
        return mandatoryCriteriaCount;
    }

    public void setMandatoryCriteriaCount(long mandatoryCriteriaCount) {
        this.mandatoryCriteriaCount = mandatoryCriteriaCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<EligibilityCriteriaResponse> getCriteriaList() {
        return criteriaList;
    }

    public void setCriteriaList(List<EligibilityCriteriaResponse> criteriaList) {
        this.criteriaList = criteriaList;
    }
}
