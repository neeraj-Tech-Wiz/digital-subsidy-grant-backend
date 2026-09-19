package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.ApplicationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.ApplicationEligibilityData;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.enums.VerificationLevel;
import com.infosys.subsidy.enums.VerificationRoute;
import com.infosys.subsidy.repository.ApplicationEligibilityDataRepository;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import com.infosys.subsidy.repository.UserRepository;
import com.infosys.subsidy.repository.VerificationHistoryRepository;
import com.infosys.subsidy.repository.SchemeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

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
    private final VerificationHistoryRepository verificationHistoryRepository;
    private final com.infosys.subsidy.repository.GrantDisbursementRepository grantDisbursementRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            BeneficiaryRepository beneficiaryRepository,
            SchemeRepository schemeRepository,
            ApplicationEligibilityDataRepository eligibilityDataRepository,
            EligibilityService eligibilityService,
            VerificationRoutingService verificationRoutingService,
            DocumentValidationService documentValidationService,
            UserRepository userRepository,
            VerificationHistoryRepository verificationHistoryRepository,
            com.infosys.subsidy.repository.GrantDisbursementRepository grantDisbursementRepository) {

        this.applicationRepository = applicationRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.eligibilityDataRepository = eligibilityDataRepository;
        this.eligibilityService = eligibilityService;
        this.verificationRoutingService = verificationRoutingService;
        this.documentValidationService = documentValidationService;
        this.userRepository = userRepository;
        this.verificationHistoryRepository = verificationHistoryRepository;
        this.grantDisbursementRepository = grantDisbursementRepository;
    }

    @org.springframework.beans.factory.annotation.Value("${rejection.cooldown.days:30}")
    private int cooldownDays;


    // ============================================================
    // HELPER — Derive the authenticated user from email.
    // ============================================================

    private User getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }


    // ============================================================
    // HELPER — Derive the Beneficiary from the authenticated user.
    // Returns 400 if no profile has been created yet.
    // ============================================================

    private Beneficiary getBeneficiaryForUser(User user) {
        return beneficiaryRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Please complete your beneficiary profile before applying for a scheme."));
    }


    // ============================================================
    // HELPER — Verify application ownership.
    // A BENEFICIARY user can only act on their own applications.
    // Returns 403 Forbidden if the ownership check fails.
    // ============================================================

    private void verifyApplicationOwnership(Application application, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        if (user.getRole() == UserRole.BENEFICIARY) {
            Beneficiary beneficiary = getBeneficiaryForUser(user);
            if (!beneficiary.getId().equals(application.getBeneficiaryId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are not authorized to access this application.");
            }
        }
    }


    // ============================================================
    // BENEFICIARY APPLIES FOR A SCHEME
    //
    // NEW SIGNATURE: beneficiaryId is no longer a parameter.
    // The beneficiary is derived from the authenticated JWT email.
    // The frontend must ONLY send schemeId and eligibilityData.
    // ============================================================

    @Transactional
    public Application applyForScheme(
            Long schemeId,
            ApplicationRequest request,
            String authenticatedEmail) {


        // ==========================================
        // 1. GET AUTHENTICATED USER
        // ==========================================

        User user = getAuthenticatedUser(authenticatedEmail);

        // Verify role
        if (user.getRole() != UserRole.BENEFICIARY) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only beneficiary users can apply for a scheme.");
        }


        // ==========================================
        // 2. GET LINKED BENEFICIARY PROFILE
        //    Error if no profile has been created yet.
        // ==========================================

        Beneficiary beneficiary = getBeneficiaryForUser(user);


        // ==========================================
        // 3. GET SCHEME
        // ==========================================

        Scheme scheme = schemeRepository
                .findById(schemeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Scheme not found with ID: " + schemeId));


        // ==========================================
        // 3.5 CHECK COOLDOWN
        // ==========================================

        applicationRepository.findFirstByBeneficiaryIdAndSchemeIdAndStatusOrderByRejectedAtDesc(beneficiary.getId(), scheme.getId(), ApplicationStatus.REJECTED)
            .ifPresent(recentApp -> {
                if (recentApp.getRejectedAt() != null) {
                    LocalDateTime expiresAt = recentApp.getRejectedAt().plusDays(cooldownDays);
                    if (LocalDateTime.now().isBefore(expiresAt)) {
                        long remainingDays = java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
                        if (remainingDays < 1 && LocalDateTime.now().isBefore(expiresAt)) remainingDays = 1; 
                        
                        throw new com.infosys.subsidy.exception.ApplicationCooldownException(
                            "You cannot reapply for this scheme yet because your previous application was rejected.",
                            scheme.getId(), recentApp.getRejectedAt(), expiresAt, (int) remainingDays);
                    }
                }
            });


        // ==========================================
        // 4. GET DYNAMIC ELIGIBILITY DATA
        // ==========================================


        Map<String, String> eligibilityData =
                request.getEligibilityData();

        if (eligibilityData == null || eligibilityData.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Eligibility data is required");
        }


        // ==========================================
        // 5. EVALUATE ELIGIBILITY CRITERIA
        // ==========================================

        int totalScore = 0;
        boolean mandatoryCriteriaPassed = true;

        for (EligibilityCriteria criterion : scheme.getCriteriaList()) {

            if (!criterion.isActive()) {
                continue;
            }

            String fieldName = criterion.getFieldName();

            if (fieldName == null || fieldName.isBlank()) {
                if (criterion.isMandatory()) {
                    mandatoryCriteriaPassed = false;
                }
                continue;
            }

            boolean passed = eligibilityService.evaluateCriterion(
                    eligibilityData,
                    criterion
            );

            if (passed) {
                totalScore += criterion.getWeight();
            }

            if (criterion.isMandatory() && !passed) {
                mandatoryCriteriaPassed = false;
            }
        }


        // ==========================================
        // 6. DETERMINE ELIGIBILITY STATUS
        // ==========================================

        ApplicationStatus applicationStatus;

        if (mandatoryCriteriaPassed && totalScore >= 60) {
            applicationStatus = ApplicationStatus.ELIGIBLE;
        } else {
            applicationStatus = ApplicationStatus.NOT_ELIGIBLE;
        }


        // ==========================================
        // 7. CREATE APPLICATION
        //    beneficiaryId is set from the LINKED profile,
        //    never from the frontend.
        // ==========================================

        Application application = new Application();

        application.setApplicationDate(LocalDateTime.now());
        application.setBeneficiaryId(beneficiary.getId());  // internal beneficiary ID from DB
        application.setSchemeId(scheme.getId());
        application.setEligibilityScore(totalScore);


        // ==========================================
        // 8. HANDLE VERIFICATION WORKFLOW
        // ==========================================

        if (applicationStatus == ApplicationStatus.NOT_ELIGIBLE) {

            application.setStatus(ApplicationStatus.NOT_ELIGIBLE);
            application.setCurrentVerificationLevel(null);
            application.setVerificationRoute(null);
            application.setRemarks(
                    "Application did not meet the eligibility requirements. "
                            + "Score: " + totalScore
                            + ". Minimum required score: 60.");

        } else {

            application.setStatus(ApplicationStatus.DOCUMENTS_PENDING);
            application.setCurrentVerificationLevel(null);
            application.setVerificationRoute(null);
            application.setRemarks(
                    "Application is eligible. Please upload all mandatory documents before submission.");
        }


        // ==========================================
        // 9. SAVE APPLICATION
        // ==========================================

        Application savedApplication = applicationRepository.save(application);


        // ==========================================
        // 10. SAVE ELIGIBILITY ANSWERS
        // ==========================================

        for (Map.Entry<String, String> entry : eligibilityData.entrySet()) {

            ApplicationEligibilityData data = new ApplicationEligibilityData();
            data.setApplicationId(savedApplication.getId());
            data.setCriterionName(entry.getKey());
            data.setActualValue(entry.getValue());
            eligibilityDataRepository.save(data);
        }


        // ==========================================
        // 11. DEBUG LOG
        // ==========================================




        return savedApplication;
    }


    // ============================================================
    // SUBMIT APPLICATION DOCUMENTS && TRIGGER VERIFICATION
    //
    // Ownership is verified server-side using the JWT email.
    // ============================================================

    @Transactional
    public Application submitApplicationDocuments(
            Long applicationId,
            String authenticatedEmail) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found with ID: " + applicationId));

        // Verify ownership
        verifyApplicationOwnership(application, authenticatedEmail);

        if (application.getStatus() != ApplicationStatus.DOCUMENTS_PENDING && application.getStatus() != ApplicationStatus.RETURNED_TO_APPLICANT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Application documents are not pending. Current status: " + application.getStatus());
        }

        boolean isValid = documentValidationService.validateMandatoryDocumentsUploaded(
                application.getSchemeId(),
                application.getId()
        );

        if (!isValid) {
            var missingDocs = documentValidationService.getMissingMandatoryDocuments(
                    application.getSchemeId(),
                    application.getId()
            );
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mandatory documents are missing: " + missingDocs);
        }

        Scheme scheme = schemeRepository.findById(application.getSchemeId()).orElseThrow();

        int maxScore = scheme.getCriteriaList().stream()
                .filter(EligibilityCriteria::isActive)
                .mapToInt(EligibilityCriteria::getWeight)
                .sum();

        VerificationRoute route = verificationRoutingService.determineRoute(
                application.getEligibilityScore(),
                maxScore,
                scheme.getGrantAmount().doubleValue()
        );
        application.setVerificationRoute(route);

        application.setStatus(ApplicationStatus.PENDING_VERIFICATION);
        application.setCurrentVerificationLevel(VerificationLevel.LEVEL_1);
        application.setRemarks(
                "Application is eligible and pending Level 1 verification. Verification route: " + route);

        LocalDateTime assignedAt = LocalDateTime.now();
        application.setVerificationAssignedAt(assignedAt);
        application.setVerificationDueDate(assignedAt.plusDays(3));

        return applicationRepository.save(application);
    }

    // ============================================================
    // GET MY APPLICATIONS
    // ============================================================

    @Transactional(readOnly = true)
    public List<Application> getMyApplications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Beneficiary beneficiary = beneficiaryRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary profile not found. Please complete your profile."));

        return applicationRepository.findByBeneficiaryId(beneficiary.getId());
    }


    // ============================================================
    // LEGACY OVERLOAD — kept for ConsoleApplicationRunner.

    // Looks up a beneficiary by ID directly (no JWT needed in console).
    // The web API must always use applyForScheme(Long, ApplicationRequest, String).
    // ============================================================

    @Transactional
    public Application applyForScheme(
            Long beneficiaryId,
            Long schemeId,
            ApplicationRequest request) {

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Beneficiary not found with ID: " + beneficiaryId));

        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Scheme not found with ID: " + schemeId));

        Map<String, String> eligibilityData = request.getEligibilityData();
        if (eligibilityData == null || eligibilityData.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Eligibility data is required");
        }

        int totalScore = 0;
        boolean mandatoryCriteriaPassed = true;

        for (EligibilityCriteria criterion : scheme.getCriteriaList()) {
            if (!criterion.isActive()) continue;
            String fieldName = criterion.getFieldName();
            if (fieldName == null || fieldName.isBlank()) {
                if (criterion.isMandatory()) mandatoryCriteriaPassed = false;
                continue;
            }
            boolean passed = eligibilityService.evaluateCriterion(eligibilityData, criterion);
            if (passed) totalScore += criterion.getWeight();
            if (criterion.isMandatory() && !passed) mandatoryCriteriaPassed = false;
        }

        ApplicationStatus applicationStatus = (mandatoryCriteriaPassed && totalScore >= 60)
                ? ApplicationStatus.ELIGIBLE
                : ApplicationStatus.NOT_ELIGIBLE;

        Application application = new Application();
        application.setApplicationDate(LocalDateTime.now());
        application.setBeneficiaryId(beneficiary.getId());
        application.setSchemeId(scheme.getId());
        application.setEligibilityScore(totalScore);

        if (applicationStatus == ApplicationStatus.NOT_ELIGIBLE) {
            application.setStatus(ApplicationStatus.NOT_ELIGIBLE);
            application.setCurrentVerificationLevel(null);
            application.setVerificationRoute(null);
            application.setRemarks("Not eligible. Score: " + totalScore + ". Minimum: 60.");
        } else {
            application.setStatus(ApplicationStatus.DOCUMENTS_PENDING);
            application.setCurrentVerificationLevel(null);
            application.setVerificationRoute(null);
            application.setRemarks("Eligible. Upload mandatory documents.");
        }

        Application saved = applicationRepository.save(application);

        for (Map.Entry<String, String> entry : eligibilityData.entrySet()) {
            ApplicationEligibilityData data = new ApplicationEligibilityData();
            data.setApplicationId(saved.getId());
            data.setCriterionName(entry.getKey());
            data.setActualValue(entry.getValue());
            eligibilityDataRepository.save(data);
        }

        return saved;
    }


    // ============================================================
    // LEGACY OVERLOAD — kept for DocumentConsoleDemo.
    // Bypasses ownership check (no HTTP session in console).
    // ============================================================

    @Transactional
    public Application submitApplicationDocuments(Long applicationId) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found with ID: " + applicationId));

        if (application.getStatus() != ApplicationStatus.DOCUMENTS_PENDING && application.getStatus() != ApplicationStatus.RETURNED_TO_APPLICANT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Application documents are not pending. Current status: " + application.getStatus());
        }

        boolean isValid = documentValidationService.validateMandatoryDocumentsUploaded(
                application.getSchemeId(), application.getId());

        if (!isValid) {
            var missingDocs = documentValidationService.getMissingMandatoryDocuments(
                    application.getSchemeId(), application.getId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mandatory documents are missing: " + missingDocs);
        }

        Scheme scheme = schemeRepository.findById(application.getSchemeId()).orElseThrow();

        int maxScore = scheme.getCriteriaList().stream()
                .filter(EligibilityCriteria::isActive)
                .mapToInt(EligibilityCriteria::getWeight)
                .sum();

        VerificationRoute route = verificationRoutingService.determineRoute(
                application.getEligibilityScore(), maxScore, scheme.getGrantAmount().doubleValue());
        application.setVerificationRoute(route);
        application.setStatus(ApplicationStatus.PENDING_VERIFICATION);
        application.setCurrentVerificationLevel(VerificationLevel.LEVEL_1);
        application.setRemarks("Pending Level 1 verification. Route: " + route);

        LocalDateTime assignedAt = LocalDateTime.now();
        application.setVerificationAssignedAt(assignedAt);
        application.setVerificationDueDate(assignedAt.plusDays(3));

        return applicationRepository.save(application);
    }

    // ============================================================
    // GET APPLICATION ANSWERS
    // ============================================================

    @Transactional(readOnly = true)
    public Map<String, String> getApplicationAnswers(Long applicationId, String authenticatedEmail) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        // Verify ownership
        verifyApplicationOwnership(application, authenticatedEmail);

        List<ApplicationEligibilityData> dataList = eligibilityDataRepository.findByApplicationId(applicationId);
        Map<String, String> answers = new java.util.HashMap<>();
        for (ApplicationEligibilityData d : dataList) {
            answers.put(d.getCriterionName(), d.getActualValue());
        }
        return answers;
    }

    // ============================================================
    // RESUBMIT APPLICATION (RECALC SCORE + TRIGGER VERIFICATION)
    // ============================================================

    @Transactional
    public Application resubmitApplication(
            Long applicationId,
            ApplicationRequest request,
            String authenticatedEmail) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        // Verify ownership
        verifyApplicationOwnership(application, authenticatedEmail);

        if (application.getStatus() != ApplicationStatus.RETURNED_TO_APPLICANT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application is not in RETURNED_TO_APPLICANT status");
        }

        Map<String, String> newEligibilityData = request.getEligibilityData();
        if (newEligibilityData == null || newEligibilityData.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Eligibility data is required");
        }

        // 1. Delete old eligibility data answers and save new ones

        List<ApplicationEligibilityData> oldDataList = eligibilityDataRepository.findByApplicationId(applicationId);
        eligibilityDataRepository.deleteAll(oldDataList);

        for (Map.Entry<String, String> entry : newEligibilityData.entrySet()) {
            ApplicationEligibilityData data = new ApplicationEligibilityData();
            data.setApplicationId(application.getId());
            data.setCriterionName(entry.getKey());
            data.setActualValue(entry.getValue());
            eligibilityDataRepository.save(data);
        }

        // 2. Validate mandatory documents
        boolean isValid = documentValidationService.validateMandatoryDocumentsUploaded(
                application.getSchemeId(),
                application.getId()
        );

        if (!isValid) {
            var missingDocs = documentValidationService.getMissingMandatoryDocuments(
                application.getSchemeId(), application.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mandatory documents are missing: " + missingDocs);
        }

        // 3. Recalculate Score using Scheme criteria
        Scheme scheme = schemeRepository.findById(application.getSchemeId()).orElseThrow();
        int totalScore = 0;
        int maxScore = 0;
        boolean mandatoryCriteriaPassed = true;

        for (EligibilityCriteria criterion : scheme.getCriteriaList()) {
            if (!criterion.isActive()) continue;
            
            String fieldName = criterion.getFieldName();
            maxScore += criterion.getWeight();

            if (fieldName == null || fieldName.isBlank()) {
                if (criterion.isMandatory()) mandatoryCriteriaPassed = false;
                continue;
            }

            boolean passed = eligibilityService.evaluateCriterion(newEligibilityData, criterion);
            if (passed) {
                totalScore += criterion.getWeight();
            }

            if (criterion.isMandatory() && !passed) {
                mandatoryCriteriaPassed = false;
            }
        }

        if (!mandatoryCriteriaPassed || totalScore < 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Updated application fails minimum eligibility requirements (Must be 60 points + all mandatory criteria).");
        }

        application.setEligibilityScore(totalScore);

        // 4. Calculate verification route dynamically
        VerificationRoute route = verificationRoutingService.determineRoute(
                application.getEligibilityScore(),
                maxScore,
                scheme.getGrantAmount().doubleValue()
        );
        application.setVerificationRoute(route);

        // 5. Update Status
        application.setStatus(ApplicationStatus.PENDING_VERIFICATION);
        application.setCurrentVerificationLevel(VerificationLevel.LEVEL_1);
        application.setRemarks("Application resubmitted and queued for Level 1 Verification. Route: " + route);

        LocalDateTime assignedAt = LocalDateTime.now();
        application.setVerificationAssignedAt(assignedAt);
        application.setVerificationDueDate(assignedAt.plusDays(3));

        return applicationRepository.save(application);
    }

    // ============================================================
    // GET APPLICATION HISTORY FOR BENEFICIARY
    // ============================================================

    @Transactional(readOnly = true)
    public List<com.infosys.subsidy.dto.VerificationHistoryDTO> getApplicationHistory(Long applicationId, String authenticatedEmail) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
                
        // Verify ownership
        verifyApplicationOwnership(application, authenticatedEmail);

        // Fetch history
        return verificationHistoryRepository.findByApplicationIdOrderByActionTimestampAsc(applicationId).stream()
                .map(h -> new com.infosys.subsidy.dto.VerificationHistoryDTO(
                        h.getOfficerName(), 
                        h.getOfficerRole(), 
                        h.getVerificationLevel(), 
                        h.getAction(), 
                        h.getRemarks(), 
                        h.getActionTimestamp()))
                .collect(java.util.stream.Collectors.toList());
    }

    // ============================================================
    // GET APPLICATION GRANT DISBURSEMENT
    // ============================================================

    @Transactional(readOnly = true)
    public com.infosys.subsidy.entity.GrantDisbursement getApplicationGrant(Long applicationId, String authenticatedEmail) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found with ID: " + applicationId));
                
        // MANDATORY OWNERSHIP CHECK - ENFORCES RULE 2
        verifyApplicationOwnership(application, authenticatedEmail);

        return grantDisbursementRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No grant disbursement found for application ID: " + applicationId));
    }

    // ============================================================
    // GET ALL MY GRANTS
    // ============================================================

    @Transactional(readOnly = true)
    public List<com.infosys.subsidy.entity.GrantDisbursement> getMyGrants(String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        Beneficiary beneficiary = getBeneficiaryForUser(user);
        
        List<Application> myApps = applicationRepository.findByBeneficiaryId(beneficiary.getId());
        List<com.infosys.subsidy.entity.GrantDisbursement> myGrants = new java.util.ArrayList<>();
        
        for (Application app : myApps) {
            grantDisbursementRepository.findByApplicationId(app.getId()).ifPresent(myGrants::add);
        }
        
        // Sort newest first
        myGrants.sort((g1, g2) -> g2.getDisbursedAt().compareTo(g1.getDisbursedAt()));
        
        return myGrants;
    }

    // ============================================================
    // GET SCHEME ELIGIBILITY (COOLDOWN)
    // ============================================================
    @Transactional(readOnly = true)
    public com.infosys.subsidy.dto.CooldownStatusDTO getSchemeEligibility(Long schemeId, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        Beneficiary beneficiary = getBeneficiaryForUser(user);
        
        var dto = new com.infosys.subsidy.dto.CooldownStatusDTO(true, false, null, null, null, null, cooldownDays);
        
        applicationRepository.findFirstByBeneficiaryIdAndSchemeIdAndStatusOrderByRejectedAtDesc(beneficiary.getId(), schemeId, ApplicationStatus.REJECTED)
            .ifPresent(recentApp -> {
                if (recentApp.getRejectedAt() != null) {
                    LocalDateTime expiresAt = recentApp.getRejectedAt().plusDays(cooldownDays);
                    if (LocalDateTime.now().isBefore(expiresAt)) {
                        long remainingDays = java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
                        if (remainingDays < 1 && LocalDateTime.now().isBefore(expiresAt)) remainingDays = 1;
                        dto.setCanApply(false);
                        dto.setCooldownActive(true);
                        dto.setReason("APPLICATION_COOLDOWN_ACTIVE");
                        dto.setRejectedAt(recentApp.getRejectedAt());
                        dto.setCooldownExpiresAt(expiresAt);
                        dto.setRemainingDays((int) remainingDays);
                    }
                }
            });
            
        return dto;
    }

    // ============================================================
    // GET ALL ACTIVE COOLDOWNS
    // ============================================================
    @Transactional(readOnly = true)
    public java.util.Map<Long, com.infosys.subsidy.dto.CooldownStatusDTO> getActiveCooldowns(String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        Beneficiary beneficiary = getBeneficiaryForUser(user);
        
        List<Application> rejectedApps = applicationRepository.findByBeneficiaryId(beneficiary.getId()).stream()
            .filter(app -> app.getStatus() == ApplicationStatus.REJECTED && app.getRejectedAt() != null)
            .collect(java.util.stream.Collectors.toList());
            
        java.util.Map<Long, com.infosys.subsidy.dto.CooldownStatusDTO> map = new java.util.HashMap<>();
        
        for (Application app : rejectedApps) {
            LocalDateTime expiresAt = app.getRejectedAt().plusDays(cooldownDays);
            if (LocalDateTime.now().isBefore(expiresAt)) {
                long remainingDays = java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
                if (remainingDays < 1 && LocalDateTime.now().isBefore(expiresAt)) remainingDays = 1;
                
                var dto = new com.infosys.subsidy.dto.CooldownStatusDTO(false, true, "APPLICATION_COOLDOWN_ACTIVE", app.getRejectedAt(), expiresAt, (int) remainingDays, cooldownDays);
                
                if (!map.containsKey(app.getSchemeId()) || map.get(app.getSchemeId()).getRejectedAt().isBefore(app.getRejectedAt())) {
                    map.put(app.getSchemeId(), dto);
                }
            }
        }
        
        return map;
    }
}