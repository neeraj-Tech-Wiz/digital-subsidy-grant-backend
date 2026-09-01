package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.CriterionScore;
import com.infosys.subsidy.dto.EligibilityResult;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.ApplicationStatus;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.enums.CriterionType;
import com.infosys.subsidy.enums.Operator;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import com.infosys.subsidy.repository.EligibilityCriteriaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EligibilityScoringService {

    private final ApplicationRepository applicationRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final EligibilityCriteriaRepository criteriaRepository;

    public EligibilityScoringService(
            ApplicationRepository applicationRepository,
            BeneficiaryRepository beneficiaryRepository,
            EligibilityCriteriaRepository criteriaRepository) {

        this.applicationRepository = applicationRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.criteriaRepository = criteriaRepository;
    }

    public EligibilityResult calculateScore(Long applicationId) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() ->
                        new RuntimeException("Application not found"));

        Beneficiary beneficiary = beneficiaryRepository
                .findById(application.getBeneficiaryId())
                .orElseThrow(() ->
                        new RuntimeException("Beneficiary not found"));

        List<EligibilityCriteria> criteriaList =
                criteriaRepository.findBySchemeIdAndActiveTrue(
                        application.getSchemeId());

        if (criteriaList.isEmpty()) {
            throw new RuntimeException(
                    "No active eligibility criteria found for this scheme");
        }

        int totalScore = 0;
        boolean mandatoryCriteriaFailed = false;

        List<CriterionScore> scoreBreakdown = new ArrayList<>();

        for (EligibilityCriteria criteria : criteriaList) {

            Object beneficiaryValue =
                    getBeneficiaryValue(
                            beneficiary,
                            criteria.getCriterionName()
                    );

            boolean satisfied =
                    evaluateCriterion(
                            beneficiaryValue,
                            criteria.getExpectedValue(),
                            criteria.getCriterionType(),
                            criteria.getOperator()
                    );

            int score = satisfied
                    ? criteria.getMaxScore()
                    : 0;

            totalScore += score;

            if (criteria.isMandatory() && !satisfied) {
                mandatoryCriteriaFailed = true;
            }

            scoreBreakdown.add(
                    new CriterionScore(
                            criteria.getCriterionName(),
                            score,
                            criteria.getMaxScore(),
                            satisfied
                    )
            );
        }

        String routingDecision;

        if (mandatoryCriteriaFailed) {
            routingDecision = "INELIGIBLE";
        } else if (totalScore >= 80) {
            routingDecision = "FAST_TRACK";
        } else if (totalScore >= 50) {
            routingDecision = "NORMAL_VERIFICATION";
        } else {
            routingDecision = "ADDITIONAL_SCRUTINY";
        }

        application.setEligibilityScore(totalScore);
        application.setStatus(ApplicationStatus.SCORING_COMPLETED);

        applicationRepository.save(application);

        return new EligibilityResult(
                applicationId,
                totalScore,
                routingDecision,
                mandatoryCriteriaFailed,
                scoreBreakdown
        );
    }

    /**
     * Gets the beneficiary value corresponding to a configured criterion.
     */
    private Object getBeneficiaryValue(
            Beneficiary beneficiary,
            String criterionName) {

        String name = criterionName.trim().toLowerCase();

        return switch (name) {

            case "name" ->
                    beneficiary.getName();

            case "age" ->
                    beneficiary.getAge();

            case "aadhaar", "aadhaar_number" ->
                    beneficiary.getAadhaarNumber();

            case "mobile", "mobile_number" ->
                    beneficiary.getMobileNumber();

            case "email" ->
                    beneficiary.getEmail();

            case "government_scheme", "scheme" ->
                    beneficiary.getGovernmentScheme();

            default ->
                    throw new RuntimeException(
                            "Unsupported eligibility criterion: "
                                    + criterionName);
        };
    }

    /**
     * Evaluates a beneficiary value against a configured criterion.
     */
    private boolean evaluateCriterion(
            Object beneficiaryValue,
            String expectedValue,
            CriterionType criterionType,
            Operator operator) {

        if (beneficiaryValue == null) {
            return false;
        }

        return switch (criterionType) {

            case NUMERIC ->
                    evaluateNumeric(
                            beneficiaryValue,
                            expectedValue,
                            operator
                    );

            case BOOLEAN ->
                    evaluateBoolean(
                            beneficiaryValue,
                            expectedValue,
                            operator
                    );

            case TEXT, ENUM ->
                    evaluateText(
                            beneficiaryValue,
                            expectedValue,
                            operator
                    );
        };
    }

    /**
     * Evaluates numeric criteria such as age.
     */
    private boolean evaluateNumeric(
            Object actualValue,
            String expectedValue,
            Operator operator) {

        try {
            double actual =
                    Double.parseDouble(actualValue.toString());

            double expected =
                    Double.parseDouble(expectedValue.trim());

            return switch (operator) {

                case LESS_THAN ->
                        actual < expected;

                case LESS_THAN_EQUAL ->
                        actual <= expected;

                case EQUAL ->
                        actual == expected;

                case GREATER_THAN ->
                        actual > expected;

                case GREATER_THAN_EQUAL ->
                        actual >= expected;

                case IN ->
                        false;
            };

        } catch (NumberFormatException exception) {

            return false;
        }
    }

    /**
     * Evaluates boolean criteria.
     */
    private boolean evaluateBoolean(
            Object actualValue,
            String expectedValue,
            Operator operator) {

        boolean actual =
                Boolean.parseBoolean(actualValue.toString());

        boolean expected =
                Boolean.parseBoolean(expectedValue.trim());

        if (operator == Operator.EQUAL) {
            return actual == expected;
        }

        return false;
    }

    /**
     * Evaluates text and enum criteria.
     */
    private boolean evaluateText(
            Object actualValue,
            String expectedValue,
            Operator operator) {

        String actual =
                actualValue.toString().trim();

        if (operator == Operator.EQUAL) {
            return actual.equalsIgnoreCase(
                    expectedValue.trim()
            );
        }

        if (operator == Operator.IN) {

            String[] allowedValues =
                    expectedValue.split(",");

            for (String value : allowedValues) {

                if (actual.equalsIgnoreCase(
                        value.trim())) {

                    return true;
                }
            }

            return false;
        }

        return false;
    }
}