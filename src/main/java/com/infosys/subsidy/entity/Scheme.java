package com.infosys.subsidy.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.infosys.subsidy.enums.BeneficiaryCategory;
import com.infosys.subsidy.enums.SchemeStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scheme Entity.
 * Represents a Government Subsidy or Grant Scheme in the master data.
 * Manages a One-to-Many relationship with EligibilityCriteria.
 */
@Entity
@Table(name = "schemes")
public class Scheme implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Scheme code is required")
    @Column(name = "scheme_code", nullable = false, unique = true, length = 50)
    private String schemeCode;

    @NotBlank(message = "Scheme name is required")
    @Column(name = "scheme_name", nullable = false, unique = true, length = 200)
    private String schemeName;

    @Column(name = "description", length = 2000)
    private String description;

    @Positive(message = "Grant amount must be greater than zero")
    @Column(name = "grant_amount", nullable = false)
    private double grantAmount;

    @Positive(message = "Total budget must be greater than zero")
    @Column(name = "total_budget", nullable = false)
    private double totalBudget;

    @Column(name = "disbursed_amount", nullable = false)
    private double disbursedAmount = 0.0;

    @NotNull(message = "Scheme status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SchemeStatus status = SchemeStatus.DRAFT;

    @Column(name = "applicable_region", nullable = false, length = 100)
    private String applicableRegion = "All India";

    @NotNull(message = "Beneficiary category is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "beneficiary_category", nullable = false, length = 50)
    private BeneficiaryCategory beneficiaryCategory = BeneficiaryCategory.GENERAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // One-to-Many Relationship with EligibilityCriteria
    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<EligibilityCriteria> criteriaList = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Scheme() {
        this.status = SchemeStatus.DRAFT;
        this.beneficiaryCategory = BeneficiaryCategory.GENERAL;
        this.applicableRegion = "All India";
        this.disbursedAmount = 0.0;
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
        this.status = status != null ? status : SchemeStatus.DRAFT;
        this.applicableRegion = (applicableRegion != null && !applicableRegion.trim().isEmpty()) ? applicableRegion : "All India";
        this.beneficiaryCategory = beneficiaryCategory != null ? beneficiaryCategory : BeneficiaryCategory.GENERAL;
    }

    // Relationship Helper Methods
    public void addCriterion(EligibilityCriteria criterion) {
        if (criterion != null) {
            criterion.setScheme(this);
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

    public List<EligibilityCriteria> getCriteriaList() {
        return criteriaList;
    }

    public void setCriteriaList(List<EligibilityCriteria> criteriaList) {
        this.criteriaList = criteriaList != null ? criteriaList : new ArrayList<>();
        for (EligibilityCriteria c : this.criteriaList) {
            c.setScheme(this);
        }
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
                ", criteriaCount=" + (criteriaList != null ? criteriaList.size() : 0) +
                '}';
    }
}
