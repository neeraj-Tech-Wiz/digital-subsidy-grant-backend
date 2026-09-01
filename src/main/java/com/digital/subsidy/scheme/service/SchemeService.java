package com.digital.subsidy.scheme.service;

import com.digital.subsidy.scheme.enums.BeneficiaryCategory;
import com.digital.subsidy.scheme.enums.SchemeStatus;
import com.digital.subsidy.scheme.model.EligibilityCriteria;
import com.digital.subsidy.scheme.model.Scheme;

import java.util.List;

/**
 * Service interface defining business operations for Scheme Master data.
 */
public interface SchemeService {

    // Scheme Master CRUD Operations
    Scheme createScheme(Scheme scheme);
    Scheme updateScheme(Long schemeId, Scheme schemeUpdates);
    Scheme getSchemeById(Long schemeId);
    Scheme getSchemeByCode(String schemeCode);
    List<Scheme> getAllSchemes();
    List<Scheme> getActiveSchemes();
    List<Scheme> filterSchemes(String keyword, BeneficiaryCategory category, String region, SchemeStatus status);
    Scheme changeSchemeStatus(Long schemeId, SchemeStatus newStatus);
    boolean deleteScheme(Long schemeId);

    // Scheme & Eligibility Criteria Relationship Operations
    EligibilityCriteria addCriterionToScheme(Long schemeId, EligibilityCriteria criterion);
    EligibilityCriteria updateCriterion(Long schemeId, Long criterionId, EligibilityCriteria updatedCriterion);
    boolean removeCriterionFromScheme(Long schemeId, Long criterionId);
    EligibilityCriteria toggleCriterionStatus(Long schemeId, Long criterionId);
    List<EligibilityCriteria> getCriteriaForScheme(Long schemeId);
    List<EligibilityCriteria> getActiveCriteriaForScheme(Long schemeId);

    // Budget and Statistics
    void recordDisbursement(Long schemeId, double amount);
    long getTotalSchemeCount();
}
