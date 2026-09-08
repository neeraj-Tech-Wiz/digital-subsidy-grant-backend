package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.ApplicationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.ApplicationEligibilityData;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.VerificationLevel;
import com.infosys.subsidy.enums.VerificationRoute;
import com.infosys.subsidy.repository.ApplicationEligibilityDataRepository;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import com.infosys.subsidy.repository.SchemeRepository;
import com.infosys.subsidy.repository.UserRepository;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.entity.User;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final ApplicationEligibilityDataRepository eligibilityDataRepository;
    private final EligibilityService eligibilityService;
    private final VerificationRoutingService verificationRoutingService;
    private final DocumentValidationService documentValidationService;
    private final UserRepository userRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            BeneficiaryRepository beneficiaryRepository,
            SchemeRepository schemeRepository,
            ApplicationEligibilityDataRepository eligibilityDataRepository,
            EligibilityService eligibilityService,
            VerificationRoutingService verificationRoutingService,
            DocumentValidationService documentValidationService,
            UserRepository userRepository) {

        this.applicationRepository = applicationRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.eligibilityDataRepository = eligibilityDataRepository;
        this.eligibilityService = eligibilityService;
        this.verificationRoutingService = verificationRoutingService;
        this.documentValidationService = documentValidationService;
        this.userRepository = userRepository;
    }

    private void validateBeneficiaryOwnership(Beneficiary beneficiary) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            userRepository.findByEmail(auth.getName()).ifPresent(user -> {
                if (user.getRole() == UserRole.BENEFICIARY) {
                    if (!beneficiary.getEmail().equalsIgnoreCase(user.getEmail())) {
                        throw new RuntimeException("Unauthorized access to application");
                    }
                }
            });
        }
    }


    // =====================================================
    // BENEFICIARY APPLIES FOR SCHEME
    // =====================================================

    @Transactional
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
                        new RuntimeException(
                                "Beneficiary not found with ID: "
                                        + beneficiaryId
                        )
                );

        validateBeneficiaryOwnership(beneficiary);

        // ==========================================
        // 2. GET SCHEME
        // ==========================================

        Scheme scheme = schemeRepository
                .findById(schemeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Scheme not found with ID: "
                                        + schemeId
                        )
                );


        // ==========================================
        // 3. GET DYNAMIC ELIGIBILITY DATA
        // ==========================================

        Map<String, String> eligibilityData =
                request.getEligibilityData();

        if (eligibilityData == null
                || eligibilityData.isEmpty()) {

            throw new RuntimeException(
                    "Eligibility data is required"
            );
        }


        // ==========================================
        // 4. START ELIGIBILITY CHECK
        // ==========================================

        int totalScore = 0;

        boolean mandatoryCriteriaPassed = true;




        // ==========================================
        // 5. EVALUATE EACH SCHEME CRITERION
        // ==========================================

        for (EligibilityCriteria criterion
                : scheme.getCriteriaList()) {


            // ==========================================
            // SKIP INACTIVE CRITERIA
            // ==========================================

            if (!criterion.isActive()) {
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

            if (fieldName == null
                    || fieldName.isBlank()) {

                if (criterion.isMandatory()) {

                    mandatoryCriteriaPassed = false;
                }

                continue;
            }


            // ==========================================
            // GET ACTUAL VALUE
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
            // ADD SCORE
            // ==========================================

            if (passed) {
                totalScore +=
                        criterion.getWeight();
            }


            // ==========================================
            // CHECK MANDATORY CRITERIA
            // ==========================================

            if (criterion.isMandatory()
                    && !passed) {
                mandatoryCriteriaPassed = false;
            }
        }


        // ==========================================
        // 6. DETERMINE ELIGIBILITY STATUS
        // ==========================================

        ApplicationStatus applicationStatus;

        if (mandatoryCriteriaPassed
                && totalScore >= 60) {

            applicationStatus =
                    ApplicationStatus.ELIGIBLE;

        } else {

            applicationStatus =
                    ApplicationStatus.NOT_ELIGIBLE;
        }





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


        // ==========================================
        // 8. HANDLE VERIFICATION WORKFLOW
        // ==========================================

        if (applicationStatus == ApplicationStatus.NOT_ELIGIBLE) {

            application.setStatus(
                    ApplicationStatus.NOT_ELIGIBLE
            );

            application.setCurrentVerificationLevel(null);

            application.setVerificationRoute(null);

            application.setRemarks(
                    "Application did not meet the eligibility requirements. "
                            + "Score: " + totalScore
                            + ". Minimum required score: 60."
            );

        } else {

            // --------------------------------------
            // ELIGIBLE → DOCUMENTS PENDING
            // --------------------------------------

            application.setStatus(
                    ApplicationStatus.DOCUMENTS_PENDING
            );

            application.setCurrentVerificationLevel(null);

            application.setVerificationRoute(null);

            application.setRemarks(
                    "Application is eligible. Please upload all mandatory documents before submission."
            );
        }


        // ==========================================
        // 9. SAVE APPLICATION
        // ==========================================

        Application savedApplication =
                applicationRepository.save(
                        application
                );


        // ==========================================
        // 10. SAVE ELIGIBILITY ANSWERS
        // ==========================================

        for (Map.Entry<String, String> entry
                : eligibilityData.entrySet()) {

            ApplicationEligibilityData data =
                    new ApplicationEligibilityData();


            data.setApplicationId(
                    savedApplication.getId()
            );


            // Store dynamic field name
            // Example:
            // annualIncome
            // landArea
            // percentage
            // attendance

            data.setCriterionName(
                    entry.getKey()
            );


            data.setActualValue(
                    entry.getValue()
            );


            eligibilityDataRepository.save(
                    data
            );
        }


        // ==========================================
        // 11. FINAL WORKFLOW DEBUG
        // ==========================================

        System.out.println(
                "\n========================================"
        );

        System.out.println(
                "APPLICATION WORKFLOW RESULT"
        );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "Application ID: "
                        + savedApplication.getId()
        );

        System.out.println(
                "Eligibility Score: "
                        + savedApplication.getEligibilityScore()
        );

        System.out.println(
                "Status: "
                        + savedApplication.getStatus()
        );

        System.out.println(
                "Verification Level: "
                        + savedApplication
                        .getCurrentVerificationLevel()
        );

        System.out.println(
                "Remarks: "
                        + savedApplication.getRemarks()
        );

        System.out.println(
                "========================================\n"
        );


        // ==========================================
        // 12. RETURN APPLICATION
        // ==========================================

        return savedApplication;
    }


    // =====================================================
    // SUBMIT APPLICATION DOCUMENTS && TRIGGER VERIFICATION
    // =====================================================

    @Transactional
    public Application submitApplicationDocuments(Long applicationId) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with ID: " + applicationId));

        Beneficiary beneficiary = beneficiaryRepository.findById(application.getBeneficiaryId())
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));
        validateBeneficiaryOwnership(beneficiary);

        if (application.getStatus() != ApplicationStatus.DOCUMENTS_PENDING) {
            throw new RuntimeException("Application applies are not pending documents check. Current status: " + application.getStatus());
        }

        boolean isValid = documentValidationService.validateMandatoryDocumentsUploaded(
                application.getSchemeId(),
                application.getId()
        );

        if (!isValid) {
            var missingDocs = documentValidationService.getMissingMandatoryDocuments(application.getSchemeId(), application.getId());
            throw new RuntimeException("Mandatory documents are missing: " + missingDocs);
        }

        Scheme scheme = schemeRepository.findById(application.getSchemeId()).orElseThrow();

        VerificationRoute route =
                verificationRoutingService.determineRoute(
                        application.getEligibilityScore(),
                        scheme.getGrantAmount()
                );
        application.setVerificationRoute(route);


        application.setStatus(ApplicationStatus.PENDING_VERIFICATION);
        application.setCurrentVerificationLevel(VerificationLevel.LEVEL_1);
        application.setRemarks("Application is eligible and pending Level 1 verification. Verification route: " + route);

        LocalDateTime assignedAt = LocalDateTime.now();
        application.setVerificationAssignedAt(assignedAt);
        application.setVerificationDueDate(assignedAt.plusDays(3));

        return applicationRepository.save(application);
    }
}