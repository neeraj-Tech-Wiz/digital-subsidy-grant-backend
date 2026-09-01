package com.digital.subsidy.scheme.service;

import com.digital.subsidy.scheme.enums.CriterionType;
import com.digital.subsidy.scheme.enums.Operator;
import com.digital.subsidy.scheme.enums.ScoreCategory;
import com.digital.subsidy.scheme.model.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Service responsible for evaluating beneficiary applications against scheme-specific
 * eligibility criteria, scoring points transparently, and determining workflow routing.
 */
public class EligibilityEvaluationService {

    /**
     * Evaluates a beneficiary profile against all active eligibility criteria of a scheme.
     */
    public EligibilityEvaluationResult evaluate(Scheme scheme, BeneficiaryProfile beneficiary) {
        if (scheme == null) {
            throw new IllegalArgumentException("Scheme cannot be null");
        }
        if (beneficiary == null) {
            throw new IllegalArgumentException("Beneficiary cannot be null");
        }

        EligibilityEvaluationResult report = new EligibilityEvaluationResult();
        report.setSchemeId(scheme.getId());
        report.setSchemeCode(scheme.getSchemeCode());
        report.setSchemeName(scheme.getSchemeName());
        report.setBeneficiaryName(beneficiary.getName());
        report.setAadhaarNumber(beneficiary.getAadhaarNumber());
        report.setRequestedGrantAmount(scheme.getGrantAmount());
        report.setEvaluatedAt(LocalDateTime.now());

        int totalScore = 0;
        int maxPossible = 0;
        boolean allMandatorySatisfied = true;

        List<EligibilityCriteria> activeCriteria = scheme.getCriteriaList().stream()
                .filter(EligibilityCriteria::isActive)
                .toList();

        for (EligibilityCriteria criterion : activeCriteria) {
            maxPossible += criterion.getWeight();
            CriterionEvaluationResult result = evaluateSingleCriterion(criterion, beneficiary);
            report.addCriterionResult(result);

            if (result.isSatisfied()) {
                totalScore += result.getPointsAwarded();
            } else {
                if (criterion.isMandatory()) {
                    allMandatorySatisfied = false;
                }
            }
        }

        report.setTotalScore(totalScore);
        report.setMaxPossibleScore(maxPossible);
        report.setAllMandatorySatisfied(allMandatorySatisfied);
        report.setScoreCategory(ScoreCategory.fromScore(totalScore, allMandatorySatisfied));

        return report;
    }

    /**
     * Evaluates an individual criterion rule against beneficiary attributes.
     */
    public CriterionEvaluationResult evaluateSingleCriterion(EligibilityCriteria criterion, BeneficiaryProfile profile) {
        String name = criterion.getCriterionName().toLowerCase();
        CriterionType type = criterion.getCriterionType();
        Operator op = criterion.getOperator();
        String expected = criterion.getExpectedValue().trim();

        Object actualValueObj = resolveApplicantValue(name, profile);
        String applicantValStr = actualValueObj != null ? actualValueObj.toString() : "Not Provided";

        boolean satisfied = false;
        String reason;

        try {
            if (actualValueObj == null) {
                satisfied = false;
                reason = "Applicant attribute not found / missing";
            } else {
                switch (type) {
                    case NUMERIC:
                        double actNum = parseNumeric(actualValueObj);
                        double expNum = Double.parseDouble(expected);
                        satisfied = evaluateNumeric(actNum, expNum, op);
                        reason = satisfied ? "Satisfied (" + actNum + " " + op.getSymbol() + " " + expNum + ")"
                                           : "Failed (" + actNum + " not " + op.getSymbol() + " " + expNum + ")";
                        break;

                    case BOOLEAN:
                        boolean actBool = parseBoolean(actualValueObj);
                        boolean expBool = Boolean.parseBoolean(expected) || expected.equalsIgnoreCase("yes");
                        satisfied = (actBool == expBool);
                        reason = satisfied ? "Satisfied (Matched: " + expBool + ")"
                                           : "Failed (Expected: " + expBool + ", Actual: " + actBool + ")";
                        break;

                    case ENUM:
                    case TEXT:
                        satisfied = evaluateText(actualValueStr(actualValueObj), expected, op);
                        reason = satisfied ? "Satisfied (Matched: " + expected + ")"
                                           : "Failed (Expected: " + expected + ", Actual: " + applicantValStr + ")";
                        break;

                    default:
                        satisfied = false;
                        reason = "Unknown criterion type";
                }
            }
        } catch (Exception e) {
            satisfied = false;
            reason = "Evaluation error: " + e.getMessage();
        }

        int points = satisfied ? criterion.getWeight() : 0;
        return new CriterionEvaluationResult(criterion, applicantValStr, satisfied, points, criterion.getWeight(), reason);
    }

    private Object resolveApplicantValue(String criterionName, BeneficiaryProfile profile) {
        // Document & verification checks
        if (criterionName.contains("document") || criterionName.contains("doc") || criterionName.contains("proof") || criterionName.contains("certificate")) {
            return profile.isDocumentsVerified();
        } else if (criterionName.contains("income") || criterionName.contains("salary")) {
            return profile.getAnnualIncome();
        } else if (criterionName.contains("age") || criterionName.contains("dob")) {
            return profile.getAge();
        } else if (criterionName.contains("land") || criterionName.contains("asset") || criterionName.contains("acre") || criterionName.contains("holding")) {
            return profile.getLandHoldingAcres();
        } else if (criterionName.contains("category") || criterionName.contains("occupation") || criterionName.contains("beneficiary")) {
            return profile.getCategory() != null ? profile.getCategory().name() : null;
        } else if (criterionName.contains("region") || criterionName.contains("state") || criterionName.contains("district") || criterionName.contains("location")) {
            return profile.getRegion();
        } else if (criterionName.contains("aadhaar") || criterionName.contains("identity") || criterionName.contains("kyc")) {
            return profile.isAadhaarVerified();
        } else if (criterionName.contains("mobile") || criterionName.contains("phone")) {
            return profile.isMobileVerified();
        } else if (criterionName.contains("previous") || criterionName.contains("prior") || criterionName.contains("benefit")) {
            return profile.isPreviousBenefitsReceived();
        }

        // Check dynamic custom attributes map
        if (profile.getAttributes() != null) {
            for (String key : profile.getAttributes().keySet()) {
                if (criterionName.contains(key.toLowerCase()) || key.toLowerCase().contains(criterionName)) {
                    return profile.getAttribute(key);
                }
            }
        }

        return null;
    }

    private boolean evaluateNumeric(double actual, double expected, Operator op) {
        return switch (op) {
            case LESS_THAN -> actual < expected;
            case LESS_THAN_EQUAL -> actual <= expected;
            case EQUAL -> Math.abs(actual - expected) < 0.0001;
            case GREATER_THAN -> actual > expected;
            case GREATER_THAN_EQUAL -> actual >= expected;
            case IN -> Math.abs(actual - expected) < 0.0001;
        };
    }

    private boolean evaluateText(String actual, String expected, Operator op) {
        if (actual == null) return false;
        String act = actual.trim();
        String exp = expected.trim();

        if (op == Operator.IN) {
            String[] tokens = exp.split(",");
            return Arrays.stream(tokens)
                    .map(String::trim)
                    .anyMatch(t -> t.equalsIgnoreCase(act));
        }

        return act.equalsIgnoreCase(exp);
    }

    private double parseNumeric(Object val) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return Double.parseDouble(val.toString().trim());
    }

    private boolean parseBoolean(Object val) {
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        String s = val.toString().trim().toLowerCase();
        return s.equals("true") || s.equals("yes") || s.equals("1") || s.equals("y");
    }

    private String actualValueStr(Object val) {
        return val != null ? val.toString().trim() : "";
    }
}
