package com.infosys.subsidy.dto;

public class EligibilityScoreDetailsDTO {

    private String criterionName;
    private String applicantValue;
    private String requirement;
    private String result;
    private Integer maxPoints;
    private Integer points;
    private boolean optional;

    public EligibilityScoreDetailsDTO() {}

    public EligibilityScoreDetailsDTO(String criterionName, String applicantValue, String requirement, String result, Integer maxPoints, Integer points, boolean optional) {
        this.criterionName = criterionName;
        this.applicantValue = applicantValue;
        this.requirement = requirement;
        this.result = result;
        this.maxPoints = maxPoints;
        this.points = points;
        this.optional = optional;
    }

    public String getCriterionName() {
        return criterionName;
    }

    public void setCriterionName(String criterionName) {
        this.criterionName = criterionName;
    }

    public String getApplicantValue() {
        return applicantValue;
    }

    public void setApplicantValue(String applicantValue) {
        this.applicantValue = applicantValue;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(Integer maxPoints) {
        this.maxPoints = maxPoints;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }
}
