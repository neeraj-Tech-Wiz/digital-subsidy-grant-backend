package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.VerificationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.VerificationLevel;
import com.infosys.subsidy.repository.ApplicationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.repository.UserRepository;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import com.infosys.subsidy.repository.SchemeRepository;
import com.infosys.subsidy.repository.VerificationHistoryRepository;
import com.infosys.subsidy.repository.ApplicationDocumentRepository;
import com.infosys.subsidy.dto.OfficerQueueItemDTO;
import com.infosys.subsidy.dto.ApplicationDetailDTO;
import com.infosys.subsidy.dto.ApplicationDocumentReviewDTO;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.entity.VerificationHistory;
import com.infosys.subsidy.repository.ApplicationEligibilityDataRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VerificationService {

    private final ApplicationRepository applicationRepository;
    private final VerificationRoutingService verificationRoutingService;
    private final DocumentValidationService documentValidationService;
    private final UserRepository userRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final VerificationHistoryRepository verificationHistoryRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final ApplicationEligibilityDataRepository applicationEligibilityDataRepository;
    private final EligibilityService eligibilityService;

    public VerificationService(
            ApplicationRepository applicationRepository,
            VerificationRoutingService verificationRoutingService,
            DocumentValidationService documentValidationService,
            UserRepository userRepository,
            BeneficiaryRepository beneficiaryRepository,
            SchemeRepository schemeRepository,
            VerificationHistoryRepository verificationHistoryRepository,
            ApplicationDocumentRepository applicationDocumentRepository,
            ApplicationEligibilityDataRepository applicationEligibilityDataRepository,
            EligibilityService eligibilityService) {

        this.applicationRepository = applicationRepository;
        this.verificationRoutingService = verificationRoutingService;
        this.documentValidationService = documentValidationService;
        this.userRepository = userRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.verificationHistoryRepository = verificationHistoryRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.applicationEligibilityDataRepository = applicationEligibilityDataRepository;
        this.eligibilityService = eligibilityService;
    }


    // =====================================================
    // VERIFY APPLICATION
    // =====================================================

    @Transactional
    public Application verifyApplication(
            Long applicationId,
            VerificationRequest request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User officer = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        
        if (!officer.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Officer account is deactivated");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found with ID: " + applicationId));

        if (application.getStatus() == ApplicationStatus.NOT_ELIGIBLE || 
            application.getStatus() == ApplicationStatus.APPROVED || 
            application.getStatus() == ApplicationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is already processed or not eligible.");
        }



        VerificationLevel currentLevel = application.getCurrentVerificationLevel();
        if (currentLevel == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not assigned to a verification level");
        }
        
        VerificationLevel officerLevel = getVerificationLevelFromRole(officer.getRole());
        
        ApplicationStatus expectedStatus = (officerLevel == VerificationLevel.LEVEL_3) 
                ? ApplicationStatus.ESCALATED 
                : ApplicationStatus.PENDING_VERIFICATION;

        if (application.getStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not currently pending verification or escalation.");
        }

        if (currentLevel != officerLevel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to verify this application at its current level.");
        }

        VerificationLevel nextLevel = verificationRoutingService.getNextLevel(currentLevel, application.getVerificationRoute());

        // Save history
        VerificationHistory history = new VerificationHistory();
        history.setApplicationId(application.getId());
        history.setOfficerId(officer.getId());
        history.setOfficerName(officer.getName());
        history.setOfficerRole(officer.getRole().name());
        history.setVerificationLevel(currentLevel.name());
        history.setRemarks(request.getRemarks());
        history.setActionTimestamp(LocalDateTime.now());
        
        application.setRemarks(request.getRemarks());

        if (!Boolean.TRUE.equals(request.getApproved())) {
            history.setAction("REJECTED");
            application.setStatus(ApplicationStatus.REJECTED);
            application.setCurrentVerificationLevel(null);
            application.setVerificationCompletedAt(LocalDateTime.now());
            application.setVerificationDueDate(null);
        } else {
            if (currentLevel == VerificationLevel.FINAL_APPROVAL || nextLevel == null) {
                // Safety Check: Document Verification
                boolean allDocsVerified = documentValidationService.validateMandatoryDocumentsVerified(
                        application.getSchemeId(), application.getId());

                if (!allDocsVerified) {
                    var unverifiedDocs = documentValidationService.getUnverifiedMandatoryDocuments(application.getSchemeId(), application.getId());
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Application cannot be finally approved. Mandatory documents are not fully verified: " + unverifiedDocs);
                }
                
                history.setAction("FINAL_APPROVED");
                application.setStatus(ApplicationStatus.APPROVED);
                application.setCurrentVerificationLevel(null);
                application.setVerificationCompletedAt(LocalDateTime.now());
                application.setVerificationDueDate(null);
            } else {
                if (currentLevel == VerificationLevel.LEVEL_3) {
                    history.setAction("ESCALATION_REVIEWED");
                } else if (currentLevel == VerificationLevel.LEVEL_1) {
                    history.setAction("APPLICATION_REVIEWED");
                } else {
                    history.setAction("FORWARDED");
                }
                application.setCurrentVerificationLevel(nextLevel);
                application.setStatus(ApplicationStatus.PENDING_VERIFICATION);
                
                LocalDateTime assignedAt = LocalDateTime.now();
                application.setVerificationAssignedAt(assignedAt);
                application.setVerificationDueDate(assignedAt.plusDays(3));
            }
        }
        
        verificationHistoryRepository.save(history);
        return applicationRepository.save(application);
    }
    
    // =====================================================
    // GET APPLICATION DETAILS FOR REVIEW (Phase 2 & 3)
    // =====================================================

    @Transactional(readOnly = true)
    public ApplicationDetailDTO getApplicationDetailsForReview(Long applicationId, String email) {
        User officer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated officer not found"));

        if (!officer.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Officer account is deactivated");
        }

        VerificationLevel officerLevel = getVerificationLevelFromRole(officer.getRole());

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if (application.getCurrentVerificationLevel() != officerLevel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to review this application in its current verification level.");
        }

        ApplicationStatus expectedStatus = (officerLevel == VerificationLevel.LEVEL_3) 
                ? ApplicationStatus.ESCALATED 
                : ApplicationStatus.PENDING_VERIFICATION;

        if (application.getStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is no longer pending verification or escalation.");
        }

        Beneficiary beneficiary = beneficiaryRepository.findById(application.getBeneficiaryId())
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));

        Scheme scheme = schemeRepository.findById(application.getSchemeId())
                .orElseThrow(() -> new RuntimeException("Scheme not found"));

        String maskedAadhaar = "XXXX-XXXX-" + beneficiary.getAadhaarNumber().substring(Math.max(0, beneficiary.getAadhaarNumber().length() - 4));

        ApplicationDetailDTO dto = new ApplicationDetailDTO();
        dto.setApplicationId(application.getId());
        dto.setStatus(application.getStatus());
        dto.setCurrentVerificationLevel(application.getCurrentVerificationLevel());
        dto.setVerificationRoute(application.getVerificationRoute());
        dto.setEligibilityScore(application.getEligibilityScore());
        dto.setApplicationDate(application.getApplicationDate());
        dto.setVerificationDueDate(application.getVerificationDueDate());
        dto.setRemarks(application.getRemarks());

        dto.setBeneficiaryName(beneficiary.getName());
        dto.setFatherName(beneficiary.getFatherName());
        dto.setGender(beneficiary.getGender());
        dto.setAge(beneficiary.getAge());
        dto.setMobileNumber(beneficiary.getMobileNumber());
        dto.setEmail(beneficiary.getEmail());
        dto.setAddress(beneficiary.getAddress());
        dto.setMaskedAadhaar(maskedAadhaar);

        dto.setSchemeName(scheme.getSchemeName());
        dto.setSchemeCode(scheme.getSchemeCode());

        List<ApplicationDocumentReviewDTO> docDtos = applicationDocumentRepository.findByApplicationId(application.getId())
            .stream().map(doc -> new ApplicationDocumentReviewDTO(doc.getId(), doc.getDocumentType(), doc.getOriginalFileName(), doc.getDocumentStatus()))
            .collect(Collectors.toList());
        
        dto.setDocuments(docDtos);

        // Load Verification History
        List<com.infosys.subsidy.dto.VerificationHistoryDTO> historyDtos = verificationHistoryRepository.findByApplicationIdOrderByActionTimestampAsc(application.getId())
            .stream().map(h -> new com.infosys.subsidy.dto.VerificationHistoryDTO(h.getOfficerName(), h.getOfficerRole(), h.getVerificationLevel(), h.getAction(), h.getRemarks(), h.getActionTimestamp()))
            .collect(Collectors.toList());
        dto.setVerificationHistory(historyDtos);
        
        // Process Eligibility Score Breakdown
        List<com.infosys.subsidy.entity.ApplicationEligibilityData> dataList = applicationEligibilityDataRepository.findByApplicationId(application.getId());
        java.util.Map<String, String> eligibilityDataMap = dataList.stream().collect(Collectors.toMap(com.infosys.subsidy.entity.ApplicationEligibilityData::getCriterionName, com.infosys.subsidy.entity.ApplicationEligibilityData::getActualValue, (a,b) -> a));
        
        List<com.infosys.subsidy.dto.EligibilityScoreDetailsDTO> eligibilityDetails = new java.util.ArrayList<>();
        if (scheme.getCriteriaList() != null) {
            for (com.infosys.subsidy.entity.EligibilityCriteria criterion : scheme.getCriteriaList()) {
                if (!criterion.isActive()) continue;
                String fieldName = criterion.getFieldName();
                if (fieldName == null || fieldName.isBlank()) fieldName = criterion.getCriterionName();
                String actualVal = eligibilityDataMap.getOrDefault(fieldName, "N/A");
                
                boolean passed = eligibilityService.evaluateCriterion(eligibilityDataMap, criterion);
                int maxPoints = criterion.getWeight();
                int points = passed ? maxPoints : 0;
                
                String result;
                if (passed) {
                    result = "ELIGIBLE";
                } else if (!criterion.isMandatory()) {
                    if ("N/A".equals(actualVal) || actualVal.trim().isEmpty() || "Not Provided".equalsIgnoreCase(actualVal) || "false".equalsIgnoreCase(actualVal)) {
                        result = "OPTIONAL NOT PROVIDED";
                    } else {
                        result = "OPTIONAL CRITERION NOT SATISFIED";
                    }
                } else {
                    result = "NOT ELIGIBLE";
                }

                String requirement = criterion.getOperator().name() + " " + criterion.getExpectedValue();
                
                eligibilityDetails.add(new com.infosys.subsidy.dto.EligibilityScoreDetailsDTO(criterion.getCriterionName(), actualVal, requirement, result, maxPoints, points, !criterion.isMandatory()));
            }
        }
        dto.setEligibilityDetails(eligibilityDetails);

        return dto;
    }

    // =====================================================
    // GET VERIFICATION QUEUE
    // =====================================================

    @Transactional(readOnly = true)
    public List<OfficerQueueItemDTO> getVerificationQueue(String email) {
        User officer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated officer not found"));

        if (!officer.isActive()) {
            throw new RuntimeException("Officer account is deactivated");
        }

        VerificationLevel targetLevel = getVerificationLevelFromRole(officer.getRole());
        
        ApplicationStatus expectedStatus = (targetLevel == VerificationLevel.LEVEL_3) 
                ? ApplicationStatus.ESCALATED 
                : ApplicationStatus.PENDING_VERIFICATION;
        
        List<Application> pendingApplications = applicationRepository.findByCurrentVerificationLevelAndStatus(
                targetLevel, 
                expectedStatus
        );

        return pendingApplications.stream().map(app -> {
            String beneficiaryName = beneficiaryRepository.findById(app.getBeneficiaryId())
                    .map(b -> b.getName())
                    .orElse("Unknown Beneficiary");
            
            String schemeName = schemeRepository.findById(app.getSchemeId())
                    .map(s -> s.getSchemeName())
                    .orElse("Unknown Scheme");

            return new OfficerQueueItemDTO(
                    app.getId(),
                    app.getApplicationDate(),
                    app.getStatus(),
                    app.getCurrentVerificationLevel(),
                    app.getVerificationRoute(),
                    app.getEligibilityScore(),
                    beneficiaryName,
                    schemeName,
                    app.getVerificationAssignedAt(),
                    app.getVerificationDueDate(),
                    app.getRemarks()
            );
        }).collect(Collectors.toList());
    }

    // =====================================================
    // LEVEL 2 ELIGIBILITY VERIFICATION
    // =====================================================
    
    @Transactional
    public Application verifyEligibility(Long applicationId, com.infosys.subsidy.dto.EligibilityVerificationRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User officer = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        
        if (!officer.isActive() || officer.getRole() != UserRole.LEVEL_2_OFFICER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized for Level 2 verification");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found with ID: " + applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING_VERIFICATION || application.getCurrentVerificationLevel() != VerificationLevel.LEVEL_2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not ready for Level 2 eligibility verification");
        }
        
        VerificationLevel currentLevel = application.getCurrentVerificationLevel();
        VerificationLevel nextLevel = verificationRoutingService.getNextLevel(currentLevel, application.getVerificationRoute());

        VerificationHistory history = new VerificationHistory();
        history.setApplicationId(application.getId());
        history.setOfficerId(officer.getId());
        history.setOfficerName(officer.getName());
        history.setOfficerRole(officer.getRole().name());
        history.setVerificationLevel(currentLevel.name());
        history.setActionTimestamp(LocalDateTime.now());
        
        if ("VERIFIED".equalsIgnoreCase(request.getAction())) {
            history.setAction("ELIGIBILITY_VERIFIED");
            history.setRemarks(request.getRemarks() != null ? request.getRemarks() : "Eligibility verified");
            
            if (nextLevel == null || currentLevel == VerificationLevel.FINAL_APPROVAL) { 
                application.setStatus(ApplicationStatus.APPROVED);
                application.setCurrentVerificationLevel(null);
            } else {
                application.setCurrentVerificationLevel(nextLevel);
                application.setStatus(ApplicationStatus.PENDING_VERIFICATION);
                LocalDateTime assignedAt = LocalDateTime.now();
                application.setVerificationAssignedAt(assignedAt);
                application.setVerificationDueDate(assignedAt.plusDays(3));
            }
        } else {
            history.setAction("REJECTED");
            history.setRemarks("Eligibility Rejected: " + request.getRemarks());
            application.setStatus(ApplicationStatus.REJECTED);
            application.setCurrentVerificationLevel(null);
        }
        
        verificationHistoryRepository.save(history);
        return applicationRepository.save(application);
    }

    // =====================================================
    // RETURN TO APPLICANT (LEVEL 2)
    // =====================================================

    @Transactional
    public Application returnToApplicant(Long applicationId, VerificationRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User officer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        if (!officer.isActive() || officer.getRole() != UserRole.LEVEL_2_OFFICER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized. Only Level 2 officers can return applications to the applicant.");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found with ID: " + applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING_VERIFICATION || application.getCurrentVerificationLevel() != VerificationLevel.LEVEL_2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not ready for Level 2 verification");
        }

        // Add history
        VerificationHistory history = new VerificationHistory();
        history.setApplicationId(application.getId());
        history.setOfficerId(officer.getId());
        history.setOfficerName(officer.getName());
        history.setOfficerRole(officer.getRole().name());
        history.setVerificationLevel(VerificationLevel.LEVEL_2.name());
        history.setAction("RETURNED_TO_APPLICANT");
        history.setRemarks(request.getRemarks() != null ? request.getRemarks() : "Application returned for additional documents");
        history.setActionTimestamp(LocalDateTime.now());
        verificationHistoryRepository.save(history);

        // Update application
        application.setStatus(ApplicationStatus.RETURNED_TO_APPLICANT);
        // Leave currentVerificationLevel intact or null? 
        // According to instructions: "Remove it from the Level 2 active verification queue."
        // Our queue fetches where status is PENDING_VERIFICATION.
        
        return applicationRepository.save(application);
    }

    private VerificationLevel getVerificationLevelFromRole(UserRole role) {
        switch (role) {
            case LEVEL_1_OFFICER: return VerificationLevel.LEVEL_1;
            case LEVEL_2_OFFICER: return VerificationLevel.LEVEL_2;
            case LEVEL_3_OFFICER: return VerificationLevel.LEVEL_3;
            case FINAL_APPROVAL_OFFICER: return VerificationLevel.FINAL_APPROVAL;
            default:
                throw new RuntimeException("Unauthorized role for verification queue: " + role);
        }
    }
}