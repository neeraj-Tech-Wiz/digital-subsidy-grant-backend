package com.digital.subsidy.scheme.enums;

/**
 * Routing categories assigned based on the calculated eligibility score.
 */
public enum ScoreCategory {
    FAST_TRACK("Fast Track Approval", 80, 100, "Eligible for expedited processing and verification"),
    NORMAL_VERIFICATION("Normal Verification", 50, 79, "Requires standard multi-level verification workflow"),
    ADDITIONAL_SCRUTINY("Additional Scrutiny", 0, 49, "Score below threshold; requires field inspection and detailed review"),
    INELIGIBLE("Ineligible", 0, 0, "Failed one or more mandatory criteria. Application cannot proceed.");

    private final String title;
    private final int minScore;
    private final int maxScore;
    private final String description;

    ScoreCategory(String title, int minScore, int maxScore, String description) {
        this.title = title;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public String getDescription() {
        return description;
    }

    public static ScoreCategory fromScore(int score, boolean mandatorySatisfied) {
        if (!mandatorySatisfied) {
            return INELIGIBLE;
        }
        if (score >= 80) {
            return FAST_TRACK;
        } else if (score >= 50) {
            return NORMAL_VERIFICATION;
        } else {
            return ADDITIONAL_SCRUTINY;
        }
    }
}
