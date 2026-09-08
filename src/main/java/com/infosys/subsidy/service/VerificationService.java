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

import java.time.LocalDateTime;

@Service
public class VerificationService {

    private final ApplicationRepository applicationRepository;
    private final VerificationRoutingService verificationRoutingService;
    private final DocumentValidationService documentValidationService;
    private final UserRepository userRepository;

    public VerificationService(
            ApplicationRepository applicationRepository,
            VerificationRoutingService verificationRoutingService,
            DocumentValidationService documentValidationService,
            UserRepository userRepository) {

        this.applicationRepository = applicationRepository;
        this.verificationRoutingService = verificationRoutingService;
        this.documentValidationService = documentValidationService;
        this.userRepository = userRepository;
    }


    // =====================================================
    // VERIFY APPLICATION
    // =====================================================

    @Transactional
    public Application verifyApplication(
            Long applicationId,
            VerificationRequest request) {


        // ==========================================
        // 1. GET APPLICATION
        // ==========================================

        Application application =
                applicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found with ID: "
                                                + applicationId
                                )
                        );


        // ==========================================
        // 2. CHECK APPLICATION STATUS
        // ==========================================

        if (application.getStatus()
                == ApplicationStatus.NOT_ELIGIBLE) {

            throw new RuntimeException(
                    "Cannot verify a NOT_ELIGIBLE application"
            );
        }


        if (application.getStatus()
                == ApplicationStatus.APPROVED) {

            throw new RuntimeException(
                    "Application is already APPROVED"
            );
        }


        if (application.getStatus()
                == ApplicationStatus.REJECTED) {

            throw new RuntimeException(
                    "Application is already REJECTED"
            );
        }

        if (application.getStatus() != ApplicationStatus.PENDING_VERIFICATION) {
            throw new RuntimeException(
                    "Application is not currently pending verification"
            );
        }


        // ==========================================
        // 3. GET CURRENT VERIFICATION LEVEL
        // ==========================================

        VerificationLevel currentLevel =
                application.getCurrentVerificationLevel();


        if (currentLevel == null) {

            throw new RuntimeException(
                    "Application is not assigned to a verification level"
            );
        }

        // ==========================================
        // 3.5. APPLY ROLE ENFORCEMENT
        // ==========================================

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User officer = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        
        if (!officer.isActive()) {
            throw new RuntimeException("Officer account is deactivated");
        }

        switch (currentLevel) {
            case LEVEL_1: if (officer.getRole() != UserRole.LEVEL_1_OFFICER) throw new RuntimeException("Unauthorized for LEVEL_1"); break;
            case LEVEL_2: if (officer.getRole() != UserRole.LEVEL_2_OFFICER) throw new RuntimeException("Unauthorized for LEVEL_2"); break;
            case LEVEL_3: if (officer.getRole() != UserRole.LEVEL_3_OFFICER) throw new RuntimeException("Unauthorized for LEVEL_3"); break;
            case FINAL_APPROVAL: if (officer.getRole() != UserRole.FINAL_APPROVAL_OFFICER) throw new RuntimeException("Unauthorized for FINAL_APPROVAL"); break;
        }

        // ==========================================
        // 4. REJECT APPLICATION
        // ==========================================

        if (!Boolean.TRUE.equals(request.getApproved())) {

            application.setStatus(
                    ApplicationStatus.REJECTED
            );

            application.setRemarks(
                    request.getRemarks()
            );

            application.setCurrentVerificationLevel(
                    null
            );

            application.setVerificationCompletedAt(
                    LocalDateTime.now()
            );

            application.setVerificationDueDate(
                    null
            );

            return applicationRepository.save(application);
        }


        // ==========================================
        // 5. GET NEXT LEVEL
        // USING SAVED VERIFICATION ROUTE
        // ==========================================

        VerificationLevel nextLevel =
                verificationRoutingService.getNextLevel(
                        currentLevel,
                        application.getVerificationRoute()
                );


        // ==========================================
        // 6. SAVE CURRENT LEVEL REMARKS
        // ==========================================

        application.setRemarks(
                request.getRemarks()
        );


        // ==========================================
        // 7. FINAL APPROVAL
        // ==========================================

        if (currentLevel
                == VerificationLevel.FINAL_APPROVAL) {

            // Safety Check: Document Verification
            boolean allDocsVerified = documentValidationService.validateMandatoryDocumentsVerified(
                    application.getSchemeId(),
                    application.getId()
            );

            if (!allDocsVerified) {
                var unverifiedDocs = documentValidationService.getUnverifiedMandatoryDocuments(application.getSchemeId(), application.getId());
                throw new RuntimeException("Application cannot be finally approved. Mandatory documents are not fully verified: " + unverifiedDocs);
            }

            application.setStatus(
                    ApplicationStatus.APPROVED
            );

            application.setCurrentVerificationLevel(
                    null
            );

            application.setVerificationCompletedAt(
                    LocalDateTime.now()
            );

            application.setVerificationDueDate(
                    null
            );

            return applicationRepository.save(application);
        }


        // ==========================================
        // 8. MOVE TO NEXT LEVEL
        // ==========================================

        if (nextLevel != null) {

            LocalDateTime assignedAt =
                    LocalDateTime.now();

            application.setCurrentVerificationLevel(
                    nextLevel
            );

            application.setStatus(
                    ApplicationStatus.PENDING_VERIFICATION
            );

            application.setVerificationAssignedAt(
                    assignedAt
            );

            application.setVerificationDueDate(
                    assignedAt.plusDays(3)
            );




            return applicationRepository.save(application);
        }


        // ==========================================
        // 9. SAFETY CHECK
        // ==========================================

        throw new RuntimeException(
                "Unable to determine next verification level"
        );
    }
}