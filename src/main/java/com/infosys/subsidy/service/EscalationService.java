package com.infosys.subsidy.service;

import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.VerificationLevel;
import com.infosys.subsidy.enums.VerificationRoute;
import com.infosys.subsidy.repository.ApplicationRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EscalationService {

    private final ApplicationRepository applicationRepository;
    private final VerificationRoutingService verificationRoutingService;


    public EscalationService(
            ApplicationRepository applicationRepository,
            VerificationRoutingService verificationRoutingService) {

        this.applicationRepository = applicationRepository;
        this.verificationRoutingService =
                verificationRoutingService;
    }


    // =====================================================
    // AUTOMATIC ESCALATION CHECK
    // =====================================================

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkOverdueApplications() {

        LocalDateTime now = LocalDateTime.now();

        List<Application> applications =
                applicationRepository
                        .findByStatusAndVerificationDueDateBefore(
                                ApplicationStatus.PENDING_VERIFICATION,
                                now
                        );

        for (Application application : applications) {

            escalateApplication(application);
        }
    }


    // =====================================================
    // ESCALATE APPLICATION
    // =====================================================

    private void escalateApplication(
            Application application) {

        VerificationLevel currentLevel =
                application.getCurrentVerificationLevel();

        VerificationRoute verificationRoute =
                application.getVerificationRoute();


        // ==========================================
        // SAFETY CHECK
        // ==========================================

        if (currentLevel == null) {

            return;
        }

        if (verificationRoute == null) {

            application.setStatus(
                    ApplicationStatus.ESCALATED
            );

            application.setRemarks(
                    "Application has no verification route. "
                            + "Requires administrator attention."
            );

            application.setVerificationDueDate(null);

            applicationRepository.save(application);

            return;
        }


        // ==========================================
        // GET NEXT LEVEL FROM CENTRAL ROUTING SERVICE
        // ==========================================

        VerificationLevel nextLevel =
                verificationRoutingService.getNextLevel(
                        currentLevel,
                        verificationRoute
                );


        // ==========================================
        // NO NEXT LEVEL AVAILABLE
        // ESCALATE FOR ADMIN ATTENTION
        // ==========================================

        if (nextLevel == null) {

            application.setStatus(
                    ApplicationStatus.ESCALATED
            );

            application.setRemarks(
                    "Application overdue at "
                            + currentLevel
                            + ". Requires administrator attention."
            );

            application.setVerificationDueDate(null);

            applicationRepository.save(application);



            return;
        }


        // ==========================================
        // ESCALATE TO NEXT LEVEL
        // ==========================================

        LocalDateTime now =
                LocalDateTime.now();

        application.setStatus(
                ApplicationStatus.PENDING_VERIFICATION
        );

        application.setCurrentVerificationLevel(
                nextLevel
        );

        application.setRemarks(
                "Application automatically escalated from "
                        + currentLevel
                        + " to "
                        + nextLevel
                        + " because verification deadline expired."
        );

        application.setVerificationAssignedAt(
                now
        );

        application.setVerificationDueDate(
                now.plusDays(3)
        );

        applicationRepository.save(application);



    }
}