package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.VerificationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.ApplicationDocument;
import com.infosys.subsidy.service.VerificationService;
import com.infosys.subsidy.service.ApplicationDocumentService;
import com.infosys.subsidy.dto.DocumentVerificationRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verifications")
@CrossOrigin(origins = "http://localhost:5173")
public class VerificationController {

    private final VerificationService verificationService;
    private final ApplicationDocumentService applicationDocumentService;

    public VerificationController(
            VerificationService verificationService,
            ApplicationDocumentService applicationDocumentService) {

        this.verificationService = verificationService;
        this.applicationDocumentService = applicationDocumentService;
    }


    // ==========================================
    // VERIFY APPLICATION
    // ==========================================

    @PutMapping("/{applicationId}")
    public ResponseEntity<Application> verifyApplication(

            @PathVariable Long applicationId,

            @RequestBody VerificationRequest request) {

        Application application =
                verificationService.verifyApplication(
                        applicationId,
                        request
                );

        return ResponseEntity.ok(application);
    }

    // ==========================================
    // GET VERIFICATION QUEUE
    // =====================================================

    @GetMapping("/queue")
    public ResponseEntity<java.util.List<com.infosys.subsidy.dto.OfficerQueueItemDTO>> getVerificationQueue(
            org.springframework.security.core.Authentication authentication) {

        String email = authentication.getName();
        java.util.List<com.infosys.subsidy.dto.OfficerQueueItemDTO> queue =
                verificationService.getVerificationQueue(email);

        return ResponseEntity.ok(queue);
    }

    // ==========================================
    // GET FULL APPLICATION DETAILS FOR REVIEW
    // ==========================================

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<com.infosys.subsidy.dto.ApplicationDetailDTO> getApplicationDetailsForReview(
            @PathVariable Long applicationId,
            org.springframework.security.core.Authentication authentication) {

        String email = authentication.getName();
        com.infosys.subsidy.dto.ApplicationDetailDTO details = 
                verificationService.getApplicationDetailsForReview(applicationId, email);

        return ResponseEntity.ok(details);
    }
    
    // ==========================================
    // VERIFY INDIVIDUAL DOCUMENT
    // ==========================================
    
    @PutMapping("/applications/{applicationId}/documents/{documentId}")
    public ResponseEntity<ApplicationDocument> verifyDocument(
            @PathVariable Long applicationId,
            @PathVariable Long documentId,
            @RequestBody DocumentVerificationRequest request) {
            
        ApplicationDocument result = applicationDocumentService.verifyDocument(applicationId, documentId, request);
        return ResponseEntity.ok(result);
    }
}