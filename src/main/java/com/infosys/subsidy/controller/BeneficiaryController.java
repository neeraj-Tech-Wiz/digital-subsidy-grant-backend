package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.BeneficiaryProfileResponse;
import com.infosys.subsidy.dto.BeneficiaryRequest;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.service.BeneficiaryService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }


    // ============================================================
    // LEGACY — Direct beneficiary register (console / admin use)
    // POST /api/beneficiaries
    // ============================================================

    @PostMapping
    public ResponseEntity<Beneficiary> registerBeneficiary(
            @Valid @RequestBody BeneficiaryRequest request) {

        Beneficiary beneficiary =
                beneficiaryService.registerBeneficiary(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(beneficiary);
    }


    // ============================================================
    // WEB FLOW — Create Beneficiary Profile (authenticated user)
    // POST /api/beneficiaries/profile
    // Requires: ROLE_BENEFICIARY
    // Backend derives user from JWT — frontend must NOT send userId.
    // ============================================================

    @PostMapping("/profile")
    public ResponseEntity<BeneficiaryProfileResponse> createProfile(
            @Valid @RequestBody BeneficiaryRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        BeneficiaryProfileResponse response =
                beneficiaryService.createProfile(request, email);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // WEB FLOW — Get My Beneficiary Profile
    // GET /api/beneficiaries/me
    // Requires: ROLE_BENEFICIARY
    // Returns profile if exists, or 404 with a clear message.
    // ============================================================

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {

        String email = authentication.getName();
        Optional<BeneficiaryProfileResponse> profile =
                beneficiaryService.getMyProfile(email);

        if (profile.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "profileExists", false,
                            "message", "Beneficiary profile not found. Please complete your profile first."
                    ));
        }

        return ResponseEntity.ok(profile.get());
    }
}