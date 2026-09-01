package com.digital.subsidy.scheme.repository;

import com.digital.subsidy.scheme.model.EligibilityCriteria;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for EligibilityCriteria Entity.
 */
public interface EligibilityCriteriaRepository {
    EligibilityCriteria save(EligibilityCriteria criteria);
    Optional<EligibilityCriteria> findById(Long id);
    List<EligibilityCriteria> findBySchemeId(Long schemeId);
    List<EligibilityCriteria> findActiveBySchemeId(Long schemeId);
    boolean deleteById(Long id);
    void deleteBySchemeId(Long schemeId);
    long count();
}
