package com.infosys.subsidy.service;

import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.repository.EligibilityCriteriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EligibilityCriteriaService {

    private final EligibilityCriteriaRepository criteriaRepository;

    public EligibilityCriteriaService(
            EligibilityCriteriaRepository criteriaRepository) {
        this.criteriaRepository = criteriaRepository;
    }

    public EligibilityCriteria addCriteria(
            EligibilityCriteria criteria) {

        if (criteria.getWeight() < 0) {
            throw new IllegalArgumentException(
                    "Weight cannot be negative");
        }

        if (criteria.getMaxScore() < 0) {
            throw new IllegalArgumentException(
                    "Maximum score cannot be negative");
        }

        if (criteria.getWeight() < 0) {
            throw new IllegalArgumentException(
                    "Weight cannot be negative");
        }

        if (criteria.getMaxScore() < 0) {
            throw new IllegalArgumentException(
                    "Maximum score cannot be negative");
        }

        if (criteria.getWeight() > criteria.getMaxScore()) {

            throw new IllegalArgumentException(
                    "Weight cannot be greater than maximum score");
        }

        if (criteria.getExpectedValue() == null ||
                criteria.getExpectedValue().isBlank()) {

            throw new IllegalArgumentException(
                    "Expected value cannot be empty");
        }

        return criteriaRepository.save(criteria);
    }

    public List<EligibilityCriteria> getCriteriaByScheme(
            Long schemeId) {

        return criteriaRepository
                .findBySchemeIdAndActiveTrue(schemeId);
    }
}
