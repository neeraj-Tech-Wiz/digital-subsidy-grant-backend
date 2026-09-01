package com.digital.subsidy.scheme.service;

import com.digital.subsidy.scheme.enums.BeneficiaryCategory;
import com.digital.subsidy.scheme.enums.CriterionType;
import com.digital.subsidy.scheme.enums.Operator;
import com.digital.subsidy.scheme.enums.SchemeStatus;
import com.digital.subsidy.scheme.exception.DuplicateSchemeException;
import com.digital.subsidy.scheme.exception.InvalidCriteriaException;
import com.digital.subsidy.scheme.exception.SchemeNotFoundException;
import com.digital.subsidy.scheme.exception.ValidationException;
import com.digital.subsidy.scheme.model.EligibilityCriteria;
import com.digital.subsidy.scheme.model.Scheme;
import com.digital.subsidy.scheme.repository.EligibilityCriteriaRepository;
import com.digital.subsidy.scheme.repository.SchemeRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of SchemeService providing complete business logic, validations,
 * exception handling, and relationship management between Scheme and EligibilityCriteria.
 */
public class SchemeServiceImpl implements SchemeService {

    private final SchemeRepository schemeRepository;
    private final EligibilityCriteriaRepository criteriaRepository;

    public SchemeServiceImpl(SchemeRepository schemeRepository, EligibilityCriteriaRepository criteriaRepository) {
        this.schemeRepository = schemeRepository;
        this.criteriaRepository = criteriaRepository;
    }

    // ==================== SCHEME OPERATIONS ====================

    @Override
    public Scheme createScheme(Scheme scheme) {
        if (scheme == null) {
            throw new ValidationException("Scheme data cannot be null");
        }
        validateSchemeFields(scheme, true);

        // Check for duplicates
        if (schemeRepository.existsBySchemeCode(scheme.getSchemeCode())) {
            throw new DuplicateSchemeException("A scheme with code '" + scheme.getSchemeCode() + "' already exists.");
        }
        if (schemeRepository.existsBySchemeName(scheme.getSchemeName())) {
            throw new DuplicateSchemeException("A scheme with name '" + scheme.getSchemeName() + "' already exists.");
        }

        return schemeRepository.save(scheme);
    }

    @Override
    public Scheme updateScheme(Long schemeId, Scheme updates) {
        if (schemeId == null) {
            throw new ValidationException("Scheme ID cannot be null");
        }
        if (updates == null) {
            throw new ValidationException("Update details cannot be null");
        }

        Scheme existing = getSchemeById(schemeId);

        // If code changed, check uniqueness
        if (updates.getSchemeCode() != null && !updates.getSchemeCode().equalsIgnoreCase(existing.getSchemeCode())) {
            Optional<Scheme> duplicateCode = schemeRepository.findBySchemeCode(updates.getSchemeCode());
            if (duplicateCode.isPresent() && !duplicateCode.get().getId().equals(schemeId)) {
                throw new DuplicateSchemeException("Scheme code '" + updates.getSchemeCode() + "' is already in use.");
            }
            existing.setSchemeCode(updates.getSchemeCode().trim());
        }

        // If name changed, check uniqueness
        if (updates.getSchemeName() != null && !updates.getSchemeName().equalsIgnoreCase(existing.getSchemeName())) {
            Optional<Scheme> duplicateName = schemeRepository.findBySchemeName(updates.getSchemeName());
            if (duplicateName.isPresent() && !duplicateName.get().getId().equals(schemeId)) {
                throw new DuplicateSchemeException("Scheme name '" + updates.getSchemeName() + "' is already in use.");
            }
            existing.setSchemeName(updates.getSchemeName().trim());
        }

        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription().trim());
        }
        if (updates.getGrantAmount() > 0) {
            existing.setGrantAmount(updates.getGrantAmount());
        }
        if (updates.getTotalBudget() > 0) {
            if (updates.getTotalBudget() < existing.getGrantAmount()) {
                throw new ValidationException("Total budget must be at least equal to single grant amount");
            }
            existing.setTotalBudget(updates.getTotalBudget());
        }
        if (updates.getApplicableRegion() != null && !updates.getApplicableRegion().trim().isEmpty()) {
            existing.setApplicableRegion(updates.getApplicableRegion().trim());
        }
        if (updates.getBeneficiaryCategory() != null) {
            existing.setBeneficiaryCategory(updates.getBeneficiaryCategory());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }

        validateSchemeFields(existing, false);
        return schemeRepository.save(existing);
    }

    @Override
    public Scheme getSchemeById(Long schemeId) {
        if (schemeId == null) {
            throw new ValidationException("Scheme ID must be provided");
        }
        return schemeRepository.findById(schemeId)
                .orElseThrow(() -> new SchemeNotFoundException("Scheme not found with ID: " + schemeId));
    }

    @Override
    public Scheme getSchemeByCode(String schemeCode) {
        if (schemeCode == null || schemeCode.trim().isEmpty()) {
            throw new ValidationException("Scheme code must be provided");
        }
        return schemeRepository.findBySchemeCode(schemeCode.trim())
                .orElseThrow(() -> new SchemeNotFoundException("Scheme not found with code: " + schemeCode));
    }

    @Override
    public List<Scheme> getAllSchemes() {
        return schemeRepository.findAll();
    }

    @Override
    public List<Scheme> getActiveSchemes() {
        return schemeRepository.findByStatus(SchemeStatus.ACTIVE);
    }

    @Override
    public List<Scheme> filterSchemes(String keyword, BeneficiaryCategory category, String region, SchemeStatus status) {
        return schemeRepository.search(keyword, category, region, status);
    }

    @Override
    public Scheme changeSchemeStatus(Long schemeId, SchemeStatus newStatus) {
        if (newStatus == null) {
            throw new ValidationException("New status cannot be null");
        }
        Scheme scheme = getSchemeById(schemeId);

        // Validation for activating: must have at least one active criterion
        if (newStatus == SchemeStatus.ACTIVE) {
            long activeCriteriaCount = scheme.getCriteriaList().stream().filter(EligibilityCriteria::isActive).count();
            if (activeCriteriaCount == 0) {
                throw new ValidationException("Cannot activate Scheme without at least 1 active Eligibility Criterion configured.");
            }
        }

        scheme.setStatus(newStatus);
        return schemeRepository.save(scheme);
    }

    @Override
    public boolean deleteScheme(Long schemeId) {
        Scheme scheme = getSchemeById(schemeId);
        // Cascade delete criteria
        criteriaRepository.deleteBySchemeId(schemeId);
        return schemeRepository.deleteById(schemeId);
    }

    // ==================== SCHEME & CRITERIA RELATIONSHIP OPERATIONS ====================

    @Override
    public EligibilityCriteria addCriterionToScheme(Long schemeId, EligibilityCriteria criterion) {
        if (criterion == null) {
            throw new ValidationException("Eligibility criterion data cannot be null");
        }
        Scheme scheme = getSchemeById(schemeId);
        validateCriteriaFields(criterion);

        // Check for duplicate criterion name in the same scheme
        boolean exists = scheme.getCriteriaList().stream()
                .anyMatch(c -> c.getCriterionName().equalsIgnoreCase(criterion.getCriterionName()));
        if (exists) {
            throw new InvalidCriteriaException("A criterion with name '" + criterion.getCriterionName() + "' already exists in this scheme.");
        }

        criterion.setScheme(scheme);
        EligibilityCriteria saved = criteriaRepository.save(criterion);
        scheme.addCriterion(saved);
        schemeRepository.save(scheme);
        return saved;
    }

    @Override
    public EligibilityCriteria updateCriterion(Long schemeId, Long criterionId, EligibilityCriteria updatedCriterion) {
        if (criterionId == null) {
            throw new ValidationException("Criterion ID cannot be null");
        }
        Scheme scheme = getSchemeById(schemeId);
        EligibilityCriteria existing = criteriaRepository.findById(criterionId)
                .orElseThrow(() -> new InvalidCriteriaException("Criterion not found with ID: " + criterionId));

        if (!Objects.equals(existing.getScheme().getId(), schemeId)) {
            throw new InvalidCriteriaException("Criterion ID " + criterionId + " does not belong to Scheme ID " + schemeId);
        }

        if (updatedCriterion.getCriterionName() != null && !updatedCriterion.getCriterionName().trim().isEmpty()) {
            existing.setCriterionName(updatedCriterion.getCriterionName().trim());
        }
        if (updatedCriterion.getCriterionType() != null) {
            existing.setCriterionType(updatedCriterion.getCriterionType());
        }
        if (updatedCriterion.getOperator() != null) {
            existing.setOperator(updatedCriterion.getOperator());
        }
        if (updatedCriterion.getExpectedValue() != null) {
            existing.setExpectedValue(updatedCriterion.getExpectedValue().trim());
        }
        if (updatedCriterion.getWeight() > 0) {
            existing.setWeight(updatedCriterion.getWeight());
            existing.setMaxScore(updatedCriterion.getWeight());
        }
        existing.setMandatory(updatedCriterion.isMandatory());
        existing.setActive(updatedCriterion.isActive());
        if (updatedCriterion.getDescription() != null) {
            existing.setDescription(updatedCriterion.getDescription());
        }

        validateCriteriaFields(existing);
        criteriaRepository.save(existing);
        schemeRepository.save(scheme);
        return existing;
    }

    @Override
    public boolean removeCriterionFromScheme(Long schemeId, Long criterionId) {
        Scheme scheme = getSchemeById(schemeId);
        boolean removed = scheme.removeCriterion(criterionId);
        if (removed) {
            criteriaRepository.deleteById(criterionId);
            schemeRepository.save(scheme);
        }
        return removed;
    }

    @Override
    public EligibilityCriteria toggleCriterionStatus(Long schemeId, Long criterionId) {
        Scheme scheme = getSchemeById(schemeId);
        EligibilityCriteria criterion = criteriaRepository.findById(criterionId)
                .orElseThrow(() -> new InvalidCriteriaException("Criterion not found with ID: " + criterionId));

        if (!Objects.equals(criterion.getScheme().getId(), schemeId)) {
            throw new InvalidCriteriaException("Criterion does not belong to specified scheme");
        }

        criterion.setActive(!criterion.isActive());
        criteriaRepository.save(criterion);
        schemeRepository.save(scheme);
        return criterion;
    }

    @Override
    public List<EligibilityCriteria> getCriteriaForScheme(Long schemeId) {
        getSchemeById(schemeId); // verify exists
        return criteriaRepository.findBySchemeId(schemeId);
    }

    @Override
    public List<EligibilityCriteria> getActiveCriteriaForScheme(Long schemeId) {
        getSchemeById(schemeId); // verify exists
        return criteriaRepository.findActiveBySchemeId(schemeId);
    }

    @Override
    public void recordDisbursement(Long schemeId, double amount) {
        if (amount <= 0) {
            throw new ValidationException("Disbursement amount must be greater than zero");
        }
        Scheme scheme = getSchemeById(schemeId);
        if (scheme.getRemainingBudget() < amount) {
            throw new ValidationException("Insufficient scheme budget! Available: ₹" + scheme.getRemainingBudget() + ", Requested: ₹" + amount);
        }
        scheme.setDisbursedAmount(scheme.getDisbursedAmount() + amount);
        schemeRepository.save(scheme);
    }

    @Override
    public long getTotalSchemeCount() {
        return schemeRepository.count();
    }

    // ==================== PRIVATE HELPER VALIDATIONS ====================

    private void validateSchemeFields(Scheme scheme, boolean isNew) {
        if (scheme.getSchemeName() == null || scheme.getSchemeName().trim().isEmpty()) {
            throw new ValidationException("Scheme name cannot be empty");
        }
        if (scheme.getSchemeCode() == null || scheme.getSchemeCode().trim().isEmpty()) {
            throw new ValidationException("Scheme code cannot be empty");
        }
        if (scheme.getGrantAmount() <= 0) {
            throw new ValidationException("Grant amount must be greater than 0");
        }
        if (scheme.getTotalBudget() <= 0) {
            throw new ValidationException("Total budget allocation must be greater than 0");
        }
        if (scheme.getTotalBudget() < scheme.getGrantAmount()) {
            throw new ValidationException("Total budget allocation (₹" + scheme.getTotalBudget() +
                    ") must be at least the individual grant amount (₹" + scheme.getGrantAmount() + ")");
        }
        if (scheme.getBeneficiaryCategory() == null) {
            scheme.setBeneficiaryCategory(BeneficiaryCategory.GENERAL);
        }
        if (scheme.getStatus() == null) {
            scheme.setStatus(SchemeStatus.DRAFT);
        }
        if (scheme.getApplicableRegion() == null || scheme.getApplicableRegion().trim().isEmpty()) {
            scheme.setApplicableRegion("All India");
        }
    }

    private void validateCriteriaFields(EligibilityCriteria criterion) {
        if (criterion.getCriterionName() == null || criterion.getCriterionName().trim().isEmpty()) {
            throw new InvalidCriteriaException("Criterion name cannot be empty");
        }
        if (criterion.getCriterionType() == null) {
            throw new InvalidCriteriaException("Criterion type must be specified (NUMERIC, BOOLEAN, TEXT, ENUM)");
        }
        if (criterion.getOperator() == null) {
            throw new InvalidCriteriaException("Comparison operator must be specified");
        }
        if (criterion.getExpectedValue() == null || criterion.getExpectedValue().trim().isEmpty()) {
            throw new InvalidCriteriaException("Expected value cannot be empty");
        }
        if (criterion.getWeight() <= 0 || criterion.getWeight() > 100) {
            throw new InvalidCriteriaException("Criterion weight must be between 1 and 100 points");
        }

        // Validate operator compatibility with criterion type
        if (criterion.getCriterionType() == CriterionType.NUMERIC) {
            try {
                Double.parseDouble(criterion.getExpectedValue().trim());
            } catch (NumberFormatException e) {
                throw new InvalidCriteriaException("Expected value must be a valid number for NUMERIC criterion type");
            }
        } else if (criterion.getCriterionType() == CriterionType.BOOLEAN) {
            String val = criterion.getExpectedValue().trim().toLowerCase();
            if (!val.equals("true") && !val.equals("false") && !val.equals("yes") && !val.equals("no")) {
                throw new InvalidCriteriaException("Expected value must be 'true' or 'false' for BOOLEAN criterion type");
            }
        }
    }
}
