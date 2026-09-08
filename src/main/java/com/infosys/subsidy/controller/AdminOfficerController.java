package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.CreateOfficerRequest;
import com.infosys.subsidy.dto.OfficerResponse;
import com.infosys.subsidy.service.AdminOfficerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/officers")
public class AdminOfficerController {

    private final AdminOfficerService adminOfficerService;

    public AdminOfficerController(AdminOfficerService adminOfficerService) {
        this.adminOfficerService = adminOfficerService;
    }

    @PostMapping
    public ResponseEntity<OfficerResponse> createOfficer(@Valid @RequestBody CreateOfficerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminOfficerService.createOfficer(request));
    }

    @GetMapping
    public ResponseEntity<List<OfficerResponse>> getAllOfficers() {
        return ResponseEntity.ok(adminOfficerService.getAllOfficers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfficerResponse> getOfficerById(@PathVariable Long id) {
        return ResponseEntity.ok(adminOfficerService.getOfficerById(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<OfficerResponse> activateOfficer(@PathVariable Long id) {
        return ResponseEntity.ok(adminOfficerService.activateOfficer(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<OfficerResponse> deactivateOfficer(@PathVariable Long id) {
        return ResponseEntity.ok(adminOfficerService.deactivateOfficer(id));
    }
}
