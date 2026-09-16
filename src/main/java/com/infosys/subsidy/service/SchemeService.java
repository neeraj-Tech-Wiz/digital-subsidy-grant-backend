package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.EligibilityCriteriaRequest;
import com.infosys.subsidy.dto.SchemeRequest;
import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.enums.BeneficiaryCategory;
import com.infosys.subsidy.enums.SchemeStatus;

import java.util.List;

/**
 * Service interface defining business operations for Scheme Master data.
 */
public interface SchemeService {

    // Scheme Master CRUD Operations
    Scheme createScheme(SchemeRequest request);
    Scheme createScheme(Scheme scheme);
    Scheme updateScheme(Long schemeId, SchemeRequest updates);
    Scheme getSchemeById(Long schemeId);
    Scheme getSchemeByCode(String schemeCode);
    List<Scheme> getAllSchemes();
    List<Scheme> getActiveSchemes();
    List<Scheme> filterSchemes(String keyword, BeneficiaryCategory category, String region, SchemeStatus status);
    Scheme changeSchemeStatus(Long schemeId, SchemeStatus newStatus);
    boolean deleteScheme(Long schemeId);

    // Scheme & Eligibility Criteria Relationship Operations
    EligibilityCriteria addCriterionToScheme(Long schemeId, EligibilityCriteriaRequest criterionRequest);
    EligibilityCriteria addCriterionToScheme(Long schemeId, EligibilityCriteria criterion);
    EligibilityCriteria updateCriterion(Long schemeId, Long criterionId, EligibilityCriteriaRequest updatedRequest);
    boolean removeCriterionFromScheme(Long schemeId, Long criterionId);
    EligibilityCriteria toggleCriterionStatus(Long schemeId, Long criterionId);
    List<EligibilityCriteria> getCriteriaForScheme(Long schemeId);
    List<EligibilityCriteria> getActiveCriteriaForScheme(Long schemeId);
    java.util.List<com.infosys.subsidy.entity.SchemeRequiredDocument> getActiveSchemeDocuments(Long schemeId);

    // Budget and Statistics
    Scheme recordDisbursement(Long schemeId, double amount);
    long getTotalSchemeCount();
}
