package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.BeneficiaryCategory;
import com.infosys.subsidy.enums.SchemeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SchemeRequest {

    @NotBlank(message = "Scheme code is required")
    private String schemeCode;

    @NotBlank(message = "Scheme name is required")
    private String schemeName;

    private String description;

    @NotNull(message = "Grant amount is required")
    @Positive(message = "Grant amount must be greater than zero")
    private BigDecimal grantAmount;

    @NotNull(message = "Total budget is required")
    @Positive(message = "Total budget must be greater than zero")
    private BigDecimal totalBudget;

    private SchemeStatus status = SchemeStatus.DRAFT;

    private String applicableRegion = "All India";

    private BeneficiaryCategory beneficiaryCategory = BeneficiaryCategory.GENERAL;

    private List<EligibilityCriteriaRequest> criteriaList = new ArrayList<>();

    public SchemeRequest() {
    }

    public SchemeRequest(String schemeCode, String schemeName, String description,
                         BigDecimal grantAmount, BigDecimal totalBudget, SchemeStatus status,
                         String applicableRegion, BeneficiaryCategory beneficiaryCategory) {
        this.schemeCode = schemeCode;
        this.schemeName = schemeName;
        this.description = description;
        this.grantAmount = grantAmount;
        this.totalBudget = totalBudget;
        this.status = status;
        this.applicableRegion = applicableRegion;
        this.beneficiaryCategory = beneficiaryCategory;
    }

    // Getters and Setters
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

    public List<EligibilityCriteriaRequest> getCriteriaList() {
        return criteriaList;
    }

    public void setCriteriaList(List<EligibilityCriteriaRequest> criteriaList) {
        this.criteriaList = criteriaList;
    }
}
