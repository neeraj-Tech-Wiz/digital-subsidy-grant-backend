package com.infosys.subsidy.enums;

/**
 * Score Category & Routing Decision after evaluation.
 */
public enum ScoreCategory {
    HIGH_PRIORITY_DIRECT_APPROVAL("High Priority - Eligible for Direct Grant Approval", "Score >= 80% and all mandatory criteria satisfied. Recommended for fast-track DBT disbursement."),
    MEDIUM_PRIORITY_MANUAL_REVIEW("Medium Priority - Eligible with Verification", "Score between 50% and 79% and all mandatory criteria satisfied. Assigned for manual officer review."),
    LOW_PRIORITY_ADDITIONAL_INFO_REQUIRED("Low Priority - Additional Documentation Required", "Score between 30% and 49%. Beneficiary must upload additional supporting proofs."),
    INELIGIBLE("Ineligible - Application Rejected", "Score < 30% or failed one or more mandatory eligibility criteria.");

    private final String title;
    private final String description;

    ScoreCategory(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public static ScoreCategory fromScore(int totalScore, boolean allMandatorySatisfied) {
        if (!allMandatorySatisfied) {
            return INELIGIBLE;
        }
        if (totalScore >= 80) {
            return HIGH_PRIORITY_DIRECT_APPROVAL;
        } else if (totalScore >= 50) {
            return MEDIUM_PRIORITY_MANUAL_REVIEW;
        } else if (totalScore >= 30) {
            return LOW_PRIORITY_ADDITIONAL_INFO_REQUIRED;
        } else {
            return INELIGIBLE;
        }
    }
}
