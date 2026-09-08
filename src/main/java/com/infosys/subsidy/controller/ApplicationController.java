package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.ApplicationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.service.ApplicationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "http://localhost:5173")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(
            ApplicationService applicationService) {

        this.applicationService = applicationService;
    }


    // =====================================================
    // BENEFICIARY APPLIES FOR A SCHEME
    // =====================================================

    @PostMapping("/apply/{beneficiaryId}/{schemeId}")
    public ResponseEntity<Application> applyForScheme(

            @PathVariable Long beneficiaryId,

            @PathVariable Long schemeId,

            @RequestBody ApplicationRequest request) {


        Application application =
                applicationService.applyForScheme(
                        beneficiaryId,
                        schemeId,
                        request
                );

        return ResponseEntity.ok(application);
    }

    @PostMapping("/submit-documents/{applicationId}")
    public ResponseEntity<Application> submitApplicationDocuments(@PathVariable Long applicationId) {
        Application application = applicationService.submitApplicationDocuments(applicationId);
        return ResponseEntity.ok(application);
    }
}