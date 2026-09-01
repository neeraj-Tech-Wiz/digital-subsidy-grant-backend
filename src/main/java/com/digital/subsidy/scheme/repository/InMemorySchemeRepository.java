package com.digital.subsidy.scheme.repository;

import com.digital.subsidy.scheme.enums.BeneficiaryCategory;
import com.digital.subsidy.scheme.enums.SchemeStatus;
import com.digital.subsidy.scheme.model.EligibilityCriteria;
import com.digital.subsidy.scheme.model.Scheme;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-Memory Repository implementation for Scheme Entity.
 */
public class InMemorySchemeRepository implements SchemeRepository {

    private final Map<Long, Scheme> schemeStore = new ConcurrentHashMap<>();
    private final AtomicLong schemeIdGenerator = new AtomicLong(100);
    private final AtomicLong criteriaIdGenerator = new AtomicLong(500);

    @Override
    public synchronized Scheme save(Scheme scheme) {
        if (scheme == null) {
            throw new IllegalArgumentException("Scheme cannot be null");
        }

        if (scheme.getId() == null) {
            scheme.setId(schemeIdGenerator.incrementAndGet());
            scheme.setCreatedAt(LocalDateTime.now());
        }
        scheme.setUpdatedAt(LocalDateTime.now());

        // Assign IDs to any detached criteria in the scheme
        if (scheme.getCriteriaList() != null) {
            for (EligibilityCriteria criterion : scheme.getCriteriaList()) {
                if (criterion.getId() == null) {
                    criterion.setId(criteriaIdGenerator.incrementAndGet());
                }
                criterion.setScheme(scheme);
            }
        }

        schemeStore.put(scheme.getId(), scheme);
        return scheme;
    }

    @Override
    public Optional<Scheme> findById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(schemeStore.get(id));
    }

    @Override
    public Optional<Scheme> findBySchemeCode(String schemeCode) {
        if (schemeCode == null || schemeCode.trim().isEmpty()) return Optional.empty();
        return schemeStore.values().stream()
                .filter(s -> s.getSchemeCode() != null && s.getSchemeCode().equalsIgnoreCase(schemeCode.trim()))
                .findFirst();
    }

    @Override
    public Optional<Scheme> findBySchemeName(String schemeName) {
        if (schemeName == null || schemeName.trim().isEmpty()) return Optional.empty();
        return schemeStore.values().stream()
                .filter(s -> s.getSchemeName() != null && s.getSchemeName().equalsIgnoreCase(schemeName.trim()))
                .findFirst();
    }

    @Override
    public List<Scheme> findAll() {
        return schemeStore.values().stream()
                .sorted(Comparator.comparing(Scheme::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Scheme> findByStatus(SchemeStatus status) {
        if (status == null) return findAll();
        return schemeStore.values().stream()
                .filter(s -> s.getStatus() == status)
                .sorted(Comparator.comparing(Scheme::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Scheme> findByCategory(BeneficiaryCategory category) {
        if (category == null) return findAll();
        return schemeStore.values().stream()
                .filter(s -> s.getBeneficiaryCategory() == category)
                .sorted(Comparator.comparing(Scheme::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Scheme> search(String keyword, BeneficiaryCategory category, String region, SchemeStatus status) {
        return schemeStore.values().stream()
                .filter(s -> {
                    // Keyword filter (code, name, or description)
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchName = s.getSchemeName() != null && s.getSchemeName().toLowerCase().contains(kw);
                        boolean matchCode = s.getSchemeCode() != null && s.getSchemeCode().toLowerCase().contains(kw);
                        boolean matchDesc = s.getDescription() != null && s.getDescription().toLowerCase().contains(kw);
                        if (!matchName && !matchCode && !matchDesc) return false;
                    }
                    // Category filter
                    if (category != null && s.getBeneficiaryCategory() != category) {
                        return false;
                    }
                    // Region filter
                    if (region != null && !region.trim().isEmpty()) {
                        String reg = region.trim().toLowerCase();
                        if (s.getApplicableRegion() == null || !s.getApplicableRegion().toLowerCase().contains(reg)) {
                            return false;
                        }
                    }
                    // Status filter
                    if (status != null && s.getStatus() != status) {
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Scheme::getId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySchemeCode(String schemeCode) {
        return findBySchemeCode(schemeCode).isPresent();
    }

    @Override
    public boolean existsBySchemeName(String schemeName) {
        return findBySchemeName(schemeName).isPresent();
    }

    @Override
    public synchronized boolean deleteById(Long id) {
        if (id == null || !schemeStore.containsKey(id)) {
            return false;
        }
        schemeStore.remove(id);
        return true;
    }

    @Override
    public long count() {
        return schemeStore.size();
    }

    @Override
    public synchronized void clear() {
        schemeStore.clear();
    }
}
