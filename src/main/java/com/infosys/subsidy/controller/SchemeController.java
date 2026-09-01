package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.*;
import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.enums.BeneficiaryCategory;
import com.infosys.subsidy.enums.SchemeStatus;
import com.infosys.subsidy.model.EligibilityEvaluationResult;
import com.infosys.subsidy.service.EligibilityEvaluationService;
import com.infosys.subsidy.service.SchemeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schemes")
public class SchemeController {

    private final SchemeService schemeService;
    private final EligibilityEvaluationService evaluationService;

    public SchemeController(SchemeService schemeService, EligibilityEvaluationService evaluationService) {
        this.schemeService = schemeService;
        this.evaluationService = evaluationService;
    }

    // ==================== SCHEME ENDPOINTS ====================

    @PostMapping
    public ResponseEntity<SchemeResponse> createScheme(@Valid @RequestBody SchemeRequest request) {
        Scheme created = schemeService.createScheme(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SchemeResponse.fromEntity(created));
    }

    @GetMapping
    public ResponseEntity<List<SchemeResponse>> getSchemes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BeneficiaryCategory category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) SchemeStatus status) {

        List<Scheme> schemes;
        if (keyword != null || category != null || region != null || status != null) {
            schemes = schemeService.filterSchemes(keyword, category, region, status);
        } else {
            schemes = schemeService.getAllSchemes();
        }

        List<SchemeResponse> response = schemes.stream()
                .map(SchemeResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<SchemeResponse>> getActiveSchemes() {
        List<Scheme> schemes = schemeService.getActiveSchemes();
        List<SchemeResponse> response = schemes.stream()
                .map(SchemeResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SchemeResponse> getSchemeById(@PathVariable Long id) {
        Scheme scheme = schemeService.getSchemeById(id);
        return ResponseEntity.ok(SchemeResponse.fromEntity(scheme));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<SchemeResponse> getSchemeByCode(@PathVariable String code) {
        Scheme scheme = schemeService.getSchemeByCode(code);
        return ResponseEntity.ok(SchemeResponse.fromEntity(scheme));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SchemeResponse> updateScheme(
            @PathVariable Long id,
            @Valid @RequestBody SchemeRequest request) {
        Scheme updated = schemeService.updateScheme(id, request);
        return ResponseEntity.ok(SchemeResponse.fromEntity(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SchemeResponse> changeStatus(
            @PathVariable Long id,
            @RequestParam SchemeStatus status) {
        Scheme updated = schemeService.changeSchemeStatus(id, status);
        return ResponseEntity.ok(SchemeResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheme(@PathVariable Long id) {
        schemeService.deleteScheme(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== CRITERIA ENDPOINTS ====================

    @GetMapping("/{id}/criteria")
    public ResponseEntity<List<EligibilityCriteriaResponse>> getCriteriaForScheme(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        List<EligibilityCriteria> criteria = activeOnly
                ? schemeService.getActiveCriteriaForScheme(id)
                : schemeService.getCriteriaForScheme(id);

        List<EligibilityCriteriaResponse> response = criteria.stream()
                .map(EligibilityCriteriaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/criteria")
    public ResponseEntity<EligibilityCriteriaResponse> addCriterion(
            @PathVariable Long id,
            @Valid @RequestBody EligibilityCriteriaRequest request) {

        EligibilityCriteria created = schemeService.addCriterionToScheme(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(EligibilityCriteriaResponse.fromEntity(created));
    }

    @PutMapping("/{id}/criteria/{criterionId}")
    public ResponseEntity<EligibilityCriteriaResponse> updateCriterion(
            @PathVariable Long id,
            @PathVariable Long criterionId,
            @Valid @RequestBody EligibilityCriteriaRequest request) {

        EligibilityCriteria updated = schemeService.updateCriterion(id, criterionId, request);
        return ResponseEntity.ok(EligibilityCriteriaResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}/criteria/{criterionId}")
    public ResponseEntity<Void> removeCriterion(
            @PathVariable Long id,
            @PathVariable Long criterionId) {

        schemeService.removeCriterionFromScheme(id, criterionId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/criteria/{criterionId}/toggle")
    public ResponseEntity<EligibilityCriteriaResponse> toggleCriterion(
            @PathVariable Long id,
            @PathVariable Long criterionId) {

        EligibilityCriteria toggled = schemeService.toggleCriterionStatus(id, criterionId);
        return ResponseEntity.ok(EligibilityCriteriaResponse.fromEntity(toggled));
    }

    // ==================== BUDGET DISBURSEMENT ====================

    @PostMapping("/{id}/disburse")
    public ResponseEntity<SchemeResponse> recordDisbursement(
            @PathVariable Long id,
            @RequestParam double amount) {

        Scheme updated = schemeService.recordDisbursement(id, amount);
        return ResponseEntity.ok(SchemeResponse.fromEntity(updated));
    }

    // ==================== ELIGIBILITY EVALUATION SIMULATION ====================

    @PostMapping("/{id}/evaluate")
    public ResponseEntity<EligibilityEvaluationResult> evaluateEligibility(
            @PathVariable Long id,
            @Valid @RequestBody EligibilityEvaluationRequest request) {

        Scheme scheme = schemeService.getSchemeById(id);
        EligibilityEvaluationResult report = evaluationService.evaluate(scheme, request);
        return ResponseEntity.ok(report);
    }
}
