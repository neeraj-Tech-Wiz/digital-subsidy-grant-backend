package com.digital.subsidy.scheme.model;

import com.digital.subsidy.scheme.enums.CriterionType;
import com.digital.subsidy.scheme.enums.Operator;

import java.io.Serializable;
import java.util.Objects;

/**
 * EligibilityCriteria Entity / Model.
 * Represents a configurable condition that applicants must meet to qualify for a Scheme.
 * Has a Many-to-One relationship with Scheme.
 */
public class EligibilityCriteria implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Scheme scheme;
    private String criterionName;
    private CriterionType criterionType;
    private Operator operator;
    private String expectedValue;
    private int weight;       // Weight out of 100
    private int maxScore;     // Maximum score for this criterion
    private boolean mandatory;
    private boolean active;
    private String description;

    public EligibilityCriteria() {
        this.active = true;
        this.mandatory = false;
        this.weight = 10;
        this.maxScore = 10;
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
                ", name='" + criterionName + '\'' +
                ", type=" + criterionType +
                ", op=" + (operator != null ? operator.getSymbol() : "N/A") +
                ", expected='" + expectedValue + '\'' +
                ", weight=" + weight +
                ", mandatory=" + mandatory +
                ", active=" + active +
                '}';
    }
}
