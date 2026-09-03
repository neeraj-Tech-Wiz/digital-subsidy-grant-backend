package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.ApplicationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.ApplicationEligibilityData;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.repository.ApplicationEligibilityDataRepository;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import com.infosys.subsidy.repository.SchemeRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final ApplicationEligibilityDataRepository eligibilityDataRepository;
    private final EligibilityService eligibilityService;


    public ApplicationService(
            ApplicationRepository applicationRepository,
            BeneficiaryRepository beneficiaryRepository,
            SchemeRepository schemeRepository,
            ApplicationEligibilityDataRepository eligibilityDataRepository,
            EligibilityService eligibilityService) {

        this.applicationRepository = applicationRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.eligibilityDataRepository = eligibilityDataRepository;
        this.eligibilityService = eligibilityService;
    }


    public Application applyForScheme(
            Long beneficiaryId,
            Long schemeId,
            ApplicationRequest request) {


        // ==========================================
        // 1. GET EXISTING BENEFICIARY
        // ==========================================

        Beneficiary beneficiary = beneficiaryRepository
                .findById(beneficiaryId)
                .orElseThrow(() ->
                        new RuntimeException("Beneficiary not found")
                );


        // ==========================================
        // 2. GET SCHEME
        // ==========================================

        Scheme scheme = schemeRepository
                .findById(schemeId)
                .orElseThrow(() ->
                        new RuntimeException("Scheme not found")
                );


        // ==========================================
        // 3. GET DYNAMIC ELIGIBILITY DATA
        // ==========================================

        Map<String, String> eligibilityData =
                request.getEligibilityData();

        if (eligibilityData == null || eligibilityData.isEmpty()) {

            throw new RuntimeException(
                    "Eligibility data is required"
            );
        }


        // ==========================================
        // 4. START ELIGIBILITY CHECK
        // ==========================================

        int totalScore = 0;

        boolean mandatoryCriteriaPassed = true;


        System.out.println("\n========================================");
        System.out.println("DYNAMIC ELIGIBILITY CHECK STARTED");
        System.out.println("Beneficiary: " + beneficiary.getName());
        System.out.println("Beneficiary ID: " + beneficiaryId);
        System.out.println("Scheme: " + scheme.getSchemeName());
        System.out.println("========================================");


        // ==========================================
        // 5. EVALUATE EACH SCHEME CRITERION
        // ==========================================

        for (EligibilityCriteria criterion
                : scheme.getCriteriaList()) {


            // ==========================================
            // SKIP INACTIVE CRITERIA
            // ==========================================

            if (!criterion.isActive()) {

                System.out.println(
                        "Skipping inactive criterion: "
                                + criterion.getCriterionName()
                );

                continue;
            }


            // ==========================================
            // GET FIELD NAME
            // ==========================================

            String fieldName =
                    criterion.getFieldName();


            // ==========================================
            // VALIDATE FIELD NAME
            // ==========================================

            if (fieldName == null || fieldName.isBlank()) {

                System.out.println(
                        "Field name missing for criterion: "
                                + criterion.getCriterionName()
                );

                if (criterion.isMandatory()) {
                    mandatoryCriteriaPassed = false;
                }

                continue;
            }


            // ==========================================
            // GET ACTUAL VALUE USING FIELD NAME
            // ==========================================

            String actualValue =
                    eligibilityData.get(fieldName);


            // ==========================================
            // EVALUATE CRITERION
            // ==========================================

            boolean passed =
                    eligibilityService.evaluateCriterion(
                            eligibilityData,
                            criterion
                    );


            // ==========================================
            // DEBUG OUTPUT
            // ==========================================

            System.out.println("\n----------------------------------------");

            System.out.println(
                    "Criterion Name: "
                            + criterion.getCriterionName()
            );

            System.out.println(
                    "Field Name: "
                            + fieldName
            );

            System.out.println(
                    "Actual Value: "
                            + actualValue
            );

            System.out.println(
                    "Expected Value: "
                            + criterion.getExpectedValue()
            );

            System.out.println(
                    "Type: "
                            + criterion.getCriterionType()
            );

            System.out.println(
                    "Operator: "
                            + criterion.getOperator()
            );

            System.out.println(
                    "Mandatory: "
                            + criterion.isMandatory()
            );

            System.out.println(
                    "Weight: "
                            + criterion.getWeight()
            );

            System.out.println(
                    "Passed: "
                            + passed
            );


            // ==========================================
            // ADD SCORE
            // ==========================================

            if (passed) {

                totalScore += criterion.getWeight();

                System.out.println(
                        "Score Added: "
                                + criterion.getWeight()
                );

            } else {

                System.out.println(
                        "Score Added: 0"
                );
            }


            // ==========================================
            // CHECK MANDATORY CRITERIA
            // ==========================================

            if (criterion.isMandatory() && !passed) {

                mandatoryCriteriaPassed = false;

                System.out.println(
                        "❌ MANDATORY CRITERION FAILED"
                );
            }
        }


        // ==========================================
        // 6. FINAL STATUS
        // ==========================================

        String status;

        if (mandatoryCriteriaPassed && totalScore >= 60) {

            status = "ELIGIBLE";

        } else {

            status = "NOT_ELIGIBLE";
        }


        // ==========================================
        // FINAL DEBUG OUTPUT
        // ==========================================

        System.out.println("\n========================================");
        System.out.println("FINAL ELIGIBILITY RESULT");
        System.out.println("========================================");

        System.out.println(
                "Total Score: " + totalScore
        );

        System.out.println(
                "Mandatory Criteria Passed: "
                        + mandatoryCriteriaPassed
        );

        System.out.println(
                "Required Minimum Score: 60"
        );

        System.out.println(
                "Final Status: " + status
        );

        System.out.println(
                "========================================\n"
        );


        // ==========================================
        // 7. CREATE APPLICATION
        // ==========================================

        Application application =
                new Application();

        application.setApplicationDate(
                LocalDateTime.now()
        );

        application.setBeneficiaryId(
                beneficiary.getId()
        );

        application.setSchemeId(
                scheme.getId()
        );

        application.setEligibilityScore(
                totalScore
        );

        application.setStatus(
                status
        );


        // ==========================================
        // 8. SAVE APPLICATION FIRST
        // ==========================================

        Application savedApplication =
                applicationRepository.save(application);


        // ==========================================
        // 9. SAVE ALL DYNAMIC ELIGIBILITY ANSWERS
        // ==========================================

        for (Map.Entry<String, String> entry
                : eligibilityData.entrySet()) {

            ApplicationEligibilityData data =
                    new ApplicationEligibilityData();

            data.setApplicationId(
                    savedApplication.getId()
            );

            // This stores the FIELD NAME
            // Example: percentage, attendance, annualIncome
            data.setCriterionName(
                    entry.getKey()
            );

            data.setActualValue(
                    entry.getValue()
            );

            eligibilityDataRepository.save(data);
        }


        // ==========================================
        // 10. RETURN SAVED APPLICATION
        // ==========================================

        return savedApplication;
    }
}