package com.infosys.subsidy.controller;

import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.service.EligibilityCriteriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eligibility-criteria")
public class EligibilityCriteriaController {

    private final EligibilityCriteriaService criteriaService;

    public EligibilityCriteriaController(
            EligibilityCriteriaService criteriaService) {
        this.criteriaService = criteriaService;
    }

    @PostMapping
    public ResponseEntity<EligibilityCriteria> addCriteria(
            @RequestBody EligibilityCriteria criteria) {

        EligibilityCriteria savedCriteria =
                criteriaService.addCriteria(criteria);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedCriteria);
    }

    @GetMapping("/scheme/{schemeId}")
    public ResponseEntity<List<EligibilityCriteria>> getCriteriaByScheme(
            @PathVariable Long schemeId) {

        List<EligibilityCriteria> criteria =
                criteriaService.getCriteriaByScheme(schemeId);

        return ResponseEntity.ok(criteria);
    }
}
