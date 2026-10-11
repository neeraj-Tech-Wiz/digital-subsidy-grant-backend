package com.infosys.subsidy.controller;

import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.GrantDisbursement;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.MilestoneStatus;
import com.infosys.subsidy.enums.SchemeStatus;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import com.infosys.subsidy.repository.DisbursementMilestoneRepository;
import com.infosys.subsidy.repository.GrantDisbursementRepository;
import com.infosys.subsidy.repository.SchemeRepository;
import com.infosys.subsidy.service.AdminAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final ApplicationRepository applicationRepository;
    private final GrantDisbursementRepository grantDisbursementRepository;
    private final DisbursementMilestoneRepository milestoneRepository;
    private final AdminAuditService adminAuditService;

    public AdminDashboardController(BeneficiaryRepository beneficiaryRepository,
                                    SchemeRepository schemeRepository,
                                    ApplicationRepository applicationRepository,
                                    GrantDisbursementRepository grantDisbursementRepository,
                                    DisbursementMilestoneRepository milestoneRepository,
                                    AdminAuditService adminAuditService) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.applicationRepository = applicationRepository;
        this.grantDisbursementRepository = grantDisbursementRepository;
        this.milestoneRepository = milestoneRepository;
        this.adminAuditService = adminAuditService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 1. KPIs
        stats.put("totalBeneficiaries", beneficiaryRepository.count());
        stats.put("activeSchemes", schemeRepository.findByStatus(SchemeStatus.ACTIVE).size());
        
        List<Application> allApps = applicationRepository.findAll();
        
        long pendingApps = allApps.stream().filter(a -> Arrays.asList(
                ApplicationStatus.SUBMITTED,
                ApplicationStatus.PENDING_VERIFICATION,
                ApplicationStatus.UNDER_VERIFICATION,
                ApplicationStatus.ESCALATED
        ).contains(a.getStatus())).count();
        stats.put("pendingApplications", pendingApps);
        
        long approvedApps = allApps.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count();
        stats.put("approvedApplications", approvedApps);
        
        BigDecimal totalFundsReleased = grantDisbursementRepository.findAll().stream()
                .map(GrantDisbursement::getGrantAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalFundsReleased", totalFundsReleased);

        String todayString = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Map<String, Object> todaysAudit = adminAuditService.getUnifiedAuditLogs(null, null, null, null, null, todayString, todayString, 0, 1);
        stats.put("activityToday", todaysAudit.get("totalElements") != null ? todaysAudit.get("totalElements") : 0);

        // 2. Attention Required
        Map<String, Long> attention = new HashMap<>();
        attention.put("verificationPending", pendingApps);
        attention.put("finalApprovalPending", allApps.stream().filter(a -> a.getStatus() == ApplicationStatus.ESCALATED).count()); // approximating final approval queue
        attention.put("nonCompliantMilestones", (long) milestoneRepository.findAll().stream().filter(m -> m.getStatus() == MilestoneStatus.NON_COMPLIANT).count());
        attention.put("complianceReviewsPending", (long) milestoneRepository.findAll().stream().filter(m -> m.getStatus() == MilestoneStatus.UNDER_REVIEW).count());
        
        Map<String, Object> failedLogins = adminAuditService.getUnifiedAuditLogs("LOGIN_FAILED", null, null, null, false, todayString, todayString, 0, 1);
        attention.put("failedSecurityEvents", (Long) (failedLogins.get("totalElements") != null ? failedLogins.get("totalElements") : 0L));
        stats.put("attentionRequired", attention);

        // 3. Recent Activity (Latest 8 events)
        Map<String, Object> recentAuditLogs = adminAuditService.getUnifiedAuditLogs(null, null, null, null, null, null, null, 0, 8);
        stats.put("recentActivity", recentAuditLogs.get("content"));

        // 4. Analytics
        // A. App distribution
        Map<String, Long> appStatusDistribution = allApps.stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));
        stats.put("applicationStatusDistribution", appStatusDistribution);

        // B. Apps by Scheme
        Map<String, Long> appsByScheme = new HashMap<>();
        for (Application a : allApps) {
            String sName = schemeRepository.findById(a.getSchemeId())
                                           .map(Scheme::getSchemeName)
                                           .orElse("Unknown");
            appsByScheme.put(sName, appsByScheme.getOrDefault(sName, 0L) + 1);
        }
        stats.put("applicationsByScheme", appsByScheme);

        // C. Fund Flow
        List<Scheme> allSchemes = schemeRepository.findAll();
        BigDecimal allocated = allSchemes.stream().map(Scheme::getTotalBudget).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal released = allSchemes.stream().map(Scheme::getDisbursedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, BigDecimal> fundFlow = new HashMap<>();
        fundFlow.put("Allocated", allocated);
        fundFlow.put("Released", released);
        // Estimate planned by what's not released + allocated
        fundFlow.put("Planned", allocated.subtract(released).max(BigDecimal.ZERO));
        stats.put("fundFlow", fundFlow);

        return ResponseEntity.ok(stats);
    }
}
