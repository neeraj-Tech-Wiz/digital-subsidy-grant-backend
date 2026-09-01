package com.digital.subsidy.scheme.enums;

/**
 * Comparison operators for evaluating eligibility criteria against beneficiary data.
 */
public enum Operator {
    LESS_THAN("<", "Strictly Less Than"),
    LESS_THAN_EQUAL("<=", "Less Than or Equal To"),
    EQUAL("==", "Equals"),
    GREATER_THAN(">", "Strictly Greater Than"),
    GREATER_THAN_EQUAL(">=", "Greater Than or Equal To"),
    IN("IN", "Contained In List (Comma-Separated)");

    private final String symbol;
    private final String description;

    Operator(String symbol, String description) {
        this.symbol = symbol;
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }
}
