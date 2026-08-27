package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.BeneficiaryRequest;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.service.BeneficiaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    public ResponseEntity<Beneficiary> registerBeneficiary(
            @Valid @RequestBody BeneficiaryRequest request) {

        Beneficiary beneficiary =
                beneficiaryService.registerBeneficiary(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(beneficiary);
    }
}