package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.PlanRequestDTO;
import com.infosys.subsidy.dto.PlanResponseDTO;
import com.infosys.subsidy.entity.DisbursementPlan;
import com.infosys.subsidy.entity.FundRelease;
import com.infosys.subsidy.entity.GrantDisbursement;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.FundReleaseRepository;
import com.infosys.subsidy.repository.GrantDisbursementRepository;
import com.infosys.subsidy.repository.SchemeRepository;
import com.infosys.subsidy.service.GrantService;
import com.infosys.subsidy.service.MilestoneComplianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/grants")
public class GrantController {

    private final GrantService grantService;
    private final MilestoneComplianceService complianceService;
    private final ApplicationRepository applicationRepository;
    private final GrantDisbursementRepository disbursementRepository;
    private final SchemeRepository schemeRepository;
    private final FundReleaseRepository fundReleaseRepository;
    private final com.infosys.subsidy.service.ApplicationDocumentService documentService;

    public GrantController(GrantService grantService, 
                           MilestoneComplianceService complianceService,
                           ApplicationRepository applicationRepository, 
                           GrantDisbursementRepository disbursementRepository,
                           SchemeRepository schemeRepository,
                           FundReleaseRepository fundReleaseRepository,
                           com.infosys.subsidy.service.ApplicationDocumentService documentService) {
        this.grantService = grantService;
        this.complianceService = complianceService;
        this.applicationRepository = applicationRepository;
        this.disbursementRepository = disbursementRepository;
        this.schemeRepository = schemeRepository;
        this.fundReleaseRepository = fundReleaseRepository;
        this.documentService = documentService;
    }

    // LEGACY ENDPOINTS (kept for system compatibility)
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<List<com.infosys.subsidy.dto.OfficerQueueItemDTO>> getPendingGrants() {
        return ResponseEntity.ok(grantService.getPendingGrants());
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<com.infosys.subsidy.dto.ApplicationDetailDTO> getGrantApplication(@PathVariable Long id) {
        return ResponseEntity.ok(grantService.getGrantApplicationDetails(id));
    }

    @PostMapping("/applications/{id}/disburse")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<GrantDisbursement> disburseGrant(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(grantService.disburseGrant(id, principal.getName()));
    }

    @GetMapping("/disbursements")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllDisbursements() {
        List<Map<String, Object>> unifiedLedger = new ArrayList<>();
        
        disbursementRepository.findAll().forEach(gd -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", "gd-" + gd.getId());
            map.put("transactionReference", gd.getTransactionReference());
            map.put("applicationId", gd.getApplicationId());
            map.put("grantAmount", gd.getGrantAmount());
            map.put("disbursedAt", gd.getDisbursedAt());
            unifiedLedger.add(map);
        });

        grantService.getAllFundReleases().forEach(fr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", "fr-" + fr.getId());
            map.put("transactionReference", fr.getTransactionReference());
            map.put("applicationId", fr.getApplicationId());
            map.put("grantAmount", fr.getAmount());
            map.put("disbursedAt", fr.getReleasedAt());
            unifiedLedger.add(map);
        });

        unifiedLedger.sort((a, b) -> ((java.time.LocalDateTime) b.get("disbursedAt")).compareTo((java.time.LocalDateTime) a.get("disbursedAt")));
        return ResponseEntity.ok(unifiedLedger);
    }

    @GetMapping("/analytics/regions")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<List<com.infosys.subsidy.dto.RegionalAnalyticsDTO>> getRegionalAnalytics() {
        return ResponseEntity.ok(grantService.getRegionalAnalytics());
    }

    @GetMapping("/schemes/{id}/summary")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getSchemeFinanceSummary(@PathVariable Long id) {
        return schemeRepository.findById(id).map(scheme -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("schemeId", scheme.getId());
            summary.put("schemeName", scheme.getSchemeName());
            summary.put("totalBudget", scheme.getTotalBudget());
            summary.put("disbursedAmount", scheme.getDisbursedAmount());
            summary.put("remainingBudget", scheme.getRemainingBudget());
            summary.put("grantAmount", scheme.getGrantAmount());
            return ResponseEntity.ok(summary);
        }).orElse(ResponseEntity.notFound().build());
    }
    
    // ==========================================
    // NEW STAGED DISBURSEMENT ENDPOINTS
    // ==========================================

    @PostMapping("/applications/{id}/plan")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<DisbursementPlan> createPlan(@PathVariable Long id, @RequestBody PlanRequestDTO request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(grantService.createDisbursementPlan(id, request, principal.getName()));
    }

    @GetMapping("/applications/{id}/plan")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN', 'BENEFICIARY')")
    public ResponseEntity<PlanResponseDTO> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(grantService.getDisbursementPlan(id));
    }

    @DeleteMapping("/applications/{id}/nuke-plan")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<String> nukePlan(@PathVariable Long id) {
        grantService.nukePlanAndResetApplication(id);
        return ResponseEntity.ok("Plan erased for APP-" + id + ". Please refresh the page to configure a new one.");
    }
    
    @GetMapping("/compliance/non-compliant")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getNonCompliantCompliance() {
        return ResponseEntity.ok(grantService.getNonCompliantMilestones());
    }

    @GetMapping("/compliance/pending")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getPendingCompliance() {
        return ResponseEntity.ok(grantService.getPendingComplianceMilestones());
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<List<com.infosys.subsidy.dto.PlanSummaryDTO>> getAllDisbursementPlans() {
        return ResponseEntity.ok(grantService.getAllDisbursementPlans());
    }

    @PostMapping("/milestones/{id}/compliance")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN', 'BENEFICIARY')")
    public ResponseEntity<?> submitCompliance(@PathVariable Long id, @RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String remarks = request.get("remarks");
        return ResponseEntity.ok(complianceService.submitComplianceEvidence(id, remarks, principal.getName()));
    }

    @PostMapping(value = "/milestones/{id}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN', 'BENEFICIARY')")
    public ResponseEntity<?> uploadComplianceEvidence(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("evidenceTypes") List<String> evidenceTypes,
            @RequestParam(value = "remarks", required = false) String remarks,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(complianceService.submitComplianceEvidenceFiles(id, files, evidenceTypes, remarks, principal.getName()));
    }

    @PostMapping("/milestones/{id}/compliance/verify")
    @PreAuthorize("hasAnyRole('LEVEL_1_OFFICER', 'LEVEL_2_OFFICER', 'ADMIN')")
    public ResponseEntity<?> verifyCompliance(@PathVariable Long id, @RequestBody Map<String, Object> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        boolean approved = Boolean.TRUE.equals(request.get("approved"));
        String remarks = (String) request.get("remarks");
        return ResponseEntity.ok(complianceService.verifyCompliance(id, approved, remarks, principal.getName()));
    }

    @GetMapping("/milestones/{id}/evidence")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN', 'BENEFICIARY')")
    public ResponseEntity<?> getEvidence(@PathVariable Long id) {
        return ResponseEntity.ok(complianceService.getEvidenceForMilestone(id));
    }
    
    @PostMapping("/milestones/{id}/evidence/{evidenceId}/review")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<?> reviewEvidence(
            @PathVariable Long id,
            @PathVariable Long evidenceId,
            @RequestBody Map<String, String> request,
            Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String action = request.get("action");
        String remarks = request.get("remarks");
        return ResponseEntity.ok(complianceService.reviewEvidence(id, evidenceId, action, remarks, principal.getName()));
    }
    
    @PutMapping("/documents/{applicationId}/{documentId}/verify-passbook")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<?> verifyPassbook(
            @PathVariable Long applicationId,
            @PathVariable Long documentId,
            @RequestBody com.infosys.subsidy.dto.DocumentVerificationRequest request,
            Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(documentService.verifyMilestonePassbook(applicationId, documentId, request));
    }

    @GetMapping("/evidence/{id}/download")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN', 'BENEFICIARY')")
    public ResponseEntity<org.springframework.core.io.Resource> downloadEvidence(@PathVariable Long id) {
        com.infosys.subsidy.entity.MilestoneComplianceEvidence evidence = complianceService.getEvidenceById(id);
        try {
            java.nio.file.Path file = java.nio.file.Paths.get(evidence.getFilePath());
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = java.nio.file.Files.probeContentType(file);
                if (contentType == null) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + evidence.getOriginalFileName() + "\"")
                        .body(resource);
            } else {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Could not read file");
            }
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download file");
        }
    }


    // ============================================================
    // GET UNIQUE RECEIPT FOR SUCCESSFUL FUND RELEASE
    // ============================================================
    @GetMapping("/milestones/{id}/receipt")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN', 'BENEFICIARY')")
    public ResponseEntity<com.infosys.subsidy.entity.FundRelease> getMilestoneReceipt(@PathVariable Long id) {
        com.infosys.subsidy.entity.FundRelease receipt = fundReleaseRepository.findAll().stream()
                .filter(fr -> id.equals(fr.getMilestoneId()) && 
                       ("RELEASED".equals(fr.getStatus()) || "DISBURSED".equals(fr.getStatus()) || "SUCCESS".equals(fr.getStatus())))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "No successful receipt found for this milestone."));
        return ResponseEntity.ok(receipt);
    }

    @PostMapping("/milestones/{id}/release")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<FundRelease> releaseMilestone(@PathVariable Long id, @RequestBody Map<String, Long> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        Long applicationId = request.get("applicationId");
        return ResponseEntity.ok(grantService.releaseMilestone(applicationId, id, principal.getName()));
    }

    @GetMapping("/applications/{id}/releases")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN', 'BENEFICIARY')")
    public ResponseEntity<List<FundRelease>> getApplicationReleases(@PathVariable Long id) {
        return ResponseEntity.ok(fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(id));
    }
}
