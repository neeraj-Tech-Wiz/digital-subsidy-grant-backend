package com.infosys.subsidy;

import com.infosys.subsidy.dto.EligibilityEvaluationRequest;
import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.enums.BeneficiaryCategory;
import com.infosys.subsidy.enums.CriterionType;
import com.infosys.subsidy.enums.Operator;
import com.infosys.subsidy.enums.SchemeStatus;
import com.infosys.subsidy.enums.ScoreCategory;
import com.infosys.subsidy.model.BeneficiaryProfile;
import com.infosys.subsidy.model.EligibilityEvaluationResult;
import com.infosys.subsidy.service.EligibilityEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EligibilityEvaluationTest {

    private EligibilityEvaluationService evaluationService;
    private Scheme testScheme;

    @BeforeEach
    void setUp() {
        evaluationService = new EligibilityEvaluationService();

        testScheme = new Scheme(1L, "SCH-AGRI-01", "PM Agri Scheme", "Support farmers", 10000.0, 1000000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.FARMER);

        testScheme.addCriterion(new EligibilityCriteria(
                1L, "Target Category", CriterionType.ENUM, Operator.EQUAL, "FARMER", 25, 25, true, true, ""
        ));
        testScheme.addCriterion(new EligibilityCriteria(
                2L, "Max Land Holding", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "5.0", 25, 25, true, true, ""
        ));
        testScheme.addCriterion(new EligibilityCriteria(
                3L, "Annual Income Limit", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "250000", 25, 25, false, true, ""
        ));
        testScheme.addCriterion(new EligibilityCriteria(
                4L, "Aadhaar KYC Verification", CriterionType.BOOLEAN, Operator.EQUAL, "true", 25, 25, true, true, ""
        ));
    }

    @Test
    void testEvaluate_FullScore_Eligible() {
        BeneficiaryProfile profile = new BeneficiaryProfile(
                1L, "Rajesh Kumar", 42, "123456789012", "9876543210",
                "rajesh@test.com", 180000.0, 3.0, BeneficiaryCategory.FARMER, "All India"
        );
        profile.setAadhaarVerified(true);
        profile.setDocumentsVerified(true);

        EligibilityEvaluationResult report = evaluationService.evaluate(testScheme, profile);

        assertNotNull(report);
        assertEquals(100, report.getTotalScore());
        assertEquals(100, report.getMaxPossibleScore());
        assertTrue(report.isAllMandatorySatisfied());
        assertEquals(ScoreCategory.HIGH_PRIORITY_DIRECT_APPROVAL, report.getScoreCategory());
        assertTrue(report.isEligible());
    }

    @Test
    void testEvaluate_FailedMandatory_Ineligible() {
        // Applicant is STUDENT not FARMER (fails mandatory category rule)
        BeneficiaryProfile profile = new BeneficiaryProfile(
                2L, "Sunil Sen", 22, "987654321098", "9123456780",
                "sunil@test.com", 50000.0, 1.0, BeneficiaryCategory.STUDENT, "All India"
        );
        profile.setAadhaarVerified(true);

        EligibilityEvaluationResult report = evaluationService.evaluate(testScheme, profile);

        assertNotNull(report);
        assertFalse(report.isAllMandatorySatisfied());
        assertEquals(ScoreCategory.INELIGIBLE, report.getScoreCategory());
        assertFalse(report.isEligible());
    }

    @Test
    void testEvaluate_RequestDTO_Evaluation() {
        EligibilityEvaluationRequest req = new EligibilityEvaluationRequest(
                "Anita Sharma", 35, "112233445566", "9988776655",
                "anita@test.com", 200000.0, 2.5, BeneficiaryCategory.FARMER, "All India"
        );
        req.setAadhaarVerified(true);
        req.setDocumentsVerified(true);

        EligibilityEvaluationResult report = evaluationService.evaluate(testScheme, req);

        assertNotNull(report);
        assertEquals("Anita Sharma", report.getBeneficiaryName());
        assertTrue(report.getTotalScore() >= 80);
        assertEquals(ScoreCategory.HIGH_PRIORITY_DIRECT_APPROVAL, report.getScoreCategory());
    }
}
