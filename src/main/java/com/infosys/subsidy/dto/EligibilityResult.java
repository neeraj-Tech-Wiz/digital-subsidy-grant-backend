package com.infosys.subsidy.dto;

import java.util.List;

public class EligibilityResult {

    private Long applicationId;
    private Integer totalScore;
    private String routingDecision;
    private boolean mandatoryCriteriaFailed;
    private List<CriterionScore> criterionScores;

    public EligibilityResult() {
    }

    public EligibilityResult(
            Long applicationId,
            Integer totalScore,
            String routingDecision,
            boolean mandatoryCriteriaFailed,
            List<CriterionScore> criterionScores) {

        this.applicationId = applicationId;
        this.totalScore = totalScore;
        this.routingDecision = routingDecision;
        this.mandatoryCriteriaFailed = mandatoryCriteriaFailed;
        this.criterionScores = criterionScores;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getRoutingDecision() {
        return routingDecision;
    }

    public void setRoutingDecision(String routingDecision) {
        this.routingDecision = routingDecision;
    }

    public boolean isMandatoryCriteriaFailed() {
        return mandatoryCriteriaFailed;
    }

    public void setMandatoryCriteriaFailed(boolean mandatoryCriteriaFailed) {
        this.mandatoryCriteriaFailed = mandatoryCriteriaFailed;
    }

    public List<CriterionScore> getCriterionScores() {
        return criterionScores;
    }

    public void setCriterionScores(List<CriterionScore> criterionScores) {
        this.criterionScores = criterionScores;
    }
}
