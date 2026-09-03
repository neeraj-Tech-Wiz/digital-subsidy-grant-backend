package com.infosys.subsidy.dto;

import com.infosys.subsidy.enums.CriterionType;
import com.infosys.subsidy.enums.Operator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EligibilityCriteriaRequest {

    @NotBlank(message = "Criterion name is required")
    private String criterionName;

    @NotBlank(message = "Field name is required")
    private String fieldName;

    @NotNull(message = "Criterion type is required")
    private CriterionType criterionType;

    @NotNull(message = "Operator is required")
    private Operator operator;

    @NotBlank(message = "Expected threshold / target value is required")
    private String expectedValue;

    @Min(value = 1, message = "Weight must be at least 1")
    @Max(value = 100, message = "Weight cannot exceed 100")
    private int weight = 10;

    private boolean mandatory = false;

    private boolean active = true;

    private String description;

    public EligibilityCriteriaRequest() {
    }

    public EligibilityCriteriaRequest(
            String criterionName,
            String fieldName,
            CriterionType criterionType,
            Operator operator,
            String expectedValue,
            int weight,
            boolean mandatory,
            boolean active,
            String description) {

        this.criterionName = criterionName;
        this.fieldName = fieldName;
        this.criterionType = criterionType;
        this.operator = operator;
        this.expectedValue = expectedValue;
        this.weight = weight;
        this.mandatory = mandatory;
        this.active = active;
        this.description = description;
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
}