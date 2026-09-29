package com.infosys.subsidy.service;

import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.DisbursementMilestone;
import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.MilestoneStatus;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.DisbursementMilestoneRepository;
import com.infosys.subsidy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MilestoneJobService {

    private final DisbursementMilestoneRepository milestoneRepository;
    private final ApplicationRepository applicationRepository;
    private final GrantService grantService;
    private final UserRepository userRepository;

    @Value("${milestone.grace-period.days:-7}")
    private int gracePeriodDays;

    public MilestoneJobService(DisbursementMilestoneRepository milestoneRepository,
                               ApplicationRepository applicationRepository,
                               GrantService grantService,
                               UserRepository userRepository) {
        this.milestoneRepository = milestoneRepository;
        this.applicationRepository = applicationRepository;
        this.grantService = grantService;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 1 * * ?") // Runs daily at 1 AM
    @Transactional
    public void processOverdueMilestones() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Detect OVERDUE
        List<DisbursementMilestone> pendingOrDue = milestoneRepository.findByStatusAndDueDateBefore(MilestoneStatus.PENDING, now);
        pendingOrDue.addAll(milestoneRepository.findByStatusAndDueDateBefore(MilestoneStatus.DUE, now));
        
        for (DisbursementMilestone m : pendingOrDue) {
            m.setStatus(MilestoneStatus.OVERDUE);
            m.setRemarks("Automatically marked as OVERDUE on " + now);
            milestoneRepository.save(m);
            
            Application app = applicationRepository.findById(m.getPlan().getApplicationId()).orElse(null);
            if (app != null && app.getStatus() != ApplicationStatus.FULLY_DISBURSED && app.getStatus() != ApplicationStatus.NON_COMPLIANT) {
                app.setStatus(ApplicationStatus.MILESTONE_OVERDUE);
                applicationRepository.save(app);
            }
        }

        // 2. Detect NON_COMPLIANT (after grace period)
        LocalDateTime graceThreshold = now.minusDays(gracePeriodDays < 0 ? Math.abs(gracePeriodDays) : gracePeriodDays);
        List<DisbursementMilestone> overdue = milestoneRepository.findByStatusAndDueDateBefore(MilestoneStatus.OVERDUE, graceThreshold);

        for (DisbursementMilestone m : overdue) {
            m.setStatus(MilestoneStatus.NON_COMPLIANT);
            m.setRemarks("Automatically marked as NON_COMPLIANT after exceeding grace period.");
            milestoneRepository.save(m);

            Application app = applicationRepository.findById(m.getPlan().getApplicationId()).orElse(null);
            if (app != null) {
                app.setStatus(ApplicationStatus.NON_COMPLIANT);
                applicationRepository.save(app);
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Runs daily at 2 AM
    public void executeAutomaticReleases() {
        // Run as the first available ADMIN for logging
        User operator = userRepository.findAll().stream()
            .filter(u -> u.getRole() == UserRole.ADMIN)
            .findFirst()
            .orElse(null);
            
        if (operator == null) {
            System.err.println("Cannot run executeAutomaticReleases: No ADMIN user found to act as operator.");
            return; 
        }

        LocalDateTime now = LocalDateTime.now();
        List<DisbursementMilestone> candidates = milestoneRepository.findByStatusAndDueDateBefore(MilestoneStatus.PENDING, now);
        candidates.addAll(milestoneRepository.findByStatusAndDueDateBefore(MilestoneStatus.DUE, now));
        
        for (DisbursementMilestone m : candidates) {
            try {
                // All verification, sequence, budget, and duplication checks are strictly enforced in GrantService natively.
                grantService.releaseMilestone(m.getPlan().getApplicationId(), m.getId(), operator.getEmail());
            } catch (Exception e) {
                // A single failure isolates itself and does not crash the entire scheduled cron block
                System.err.println("Auto-Release Skipped for Milestone " + m.getId() + " - " + e.getMessage());
            }
        }
    }
}
