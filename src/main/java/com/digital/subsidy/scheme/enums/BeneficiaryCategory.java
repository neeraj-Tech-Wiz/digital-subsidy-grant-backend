package com.digital.subsidy.scheme.enums;

/**
 * Beneficiary target categories for government schemes.
 */
public enum BeneficiaryCategory {
    FARMER("Farmer / Agricultural Worker"),
    STUDENT("Student / Scholar"),
    WOMEN_ENTREPRENEUR("Women Entrepreneur / Self-Help Group"),
    SMALL_BUSINESS("Small & Micro Enterprise (MSME)"),
    SENIOR_CITIZEN("Senior Citizen / Pensioner"),
    LOW_INCOME("Low Income / BPL"),
    GENERAL("General Public / Universal");

    private final String displayName;

    BeneficiaryCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
