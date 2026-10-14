package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.AuditResponseDTO;
import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.MilestoneStatus;
import com.infosys.subsidy.enums.SchemeStatus;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import com.infosys.subsidy.repository.DisbursementMilestoneRepository;
import com.infosys.subsidy.repository.GrantDisbursementRepository;
import com.infosys.subsidy.repository.SchemeRepository;
import com.infosys.subsidy.service.AdminAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final ApplicationRepository applicationRepository;
    private final GrantDisbursementRepository grantDisbursementRepository;
    private final DisbursementMilestoneRepository milestoneRepository;
    private final AdminAuditService adminAuditService;
    private final DataSource dataSource;

    public AdminDashboardController(BeneficiaryRepository beneficiaryRepository,
                                    SchemeRepository schemeRepository,
                                    ApplicationRepository applicationRepository,
                                    GrantDisbursementRepository grantDisbursementRepository,
                                    DisbursementMilestoneRepository milestoneRepository,
                                    AdminAuditService adminAuditService,
                                    DataSource dataSource) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.applicationRepository = applicationRepository;
        this.grantDisbursementRepository = grantDisbursementRepository;
        this.milestoneRepository = milestoneRepository;
        this.adminAuditService = adminAuditService;
        this.dataSource = dataSource;
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/dashboard/stats
    //
    // Cached for 60 seconds (Caffeine). Preserves all existing field names,
    // data types, business rules, and response structure exactly as before.
    // -------------------------------------------------------------------------
    @GetMapping("/stats")
    @Cacheable("dashboardStats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        long t0 = System.currentTimeMillis();
        Map<String, Object> stats = new HashMap<>();

        // ---- 0. Connection Acquisition Timing -------------------------------
        long t_connStart = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            // Pool connection acquisition check
        } catch (Exception e) {
            log.warn("[STATS-TIMING] Connection check warning: {}", e.getMessage());
        }
        long connAcquisitionMs = System.currentTimeMillis() - t_connStart;
        log.info("[STATS-TIMING] connection_acquisition={}ms", connAcquisitionMs);

        // ---- 1. KPIs --------------------------------------------------------
        long t_kpi = System.currentTimeMillis();

        long t_benStart = System.currentTimeMillis();
        stats.put("totalBeneficiaries", beneficiaryRepository.count());
        long t_beneficiaries = System.currentTimeMillis() - t_benStart;

        long t_schemeStart = System.currentTimeMillis();
        List<Scheme> allSchemes = schemeRepository.findAll();
        long activeSchemesCount = allSchemes.stream()
                .filter(s -> s.getStatus() == SchemeStatus.ACTIVE)
                .count();
        stats.put("activeSchemes", activeSchemesCount);
        long t_schemes = System.currentTimeMillis() - t_schemeStart;

        long t_appStart = System.currentTimeMillis();
        List<ApplicationStatus> pendingStatuses = Arrays.asList(
                ApplicationStatus.SUBMITTED,
                ApplicationStatus.PENDING_VERIFICATION,
                ApplicationStatus.UNDER_VERIFICATION,
                ApplicationStatus.ESCALATED
        );
        long pendingApps = applicationRepository.countByStatusIn(pendingStatuses);
        stats.put("pendingApplications", pendingApps);

        long approvedApps = applicationRepository.countByStatus(ApplicationStatus.APPROVED);
        stats.put("approvedApplications", approvedApps);
        long t_appAggregates = System.currentTimeMillis() - t_appStart;

        long t_grantStart = System.currentTimeMillis();
        BigDecimal totalFundsReleased = grantDisbursementRepository.sumGrantAmount();
        stats.put("totalFundsReleased", totalFundsReleased);
        long t_grantTotals = System.currentTimeMillis() - t_grantStart;

        long t_auditTodayStart = System.currentTimeMillis();
        String todayString = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        long activityToday = adminAuditService.countAuditLogs(
                null, null, null, null, null, todayString, todayString);
        stats.put("activityToday", activityToday);
        long t_auditToday = System.currentTimeMillis() - t_auditTodayStart;

        log.info("[STATS-TIMING] kpis_total={}ms (beneficiaries={}ms, schemes={}ms, apps={}ms, grants={}ms, audit_today={}ms)",
                System.currentTimeMillis() - t_kpi, t_beneficiaries, t_schemes, t_appAggregates, t_grantTotals, t_auditToday);

        // ---- 2. Attention Required ------------------------------------------
        long t_attention = System.currentTimeMillis();

        Map<String, Long> attention = new HashMap<>();
        attention.put("verificationPending", pendingApps);

        long escalatedCount = applicationRepository.countByStatus(ApplicationStatus.ESCALATED);
        attention.put("finalApprovalPending", escalatedCount);

        long t_milestoneStart = System.currentTimeMillis();
        attention.put("nonCompliantMilestones",
                milestoneRepository.countByStatus(MilestoneStatus.NON_COMPLIANT));
        attention.put("complianceReviewsPending",
                milestoneRepository.countByStatus(MilestoneStatus.UNDER_REVIEW));
        long t_milestones = System.currentTimeMillis() - t_milestoneStart;

        long t_secStart = System.currentTimeMillis();
        long failedLogins = adminAuditService.countAuditLogs(
                "LOGIN_FAILED", null, null, null, false, todayString, todayString);
        attention.put("failedSecurityEvents", failedLogins);
        long t_secEvents = System.currentTimeMillis() - t_secStart;

        stats.put("attentionRequired", attention);
        log.info("[STATS-TIMING] attention_total={}ms (milestones={}ms, security_audit={}ms)",
                System.currentTimeMillis() - t_attention, t_milestones, t_secEvents);

        // ---- 3. Recent Activity (latest 8 events) ---------------------------
        long t_recentStart = System.currentTimeMillis();
        List<AuditResponseDTO> recentActivity = adminAuditService.getRecentActivityLogs(8);
        stats.put("recentActivity", recentActivity);
        long t_recent = System.currentTimeMillis() - t_recentStart;
        log.info("[STATS-TIMING] recent_activity={}ms", t_recent);

        // ---- 4. Analytics ---------------------------------------------------
        long t_analytics = System.currentTimeMillis();

        // A. Application status distribution — single GROUP BY query (zero entity loading)
        long t_statusDistStart = System.currentTimeMillis();
        List<Object[]> statusCounts = applicationRepository.countGroupedByStatus();
        Map<String, Long> appStatusDistribution = new HashMap<>();
        for (Object[] row : statusCounts) {
            if (row[0] != null) {
                String statusName = row[0] instanceof ApplicationStatus
                        ? ((ApplicationStatus) row[0]).name()
                        : row[0].toString();
                Long count = ((Number) row[1]).longValue();
                appStatusDistribution.put(statusName, count);
            }
        }
        stats.put("applicationStatusDistribution", appStatusDistribution);
        long t_statusDist = System.currentTimeMillis() - t_statusDistStart;

        // B. Applications by scheme — single GROUP BY query + scheme name map (no N+1)
        long t_schemeDistStart = System.currentTimeMillis();
        Map<Long, String> schemeIdToName = allSchemes.stream()
                .collect(Collectors.toMap(Scheme::getId, Scheme::getSchemeName));

        List<Object[]> groupedByScheme = applicationRepository.countGroupedBySchemeId();
        Map<String, Long> appsByScheme = new HashMap<>();
        for (Object[] row : groupedByScheme) {
            Long schemeId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            String sName = schemeIdToName.getOrDefault(schemeId, "Unknown");
            appsByScheme.put(sName, appsByScheme.getOrDefault(sName, 0L) + count);
        }
        stats.put("applicationsByScheme", appsByScheme);
        long t_schemeDist = System.currentTimeMillis() - t_schemeDistStart;

        // C. Fund flow — reuse allSchemes list (zero additional reads)
        long t_fundFlowStart = System.currentTimeMillis();
        BigDecimal allocated = allSchemes.stream()
                .map(Scheme::getTotalBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal released = allSchemes.stream()
                .map(Scheme::getDisbursedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> fundFlow = new HashMap<>();
        fundFlow.put("Allocated", allocated);
        fundFlow.put("Released", released);
        fundFlow.put("Planned", allocated.subtract(released).max(BigDecimal.ZERO));
        stats.put("fundFlow", fundFlow);
        long t_fundFlow = System.currentTimeMillis() - t_fundFlowStart;

        long t_analyticsTotal = System.currentTimeMillis() - t_analytics;
        long totalTime = System.currentTimeMillis() - t0;
        log.info("[STATS-TIMING] analytics_total={}ms (status_dist={}ms, scheme_dist={}ms, fund_flow={}ms) | TOTAL_EXECUTION={}ms",
                t_analyticsTotal, t_statusDist, t_schemeDist, t_fundFlow, totalTime);

        return ResponseEntity.ok(stats);
    }

    // -------------------------------------------------------------------------
    // Cache eviction endpoint (internal use / scheduled eviction).
    //
    // The 60-second TTL handles the common case automatically.
    // This endpoint provides explicit eviction for operational use.
    // Write-path eviction is handled by @CacheEvict annotations in the
    // service layer for application submissions, approvals, scheme changes,
    // disbursements, and milestone compliance updates.
    // -------------------------------------------------------------------------
    @DeleteMapping("/stats/cache")
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ResponseEntity<Void> evictStatsCache() {
        log.info("[CACHE-EVICT] dashboardStats cache manually evicted");
        return ResponseEntity.noContent().build();
    }
}
