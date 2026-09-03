package com.infosys.subsidy.service;

import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.enums.CriterionType;
import com.infosys.subsidy.enums.Operator;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EligibilityService {

    // =====================================================
    // EVALUATE DYNAMIC ELIGIBILITY CRITERION
    // =====================================================

    public boolean evaluateCriterion(
            Map<String, String> eligibilityData,
            EligibilityCriteria criterion) {

        // Use fieldName as the actual dynamic key
        String fieldName = criterion.getFieldName();

        // Fallback to criterion name if fieldName is empty
        if (fieldName == null || fieldName.trim().isEmpty()) {

            fieldName = criterion.getCriterionName();
        }


        // =====================================================
        // GET ACTUAL VALUE
        // =====================================================

        String actualValue =
                eligibilityData.get(fieldName);


        // =====================================================
        // CASE-INSENSITIVE FALLBACK
        // =====================================================

        if (actualValue == null) {

            for (Map.Entry<String, String> entry
                    : eligibilityData.entrySet()) {

                if (entry.getKey()
                        .equalsIgnoreCase(fieldName)) {

                    actualValue = entry.getValue();
                    break;
                }
            }
        }


        // =====================================================
        // VALUE NOT PROVIDED
        // =====================================================

        if (actualValue == null
                || actualValue.trim().isEmpty()) {

            System.out.println(
                    "No value provided for field: "
                            + fieldName
            );

            return false;
        }


        String expectedValue =
                criterion.getExpectedValue();


        // =====================================================
        // NUMERIC
        // =====================================================

        if (criterion.getCriterionType()
                == CriterionType.NUMERIC) {

            try {

                double actual =
                        Double.parseDouble(
                                actualValue.trim()
                        );

                double expected =
                        Double.parseDouble(
                                expectedValue.trim()
                        );

                return compareNumbers(
                        actual,
                        expected,
                        criterion.getOperator()
                );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid numeric value for field: "
                                + fieldName
                );

                return false;
            }
        }


        // =====================================================
// BOOLEAN
// =====================================================

        if (criterion.getCriterionType()
                == CriterionType.BOOLEAN) {

            String actualText = actualValue.trim();
            String expectedText = expectedValue.trim();

            // Validate actual value
            if (!actualText.equalsIgnoreCase("true")
                    && !actualText.equalsIgnoreCase("false")) {

                System.out.println(
                        "Invalid boolean value for field: "
                                + fieldName
                );

                return false;
            }

            // Validate expected value
            if (!expectedText.equalsIgnoreCase("true")
                    && !expectedText.equalsIgnoreCase("false")) {

                System.out.println(
                        "Invalid expected boolean value for field: "
                                + fieldName
                );

                return false;
            }

            boolean actual =
                    Boolean.parseBoolean(actualText);

            boolean expected =
                    Boolean.parseBoolean(expectedText);

            return actual == expected;
        }

        // =====================================================
        // TEXT / ENUM
        // =====================================================

        if (criterion.getCriterionType()
                == CriterionType.TEXT
                || criterion.getCriterionType()
                == CriterionType.ENUM) {

            return compareText(
                    actualValue,
                    expectedValue,
                    criterion.getOperator()
            );
        }


        return false;
    }


    // =====================================================
    // NUMERIC COMPARISON
    // =====================================================

    private boolean compareNumbers(
            double actual,
            double expected,
            Operator operator) {

        return switch (operator) {

            case EQUAL ->
                    Double.compare(actual, expected) == 0;

            case LESS_THAN ->
                    actual < expected;

            case LESS_THAN_EQUAL ->
                    actual <= expected;

            case GREATER_THAN ->
                    actual > expected;

            case GREATER_THAN_EQUAL ->
                    actual >= expected;

            default ->
                    false;
        };
    }


    // =====================================================
    // TEXT / ENUM COMPARISON
    // =====================================================

    private boolean compareText(
            String actual,
            String expected,
            Operator operator) {

        if (actual == null || expected == null) {
            return false;
        }


        // =====================================================
        // EQUAL
        // =====================================================

        if (operator == Operator.EQUAL) {

            return actual.trim()
                    .equalsIgnoreCase(
                            expected.trim()
                    );
        }


        // =====================================================
        // IN
        //
        // Example:
        // FARMER, STUDENT, GENERAL
        // =====================================================

        if (operator == Operator.IN) {

            String[] values =
                    expected.split(",");

            for (String value : values) {

                if (actual.trim()
                        .equalsIgnoreCase(
                                value.trim())) {

                    return true;
                }
            }
        }

        return false;
    }
}