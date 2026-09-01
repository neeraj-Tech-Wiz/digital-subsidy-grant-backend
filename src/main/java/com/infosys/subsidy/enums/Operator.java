package com.infosys.subsidy.enums;

/**
 * Comparison operators for evaluating applicant attributes against criteria.
 */
public enum Operator {
    EQUAL("==", "Equals"),
    LESS_THAN("<", "Less Than"),
    LESS_THAN_EQUAL("<=", "Less Than or Equal To"),
    GREATER_THAN(">", "Greater Than"),
    GREATER_THAN_EQUAL(">=", "Greater Than or Equal To"),
    IN("IN", "Matches Any In List (Comma-separated)");

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
