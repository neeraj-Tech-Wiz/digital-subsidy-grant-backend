package com.digital.subsidy.scheme.model;

import com.digital.subsidy.scheme.enums.BeneficiaryCategory;
import com.digital.subsidy.scheme.enums.SchemeStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Scheme Entity / Model.
 * Represents a Government Subsidy or Grant Scheme.
 * Manages a One-to-Many relationship with EligibilityCriteria.
 */
public class Scheme implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String schemeCode;
    private String schemeName;
    private String description;
    private double grantAmount;
    private double totalBudget;
    private double disbursedAmount;
    private SchemeStatus status;
    private String applicableRegion;
    private BeneficiaryCategory beneficiaryCategory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // One-to-Many Relationship with EligibilityCriteria
    private List<EligibilityCriteria> criteriaList = new ArrayList<>();

    public Scheme() {
        this.status = SchemeStatus.DRAFT;
        this.beneficiaryCategory = BeneficiaryCategory.GENERAL;
        this.applicableRegion = "All India";
        this.disbursedAmount = 0.0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Scheme(Long id, String schemeCode, String schemeName, String description,
                  double grantAmount, double totalBudget, SchemeStatus status,
                  String applicableRegion, BeneficiaryCategory beneficiaryCategory) {
        this();
        this.id = id;
        this.schemeCode = schemeCode;
        this.schemeName = schemeName;
        this.description = description;
        this.grantAmount = grantAmount;
        this.totalBudget = totalBudget;
        this.status = status;
        this.applicableRegion = applicableRegion;
        this.beneficiaryCategory = beneficiaryCategory;
    }

    // Relationship Helper Methods (Bidirectional management)
    public void addCriterion(EligibilityCriteria criterion) {
        if (criterion != null) {
            criterion.setScheme(this);
            // If criterion has no id, it might be added freshly
            this.criteriaList.add(criterion);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public boolean removeCriterion(Long criterionId) {
        boolean removed = this.criteriaList.removeIf(c -> {
            if (Objects.equals(c.getId(), criterionId)) {
                c.setScheme(null);
                return true;
            }
            return false;
        });
        if (removed) {
            this.updatedAt = LocalDateTime.now();
        }
        return removed;
    }

    public void clearCriteria() {
        for (EligibilityCriteria c : this.criteriaList) {
            c.setScheme(null);
        }
        this.criteriaList.clear();
        this.updatedAt = LocalDateTime.now();
    }

    public double getRemainingBudget() {
        return Math.max(0.0, this.totalBudget - this.disbursedAmount);
    }

    public int getTotalCriteriaWeight() {
        return criteriaList.stream()
                .filter(EligibilityCriteria::isActive)
                .mapToInt(EligibilityCriteria::getWeight)
                .sum();
    }

    public long getMandatoryCriteriaCount() {
        return criteriaList.stream()
                .filter(c -> c.isActive() && c.isMandatory())
                .count();
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

    public double getGrantAmount() {
        return grantAmount;
    }

    public void setGrantAmount(double grantAmount) {
        this.grantAmount = grantAmount;
    }

    public double getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(double totalBudget) {
        this.totalBudget = totalBudget;
    }

    public double getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(double disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
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

    public List<EligibilityCriteria> getCriteriaList() {
        return criteriaList;
    }

    public void setCriteriaList(List<EligibilityCriteria> criteriaList) {
        this.criteriaList = criteriaList != null ? criteriaList : new ArrayList<>();
        for (EligibilityCriteria c : this.criteriaList) {
            c.setScheme(this);
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Scheme scheme = (Scheme) o;
        return Objects.equals(id, scheme.id) || Objects.equals(schemeCode, scheme.schemeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, schemeCode);
    }

    @Override
    public String toString() {
        return "Scheme{" +
                "id=" + id +
                ", code='" + schemeCode + '\'' +
                ", name='" + schemeName + '\'' +
                ", grant=" + grantAmount +
                ", budget=" + totalBudget +
                ", status=" + status +
                ", category=" + beneficiaryCategory +
                ", criteriaCount=" + criteriaList.size() +
                '}';
    }
}
