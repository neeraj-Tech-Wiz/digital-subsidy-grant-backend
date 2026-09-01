package com.infosys.subsidy.dto;

import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.enums.CriterionType;
import com.infosys.subsidy.enums.Operator;

public class EligibilityCriteriaResponse {

    private Long id;
    private Long schemeId;
    private String criterionName;
    private CriterionType criterionType;
    private Operator operator;
    private String expectedValue;
    private int weight;
    private int maxScore;
    private boolean mandatory;
    private boolean active;
    private String description;

    public EligibilityCriteriaResponse() {
    }

    public static EligibilityCriteriaResponse fromEntity(EligibilityCriteria entity) {
        if (entity == null) return null;
        EligibilityCriteriaResponse resp = new EligibilityCriteriaResponse();
        resp.setId(entity.getId());
        resp.setSchemeId(entity.getScheme() != null ? entity.getScheme().getId() : null);
        resp.setCriterionName(entity.getCriterionName());
        resp.setCriterionType(entity.getCriterionType());
        resp.setOperator(entity.getOperator());
        resp.setExpectedValue(entity.getExpectedValue());
        resp.setWeight(entity.getWeight());
        resp.setMaxScore(entity.getMaxScore());
        resp.setMandatory(entity.isMandatory());
        resp.setActive(entity.isActive());
        resp.setDescription(entity.getDescription());
        return resp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(Long schemeId) {
        this.schemeId = schemeId;
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
}
