package com.infosys.subsidy.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.infosys.subsidy.enums.CriterionType;
import com.infosys.subsidy.enums.Operator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * EligibilityCriteria Entity.
 * Represents a configurable rule or condition that applicants must meet to qualify for a Scheme.
 * Has a Many-to-One relationship with Scheme.
 */
@Entity
@Table(name = "eligibility_criteria")
public class EligibilityCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    @JsonBackReference
    private Scheme scheme;

    @NotBlank(message = "Criterion name is required")
    @Column(name = "criterion_name", nullable = false)
    private String criterionName;

    @Column(name = "field_name")
    private String fieldName;

    @NotNull(message = "Criterion type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "criterion_type", nullable = false, length = 30)
    private CriterionType criterionType;

    @NotNull(message = "Operator is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 30)
    private Operator operator;

    @NotBlank(message = "Expected threshold / target value is required")
    @Column(name = "expected_value", nullable = false)
    private String expectedValue;

    @Min(value = 1, message = "Weight must be at least 1")
    @Max(value = 100, message = "Weight cannot exceed 100")
    @Column(name = "weight", nullable = false)
    private int weight = 10;

    @Column(name = "max_score", nullable = false)
    private int maxScore = 10;

    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 1000)
    private String description;

    public EligibilityCriteria() {
    }

    public EligibilityCriteria(Long id, String criterionName, CriterionType criterionType,
                               Operator operator, String expectedValue, int weight,
                               int maxScore, boolean mandatory, boolean active, String description) {
        this.id = id;
        this.criterionName = criterionName;
        this.criterionType = criterionType;
        this.operator = operator;
        this.expectedValue = expectedValue;
        this.weight = weight;
        this.maxScore = maxScore;
        this.mandatory = mandatory;
        this.active = active;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Scheme getScheme() {
        return scheme;
    }

    public void setScheme(Scheme scheme) {
        this.scheme = scheme;
    }

    public String getCriterionName() {
        return criterionName;
    }

    public void setCriterionName(String criterionName) {
        this.criterionName = criterionName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public CriterionType getCriterionType() {
        return criterionType;
    }

    public void setCriterionType(CriterionType criterionType) {
        this.criterionType = criterionType;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EligibilityCriteria that = (EligibilityCriteria) o;
        return Objects.equals(id, that.id) ||
                (Objects.equals(criterionName, that.criterionName) &&
                 Objects.equals(scheme != null ? scheme.getId() : null, that.scheme != null ? that.scheme.getId() : null));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, criterionName);
    }

    @Override
    public String toString() {
        return "EligibilityCriteria{" +
                "id=" + id +
                ", criterionName='" + criterionName + '\'' +
                ", criterionType=" + criterionType +
                ", operator=" + operator +
                ", expectedValue='" + expectedValue + '\'' +
                ", weight=" + weight +
                ", mandatory=" + mandatory +
                ", active=" + active +
                '}';
    }
}
