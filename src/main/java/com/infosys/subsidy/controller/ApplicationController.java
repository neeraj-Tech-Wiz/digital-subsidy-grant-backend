package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.ApplicationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.service.ApplicationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "http://localhost:5173")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }


    // ============================================================
    // BENEFICIARY APPLIES FOR A SCHEME
    //
    // OLD: POST /api/applications/apply/{beneficiaryId}/{schemeId}
    // NEW: POST /api/applications/apply/{schemeId}
    //
    // The frontend must NOT send beneficiaryId.
    // The backend derives the beneficiary from the JWT.
    // ============================================================

    @PostMapping("/apply/{schemeId}")
    public ResponseEntity<Application> applyForScheme(

            @PathVariable Long schemeId,

            @RequestBody ApplicationRequest request,

            Authentication authentication) {

        String email = authentication.getName();

        Application application =
                applicationService.applyForScheme(
                        schemeId,
                        request,
                        email
                );

        return ResponseEntity.ok(application);
    }


    // ============================================================
    // SUBMIT APPLICATION DOCUMENTS
    //
    // Ownership verified server-side from JWT.
    // ============================================================

    @PostMapping("/submit-documents/{applicationId}")
    public ResponseEntity<Application> submitApplicationDocuments(
            @PathVariable Long applicationId,
            Authentication authentication) {

        String email = authentication.getName();
        Application application =
                applicationService.submitApplicationDocuments(applicationId, email);
        return ResponseEntity.ok(application);
    }

    // ============================================================
    // GET ELIGIBILITY ANSWERS
    // ============================================================

    @GetMapping("/{applicationId}/answers")
    public ResponseEntity<java.util.Map<String, String>> getApplicationAnswers(
            @PathVariable Long applicationId,
            Authentication authentication) {

        String email = authentication.getName();
        java.util.Map<String, String> answers =
                applicationService.getApplicationAnswers(applicationId, email);
        return ResponseEntity.ok(answers);
    }

    // ============================================================
    // RESUBMIT APPLICATION (UPDATE ELIGIBILITY, RECALCULATE ROUTE)
    // ============================================================

    @PutMapping("/{applicationId}/resubmit")
    public ResponseEntity<Application> resubmitApplication(
            @PathVariable Long applicationId,
            @RequestBody ApplicationRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        Application application =
                applicationService.resubmitApplication(applicationId, request, email);
        return ResponseEntity.ok(application);
    }

    // ============================================================
    // GET MY APPLICATIONS
    //
    // Retrieves all applications for the authenticated beneficiary.
    // ============================================================

    @GetMapping("/my-applications")
    public ResponseEntity<List<Application>> getMyApplications(Authentication authentication) {
        String email = authentication.getName();
        List<Application> applications = applicationService.getMyApplications(email);
        return ResponseEntity.ok(applications);
    }

    // ============================================================
    // GET APPLICATION VERIFICATION HISTORY
    // ============================================================

    @GetMapping("/{applicationId}/history")
    public ResponseEntity<List<com.infosys.subsidy.dto.VerificationHistoryDTO>> getApplicationHistory(
            @PathVariable Long applicationId,
            Authentication authentication) {
        String email = authentication.getName();
        List<com.infosys.subsidy.dto.VerificationHistoryDTO> history = applicationService.getApplicationHistory(applicationId, email);
        return ResponseEntity.ok(history);
    }
    
    // ============================================================
    // GET APPLICATION DISBURSEMENT RECORD
    // ============================================================

    @GetMapping("/{applicationId}/grant")
    public ResponseEntity<com.infosys.subsidy.entity.GrantDisbursement> getApplicationGrantDisbursement(
            @PathVariable Long applicationId,
            Authentication authentication) {
        String email = authentication.getName();
        com.infosys.subsidy.entity.GrantDisbursement grant = applicationService.getApplicationGrant(applicationId, email);
        return ResponseEntity.ok(grant);
    }

    // ============================================================
    // GET MY GRANTS (ALL DISBURSED TO ME)
    // ============================================================

    @GetMapping("/my-applications/grants")
    public ResponseEntity<List<com.infosys.subsidy.entity.GrantDisbursement>> getMyGrants(
            Authentication authentication) {
        String email = authentication.getName();
        List<com.infosys.subsidy.entity.GrantDisbursement> grants = applicationService.getMyGrants(email);
        return ResponseEntity.ok(grants);
    }
}