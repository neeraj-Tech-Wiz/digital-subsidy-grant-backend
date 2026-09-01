package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.ApplicationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(
            ApplicationService applicationService) {

        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<Application> createApplication(
            @Valid @RequestBody ApplicationRequest request) {

        Application application =
                applicationService.createApplication(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(application);
    }
}
