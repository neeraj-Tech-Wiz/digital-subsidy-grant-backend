package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.VerificationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.service.VerificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verifications")
@CrossOrigin(origins = "http://localhost:5173")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(
            VerificationService verificationService) {

        this.verificationService = verificationService;
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
}