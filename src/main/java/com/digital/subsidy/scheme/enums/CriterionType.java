package com.digital.subsidy.scheme.enums;

/**
 * Data type of an eligibility criterion value.
 */
public enum CriterionType {
    NUMERIC("Numeric Comparison (Income, Age, Land Size, etc.)"),
    BOOLEAN("Boolean Flag (Yes/No, True/False)"),
    TEXT("Exact Text Match (State, District, etc.)"),
    ENUM("Category / List Match (Farmer, Student, Caste, etc.)");

    private final String description;

    CriterionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
