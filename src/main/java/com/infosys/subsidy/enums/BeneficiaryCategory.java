package com.infosys.subsidy.enums;

/**
 * Categorization for targeted beneficiary demographics.
 */
public enum BeneficiaryCategory {
    FARMER("Farmer & Agricultural Worker"),
    STUDENT("Student & Youth Scholar"),
    WOMEN_ENTREPRENEUR("Women Entrepreneur & Self Help Group"),
    SENIOR_CITIZEN("Senior Citizen & Pensioner"),
    BPL_FAMILY("Below Poverty Line (BPL) Family"),
    ARTISAN("Artisan & Traditional Craftsman"),
    GENERAL("General Citizen");

    private final String displayName;

    BeneficiaryCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
