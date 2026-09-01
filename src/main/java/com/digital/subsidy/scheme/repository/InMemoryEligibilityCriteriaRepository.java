package com.digital.subsidy.scheme.repository;

import com.digital.subsidy.scheme.model.EligibilityCriteria;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-Memory Repository for EligibilityCriteria entities.
 */
public class InMemoryEligibilityCriteriaRepository implements EligibilityCriteriaRepository {

    private final Map<Long, EligibilityCriteria> criteriaStore = new ConcurrentHashMap<>();
    private final AtomicLong criteriaIdGenerator = new AtomicLong(500);

    @Override
    public synchronized EligibilityCriteria save(EligibilityCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("Criteria cannot be null");
        }
        if (criteria.getId() == null) {
            criteria.setId(criteriaIdGenerator.incrementAndGet());
        }
        criteriaStore.put(criteria.getId(), criteria);
        return criteria;
    }

    @Override
    public Optional<EligibilityCriteria> findById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(criteriaStore.get(id));
    }

    @Override
    public List<EligibilityCriteria> findBySchemeId(Long schemeId) {
        if (schemeId == null) return Collections.emptyList();
        return criteriaStore.values().stream()
                .filter(c -> c.getScheme() != null && Objects.equals(c.getScheme().getId(), schemeId))
                .sorted(Comparator.comparing(EligibilityCriteria::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<EligibilityCriteria> findActiveBySchemeId(Long schemeId) {
        if (schemeId == null) return Collections.emptyList();
        return criteriaStore.values().stream()
                .filter(c -> c.getScheme() != null && Objects.equals(c.getScheme().getId(), schemeId) && c.isActive())
                .sorted(Comparator.comparing(EligibilityCriteria::getId))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized boolean deleteById(Long id) {
        if (id == null) return false;
        return criteriaStore.remove(id) != null;
    }

    @Override
    public synchronized void deleteBySchemeId(Long schemeId) {
        if (schemeId == null) return;
        List<Long> toRemove = criteriaStore.values().stream()
                .filter(c -> c.getScheme() != null && Objects.equals(c.getScheme().getId(), schemeId))
                .map(EligibilityCriteria::getId)
                .collect(Collectors.toList());
        toRemove.forEach(criteriaStore::remove);
    }

    @Override
    public long count() {
        return criteriaStore.size();
    }
}
