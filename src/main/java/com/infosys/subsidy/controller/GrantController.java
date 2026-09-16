package com.infosys.subsidy.controller;

import com.infosys.subsidy.entity.GrantDisbursement;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.GrantDisbursementRepository;
import com.infosys.subsidy.repository.SchemeRepository;
import com.infosys.subsidy.service.GrantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grants")
public class GrantController {

    private final GrantService grantService;
    private final ApplicationRepository applicationRepository;
    private final GrantDisbursementRepository disbursementRepository;
    private final SchemeRepository schemeRepository;

    public GrantController(GrantService grantService, 
                           ApplicationRepository applicationRepository, 
                           GrantDisbursementRepository disbursementRepository,
                           SchemeRepository schemeRepository) {
        this.grantService = grantService;
        this.applicationRepository = applicationRepository;
        this.disbursementRepository = disbursementRepository;
        this.schemeRepository = schemeRepository;
    }

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
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(grantService.disburseGrant(id, principal.getName()));
    }

    @GetMapping("/disbursements")
    @PreAuthorize("hasAnyRole('GRANT_OFFICER', 'ADMIN')")
    public ResponseEntity<List<GrantDisbursement>> getAllDisbursements() {
        return ResponseEntity.ok(disbursementRepository.findAllByOrderByDisbursedAtDesc());
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
}
