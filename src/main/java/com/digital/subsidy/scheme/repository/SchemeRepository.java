package com.digital.subsidy.scheme.repository;

import com.digital.subsidy.scheme.enums.BeneficiaryCategory;
import com.digital.subsidy.scheme.enums.SchemeStatus;
import com.digital.subsidy.scheme.model.Scheme;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Scheme Entity.
 * Follows Spring Data JPA design patterns.
 */
public interface SchemeRepository {
    Scheme save(Scheme scheme);
    Optional<Scheme> findById(Long id);
    Optional<Scheme> findBySchemeCode(String schemeCode);
    Optional<Scheme> findBySchemeName(String schemeName);
    List<Scheme> findAll();
    List<Scheme> findByStatus(SchemeStatus status);
    List<Scheme> findByCategory(BeneficiaryCategory category);
    List<Scheme> search(String keyword, BeneficiaryCategory category, String region, SchemeStatus status);
    boolean existsBySchemeCode(String schemeCode);
    boolean existsBySchemeName(String schemeName);
    boolean deleteById(Long id);
    long count();
    void clear();
}
