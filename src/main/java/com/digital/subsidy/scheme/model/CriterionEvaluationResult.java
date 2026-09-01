package com.digital.subsidy.scheme.model;

import java.io.Serializable;

/**
 * Result of evaluating a single EligibilityCriterion against a Beneficiary.
 */
public class CriterionEvaluationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private EligibilityCriteria criterion;
    private String applicantValue;
    private boolean satisfied;
    private int pointsAwarded;
    private int maxPoints;
    private String statusMessage;

    public CriterionEvaluationResult() {}

    public CriterionEvaluationResult(EligibilityCriteria criterion, String applicantValue,
                                     boolean satisfied, int pointsAwarded, int maxPoints,
                                     String statusMessage) {
        this.criterion = criterion;
        this.applicantValue = applicantValue;
        this.satisfied = satisfied;
        this.pointsAwarded = pointsAwarded;
        this.maxPoints = maxPoints;
        this.statusMessage = statusMessage;
    }

    public EligibilityCriteria getCriterion() {
        return criterion;
    }

    public void setCriterion(EligibilityCriteria criterion) {
        this.criterion = criterion;
    }

    public String getApplicantValue() {
        return applicantValue;
    }

    public void setApplicantValue(String applicantValue) {
        this.applicantValue = applicantValue;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public void setSatisfied(boolean satisfied) {
        this.satisfied = satisfied;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(int pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
