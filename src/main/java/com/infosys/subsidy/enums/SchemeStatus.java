package com.infosys.subsidy.enums;

/**
 * Lifecycle status for Government Schemes.
 */
public enum SchemeStatus {
    DRAFT("Draft - Under Configuration"),
    ACTIVE("Active - Open for Applications"),
    INACTIVE("Inactive - Temporarily Suspended"),
    CLOSED("Closed - Budget Exhausted or Expired");

    private final String description;

    SchemeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
