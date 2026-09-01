package com.digital.subsidy.scheme;

import com.digital.subsidy.scheme.enums.BeneficiaryCategory;
import com.digital.subsidy.scheme.enums.CriterionType;
import com.digital.subsidy.scheme.enums.Operator;
import com.digital.subsidy.scheme.enums.SchemeStatus;
import com.digital.subsidy.scheme.enums.ScoreCategory;
import com.digital.subsidy.scheme.exception.DuplicateSchemeException;
import com.digital.subsidy.scheme.exception.SchemeNotFoundException;
import com.digital.subsidy.scheme.exception.ValidationException;
import com.digital.subsidy.scheme.model.*;
import com.digital.subsidy.scheme.repository.InMemoryEligibilityCriteriaRepository;
import com.digital.subsidy.scheme.repository.InMemorySchemeRepository;
import com.digital.subsidy.scheme.service.EligibilityEvaluationService;
import com.digital.subsidy.scheme.service.SchemeService;
import com.digital.subsidy.scheme.service.SchemeServiceImpl;

import java.util.List;

/**
 * Automated Test Suite for Scheme Master Backend:
 * - Scheme CRUD operations
 * - Scheme and Eligibility Criteria One-to-Many Relationship
 * - Validation & Exception handling
 * - Search & Filtering
 * - Eligibility Scoring & Routing Logic
 */
public class SchemeMasterServiceTest {

    private static int passedTests = 0;
    private static int failedTests = 0;

    private static SchemeService createService() {
        InMemorySchemeRepository schemeRepo = new InMemorySchemeRepository();
        InMemoryEligibilityCriteriaRepository criteriaRepo = new InMemoryEligibilityCriteriaRepository();
        return new SchemeServiceImpl(schemeRepo, criteriaRepo);
    }

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("   STARTING SCHEME MASTER AUTOMATED UNIT & INTEGRATION TESTS ");
        System.out.println("==========================================================");

        testCreateSchemeSuccess();
        testDuplicateSchemeCodeRejection();
        testDuplicateSchemeNameRejection();
        testSchemeValidationConstraints();
        testSchemeEligibilityCriteriaRelationship();
        testUpdateSchemeAndCriteria();
        testDeleteSchemeCascadesCriteria();
        testSearchAndFilterSchemes();
        testEligibilityScoringFastTrack();
        testEligibilityScoringMandatoryFailure();
        testEligibilityScoringNormalVerification();

        System.out.println("==========================================================");
        System.out.println("TEST SUMMARY: " + passedTests + " PASSED, " + failedTests + " FAILED");
        System.out.println("==========================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }

    private static void testCreateSchemeSuccess() {
        try {
            SchemeService service = createService();
            Scheme s = new Scheme(null, "SCH-TEST-01", "Test Agriculture Scheme", "Test Desc", 5000.0, 100000.0, SchemeStatus.DRAFT, "Maharashtra", BeneficiaryCategory.FARMER);
            Scheme created = service.createScheme(s);

            assertCondition(created.getId() != null, "Scheme ID generated");
            assertCondition(created.getSchemeCode().equals("SCH-TEST-01"), "Scheme code matches");
            assertCondition(created.getRemainingBudget() == 100000.0, "Remaining budget calculated");
            recordPass("testCreateSchemeSuccess");
        } catch (Exception e) {
            recordFail("testCreateSchemeSuccess", e.getMessage());
        }
    }

    private static void testDuplicateSchemeCodeRejection() {
        try {
            SchemeService service = createService();
            Scheme s1 = new Scheme(null, "SCH-DUP-01", "Unique Scheme 1", "Desc", 5000.0, 100000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.GENERAL);
            service.createScheme(s1);

            Scheme s2 = new Scheme(null, "SCH-DUP-01", "Different Name Scheme", "Desc", 5000.0, 100000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.GENERAL);
            try {
                service.createScheme(s2);
                recordFail("testDuplicateSchemeCodeRejection", "Expected DuplicateSchemeException but succeeded");
            } catch (DuplicateSchemeException e) {
                recordPass("testDuplicateSchemeCodeRejection");
            }
        } catch (Exception e) {
            recordFail("testDuplicateSchemeCodeRejection", e.getMessage());
        }
    }

    private static void testDuplicateSchemeNameRejection() {
        try {
            SchemeService service = createService();
            Scheme s1 = new Scheme(null, "SCH-CODE-01", "Duplicate Scheme Name", "Desc", 5000.0, 100000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.GENERAL);
            service.createScheme(s1);

            Scheme s2 = new Scheme(null, "SCH-CODE-02", "Duplicate Scheme Name", "Desc", 5000.0, 100000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.GENERAL);
            try {
                service.createScheme(s2);
                recordFail("testDuplicateSchemeNameRejection", "Expected DuplicateSchemeException but succeeded");
            } catch (DuplicateSchemeException e) {
                recordPass("testDuplicateSchemeNameRejection");
            }
        } catch (Exception e) {
            recordFail("testDuplicateSchemeNameRejection", e.getMessage());
        }
    }

    private static void testSchemeValidationConstraints() {
        try {
            SchemeService service = createService();

            // Grant amount <= 0
            Scheme s = new Scheme(null, "SCH-ERR-01", "Invalid Grant", "Desc", -100.0, 100000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.GENERAL);
            try {
                service.createScheme(s);
                recordFail("testSchemeValidationConstraints", "Expected ValidationException for negative grant");
                return;
            } catch (ValidationException ignored) {}

            // Budget < Grant
            Scheme s2 = new Scheme(null, "SCH-ERR-02", "Invalid Budget", "Desc", 50000.0, 10000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.GENERAL);
            try {
                service.createScheme(s2);
                recordFail("testSchemeValidationConstraints", "Expected ValidationException when budget < grant");
                return;
            } catch (ValidationException ignored) {}

            recordPass("testSchemeValidationConstraints");
        } catch (Exception e) {
            recordFail("testSchemeValidationConstraints", e.getMessage());
        }
    }

    private static void testSchemeEligibilityCriteriaRelationship() {
        try {
            SchemeService service = createService();
            Scheme s = new Scheme(null, "SCH-REL-01", "Relationship Test Scheme", "Desc", 10000.0, 500000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.FARMER);
            Scheme created = service.createScheme(s);

            EligibilityCriteria c1 = new EligibilityCriteria(null, "Annual Income Limit", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "200000", 30, 30, true, true, "Desc");
            EligibilityCriteria c2 = new EligibilityCriteria(null, "Land Holding", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "4.0", 25, 25, true, true, "Desc");
            EligibilityCriteria c3 = new EligibilityCriteria(null, "KYC Verified", CriterionType.BOOLEAN, Operator.EQUAL, "true", 45, 45, true, true, "Desc");

            service.addCriterionToScheme(created.getId(), c1);
            service.addCriterionToScheme(created.getId(), c2);
            service.addCriterionToScheme(created.getId(), c3);

            Scheme fetched = service.getSchemeById(created.getId());
            assertCondition(fetched.getCriteriaList().size() == 3, "Criteria list size is 3");
            assertCondition(fetched.getTotalCriteriaWeight() == 100, "Total criteria weight is 100 points");
            assertCondition(fetched.getMandatoryCriteriaCount() == 3, "Mandatory criteria count is 3");

            // Verify child criterion references parent scheme
            for (EligibilityCriteria crit : fetched.getCriteriaList()) {
                assertCondition(crit.getScheme().getId().equals(created.getId()), "Criterion correctly references parent scheme ID");
            }

            recordPass("testSchemeEligibilityCriteriaRelationship");
        } catch (Exception e) {
            recordFail("testSchemeEligibilityCriteriaRelationship", e.getMessage());
        }
    }

    private static void testUpdateSchemeAndCriteria() {
        try {
            SchemeService service = createService();
            Scheme s = new Scheme(null, "SCH-UPD-01", "Original Scheme", "Desc", 10000.0, 500000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.FARMER);
            Scheme created = service.createScheme(s);

            EligibilityCriteria c1 = service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Income", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "200000", 50, 50, true, true, "Desc"));

            // Update scheme details
            Scheme updates = new Scheme();
            updates.setSchemeName("Updated Scheme Name");
            updates.setGrantAmount(15000.0);
            service.updateScheme(created.getId(), updates);

            Scheme fetched = service.getSchemeById(created.getId());
            assertCondition(fetched.getSchemeName().equals("Updated Scheme Name"), "Scheme name updated");
            assertCondition(fetched.getGrantAmount() == 15000.0, "Grant amount updated");

            // Update Criterion
            EligibilityCriteria critUpdate = new EligibilityCriteria();
            critUpdate.setExpectedValue("300000");
            critUpdate.setWeight(60);
            service.updateCriterion(created.getId(), c1.getId(), critUpdate);

            EligibilityCriteria updatedCrit = service.getCriteriaForScheme(created.getId()).get(0);
            assertCondition(updatedCrit.getExpectedValue().equals("300000"), "Criterion expected value updated");
            assertCondition(updatedCrit.getWeight() == 60, "Criterion weight updated");

            // Remove Criterion
            service.removeCriterionFromScheme(created.getId(), c1.getId());
            assertCondition(service.getCriteriaForScheme(created.getId()).isEmpty(), "Criterion removed successfully");

            recordPass("testUpdateSchemeAndCriteria");
        } catch (Exception e) {
            recordFail("testUpdateSchemeAndCriteria", e.getMessage());
        }
    }

    private static void testDeleteSchemeCascadesCriteria() {
        try {
            SchemeService service = createService();
            Scheme s = new Scheme(null, "SCH-DEL-01", "To Be Deleted", "Desc", 10000.0, 500000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.FARMER);
            Scheme created = service.createScheme(s);
            service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Income", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "200000", 50, 50, true, true, "Desc"));

            service.deleteScheme(created.getId());

            try {
                service.getSchemeById(created.getId());
                recordFail("testDeleteSchemeCascadesCriteria", "Scheme should have been deleted");
            } catch (SchemeNotFoundException expected) {
                recordPass("testDeleteSchemeCascadesCriteria");
            }
        } catch (Exception e) {
            recordFail("testDeleteSchemeCascadesCriteria", e.getMessage());
        }
    }

    private static void testSearchAndFilterSchemes() {
        try {
            SchemeService service = createService();
            service.createScheme(new Scheme(null, "SCH-FARM-01", "Kisan Subsidy", "Farmer grant", 6000.0, 50000.0, SchemeStatus.ACTIVE, "Maharashtra", BeneficiaryCategory.FARMER));
            service.createScheme(new Scheme(null, "SCH-STUD-01", "Merit Scholar", "Education grant", 20000.0, 100000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.STUDENT));
            service.createScheme(new Scheme(null, "SCH-FARM-02", "Drip Irrigation Subsidy", "Farmer grant", 15000.0, 100000.0, SchemeStatus.DRAFT, "Karnataka", BeneficiaryCategory.FARMER));

            List<Scheme> farmers = service.filterSchemes(null, BeneficiaryCategory.FARMER, null, null);
            assertCondition(farmers.size() == 2, "Farmer filter returned 2 schemes");

            List<Scheme> activeSchemes = service.filterSchemes(null, null, null, SchemeStatus.ACTIVE);
            assertCondition(activeSchemes.size() == 2, "Active filter returned 2 schemes");

            List<Scheme> maharashtra = service.filterSchemes(null, null, "Maharashtra", null);
            assertCondition(maharashtra.size() == 1, "Maharashtra region filter returned 1 scheme");

            List<Scheme> keywordSearch = service.filterSchemes("Irrigation", null, null, null);
            assertCondition(keywordSearch.size() == 1, "Keyword search returned 1 scheme");

            recordPass("testSearchAndFilterSchemes");
        } catch (Exception e) {
            recordFail("testSearchAndFilterSchemes", e.getMessage());
        }
    }

    private static void testEligibilityScoringFastTrack() {
        try {
            SchemeService service = createService();
            EligibilityEvaluationService evalService = new EligibilityEvaluationService();

            Scheme scheme = new Scheme(null, "SCH-PM-KISAN", "PM Kisan", "Desc", 6000.0, 100000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.FARMER);
            Scheme created = service.createScheme(scheme);

            service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Annual Family Income", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "250000", 30, 30, true, true, ""));
            service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Land Holding Area", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "5.0", 30, 30, true, true, ""));
            service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Beneficiary Category", CriterionType.ENUM, Operator.EQUAL, "FARMER", 20, 20, true, true, ""));
            service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Aadhaar Identity KYC", CriterionType.BOOLEAN, Operator.EQUAL, "true", 20, 20, true, true, ""));

            Scheme freshScheme = service.getSchemeById(created.getId());

            // Beneficiary satisfies all criteria (30 + 30 + 20 + 20 = 100 points)
            BeneficiaryProfile profile = new BeneficiaryProfile(1L, "Rajesh Kumar", 42, "123456789012", "9876543210", "rajesh@test.com", 150000.0, 3.2, BeneficiaryCategory.FARMER, "Maharashtra");
            profile.setAadhaarVerified(true);

            EligibilityEvaluationResult result = evalService.evaluate(freshScheme, profile);
            assertCondition(result.getTotalScore() == 100, "Total score is 100");
            assertCondition(result.isAllMandatorySatisfied(), "All mandatory satisfied");
            assertCondition(result.getScoreCategory() == ScoreCategory.FAST_TRACK, "Category is FAST_TRACK");

            recordPass("testEligibilityScoringFastTrack");
        } catch (Exception e) {
            recordFail("testEligibilityScoringFastTrack", e.getMessage());
        }
    }

    private static void testEligibilityScoringMandatoryFailure() {
        try {
            SchemeService service = createService();
            EligibilityEvaluationService evalService = new EligibilityEvaluationService();

            Scheme scheme = new Scheme(null, "SCH-SOLAR", "Surya Ghar", "Desc", 78000.0, 500000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.GENERAL);
            Scheme created = service.createScheme(scheme);

            service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Annual Family Income", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "500000", 50, 50, false, true, ""));
            service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Aadhaar Identity KYC", CriterionType.BOOLEAN, Operator.EQUAL, "true", 50, 50, true, true, "Mandatory Aadhaar"));

            Scheme freshScheme = service.getSchemeById(created.getId());

            // Beneficiary passes income, but fails mandatory Aadhaar KYC
            BeneficiaryProfile profile = new BeneficiaryProfile(2L, "Sunil Verma", 35, "123456789012", "9876543210", "sunil@test.com", 200000.0, 0.0, BeneficiaryCategory.GENERAL, "Delhi");
            profile.setAadhaarVerified(false);

            EligibilityEvaluationResult result = evalService.evaluate(freshScheme, profile);
            assertCondition(result.getTotalScore() == 50, "Total score awarded is 50");
            assertCondition(!result.isAllMandatorySatisfied(), "Mandatory not satisfied");
            assertCondition(result.getScoreCategory() == ScoreCategory.INELIGIBLE, "Routing category is INELIGIBLE due to mandatory failure");

            recordPass("testEligibilityScoringMandatoryFailure");
        } catch (Exception e) {
            recordFail("testEligibilityScoringMandatoryFailure", e.getMessage());
        }
    }

    private static void testEligibilityScoringNormalVerification() {
        try {
            SchemeService service = createService();
            EligibilityEvaluationService evalService = new EligibilityEvaluationService();

            Scheme scheme = new Scheme(null, "SCH-TEST-ROUTING", "Routing Test Scheme", "Desc", 25000.0, 500000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.GENERAL);
            Scheme created = service.createScheme(scheme);

            service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Annual Family Income", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "300000", 40, 40, false, true, ""));
            service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Land Holding Area", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "2.0", 30, 30, false, true, ""));
            service.addCriterionToScheme(created.getId(), new EligibilityCriteria(null, "Aadhaar Identity KYC", CriterionType.BOOLEAN, Operator.EQUAL, "true", 30, 30, true, true, ""));

            Scheme freshScheme = service.getSchemeById(created.getId());

            // Score: Income pass (40) + KYC pass (30) + Land fail (0) = 70 points -> NORMAL_VERIFICATION (50-79)
            BeneficiaryProfile profile = new BeneficiaryProfile(3L, "Anand Rao", 30, "123456789012", "9876543210", "anand@test.com", 250000.0, 4.5, BeneficiaryCategory.GENERAL, "Karnataka");
            profile.setAadhaarVerified(true);

            EligibilityEvaluationResult result = evalService.evaluate(freshScheme, profile);
            assertCondition(result.getTotalScore() == 70, "Total score is 70");
            assertCondition(result.isAllMandatorySatisfied(), "All mandatory satisfied");
            assertCondition(result.getScoreCategory() == ScoreCategory.NORMAL_VERIFICATION, "Category is NORMAL_VERIFICATION");

            recordPass("testEligibilityScoringNormalVerification");
        } catch (Exception e) {
            recordFail("testEligibilityScoringNormalVerification", e.getMessage());
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    private static void recordPass(String testName) {
        System.out.println("  [PASS] " + testName);
        passedTests++;
    }

    private static void recordFail(String testName, String reason) {
        System.out.println("  [FAIL] " + testName + ": " + reason);
        failedTests++;
    }
}
