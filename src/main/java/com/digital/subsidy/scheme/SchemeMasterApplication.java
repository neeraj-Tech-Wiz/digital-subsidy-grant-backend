package com.digital.subsidy.scheme;

import com.digital.subsidy.scheme.repository.InMemoryEligibilityCriteriaRepository;
import com.digital.subsidy.scheme.repository.InMemorySchemeRepository;
import com.digital.subsidy.scheme.runner.SampleDataInitializer;
import com.digital.subsidy.scheme.runner.SchemeMasterConsoleRunner;
import com.digital.subsidy.scheme.service.EligibilityEvaluationService;
import com.digital.subsidy.scheme.service.SchemeService;
import com.digital.subsidy.scheme.service.SchemeServiceImpl;

/**
 * Main Entry Point for Scheme Master Backend Terminal Application.
 * Initializes layers, loads initial government schemes, and launches terminal UI.
 */
public class SchemeMasterApplication {

    public static void main(String[] args) {
        // Initialize Repositories
        InMemorySchemeRepository schemeRepository = new InMemorySchemeRepository();
        InMemoryEligibilityCriteriaRepository criteriaRepository = new InMemoryEligibilityCriteriaRepository();

        // Initialize Services
        SchemeService schemeService = new SchemeServiceImpl(schemeRepository, criteriaRepository);
        EligibilityEvaluationService evaluationService = new EligibilityEvaluationService();

        // Load Pre-configured Government Schemes
        SampleDataInitializer.loadSampleData(schemeService);

        // Start Console Runner Interface
        SchemeMasterConsoleRunner runner = new SchemeMasterConsoleRunner(schemeService, evaluationService);
        runner.run();
    }
}
