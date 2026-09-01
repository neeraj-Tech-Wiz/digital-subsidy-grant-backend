package com.infosys.subsidy.dto;

public class CriterionScore {

    private String criterionName;
    private Integer score;
    private Integer maxScore;
    private boolean satisfied;

    public CriterionScore() {
    }

    public CriterionScore(
            String criterionName,
            Integer score,
            Integer maxScore,
            boolean satisfied) {

        this.criterionName = criterionName;
        this.score = score;
        this.maxScore = maxScore;
        this.satisfied = satisfied;
    }

    public String getCriterionName() {
        return criterionName;
    }

    public void setCriterionName(String criterionName) {
        this.criterionName = criterionName;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public void setSatisfied(boolean satisfied) {
        this.satisfied = satisfied;
    }
}
