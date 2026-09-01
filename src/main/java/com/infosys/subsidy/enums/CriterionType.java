package com.infosys.subsidy.enums;

/**
 * Supported data types for scheme eligibility criteria.
 */
public enum CriterionType {
    NUMERIC("Numeric Comparison (Income, Age, Land Area)"),
    BOOLEAN("Boolean Flag (Yes/No, Verification Status)"),
    TEXT("Exact Text Match (Region, State, Occupation)"),
    ENUM("Enumerated Value / Multi-choice");

    private final String description;

    CriterionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
