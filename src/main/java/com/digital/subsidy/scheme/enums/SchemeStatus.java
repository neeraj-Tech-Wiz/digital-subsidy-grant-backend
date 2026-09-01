package com.digital.subsidy.scheme.enums;

/**
 * Represents the lifecycle status of a government subsidy or grant scheme.
 */
public enum SchemeStatus {
    DRAFT("Draft - Under Preparation"),
    ACTIVE("Active - Open for Applications"),
    INACTIVE("Inactive - Temporarily Paused"),
    SUSPENDED("Suspended - Under Review"),
    CLOSED("Closed - Scheme Concluded");

    private final String description;

    SchemeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
