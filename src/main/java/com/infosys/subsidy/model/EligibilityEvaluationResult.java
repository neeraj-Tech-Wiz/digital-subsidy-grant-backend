package com.infosys.subsidy.model;

import com.infosys.subsidy.enums.ScoreCategory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Eligibility Evaluation & Scoring Report.
 * Contains transparent criterion breakdown, aggregate score, and automated workflow routing decision.
 */
public class EligibilityEvaluationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long schemeId;
    private String schemeCode;
    private String schemeName;
    private String beneficiaryName;
    private String aadhaarNumber;
    private double requestedGrantAmount;
    private int totalScore;
    private int maxPossibleScore;
    private boolean allMandatorySatisfied;
    private ScoreCategory scoreCategory;
    private LocalDateTime evaluatedAt;

    private List<CriterionEvaluationResult> criteriaResults = new ArrayList<>();

    public EligibilityEvaluationResult() {
        this.evaluatedAt = LocalDateTime.now();
    }

    public void addCriterionResult(CriterionEvaluationResult result) {
        this.criteriaResults.add(result);
    }

    public double getScorePercentage() {
        if (maxPossibleScore == 0) return 0.0;
        return ((double) totalScore / maxPossibleScore) * 100.0;
    }

    public boolean isEligible() {
        return allMandatorySatisfied && totalScore >= 50;
    }

    // Getters and Setters
    public Long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(Long schemeId) {
        this.schemeId = schemeId;
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

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    public double getRequestedGrantAmount() {
        return requestedGrantAmount;
    }

    public void setRequestedGrantAmount(double requestedGrantAmount) {
        this.requestedGrantAmount = requestedGrantAmount;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getMaxPossibleScore() {
        return maxPossibleScore;
    }

    public void setMaxPossibleScore(int maxPossibleScore) {
        this.maxPossibleScore = maxPossibleScore;
    }

    public boolean isAllMandatorySatisfied() {
        return allMandatorySatisfied;
    }

    public void setAllMandatorySatisfied(boolean allMandatorySatisfied) {
        this.allMandatorySatisfied = allMandatorySatisfied;
    }

    public ScoreCategory getScoreCategory() {
        return scoreCategory;
    }

    public void setScoreCategory(ScoreCategory scoreCategory) {
        this.scoreCategory = scoreCategory;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public List<CriterionEvaluationResult> getCriteriaResults() {
        return criteriaResults;
    }

    public void setCriteriaResults(List<CriterionEvaluationResult> criteriaResults) {
        this.criteriaResults = criteriaResults;
    }
}
