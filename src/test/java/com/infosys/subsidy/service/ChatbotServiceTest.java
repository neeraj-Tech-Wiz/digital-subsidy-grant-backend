package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.ChatbotRequest;
import com.infosys.subsidy.dto.ChatbotResponse;
import com.infosys.subsidy.entity.*;
import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.MilestoneStatus;
import com.infosys.subsidy.enums.SchemeStatus;
import com.infosys.subsidy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatbotServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private SchemeRepository schemeRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private SchemeRequiredDocumentRepository documentRepository;
    @Mock private DisbursementPlanRepository planRepository;
    @Mock private FundReleaseRepository fundReleaseRepository;

    @InjectMocks
    private ChatbotService chatbotService;

    private User testUser;
    private Beneficiary testBeneficiary;
    private Scheme testScheme;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@beneficiary.com");

        testBeneficiary = new Beneficiary();
        testBeneficiary.setId(5L);
        testBeneficiary.setUser(testUser);

        testScheme = new Scheme();
        testScheme.setId(10L);
        testScheme.setSchemeName("Solar Subsidy");
        testScheme.setSchemeCode("SOLAR");
        testScheme.setStatus(SchemeStatus.ACTIVE);
    }

    @Test
    void testUnknownUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("hello"), "missing@user.com");
        assertTrue(response.getReply().contains("could not be found"));
    }

    @Test
    void testMissingBeneficiaryContext() {
        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.empty());
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("hello"), "test@beneficiary.com");
        assertEquals("PROFILE_MISSING", response.getActionMetadata());
    }

    @Test
    void testSchemesIntent() {
        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        when(schemeRepository.findByStatus(SchemeStatus.ACTIVE)).thenReturn(List.of(testScheme));
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("what schemes are available?"), "test@beneficiary.com");
        assertEquals("SCHEMES", response.getActionMetadata());
        assertTrue(response.getReply().contains("Solar Subsidy"));
    }

    @Test
    void testEligibilityIntent_SpecificScheme() {
        EligibilityCriteria criteria = new EligibilityCriteria();
        criteria.setCriterionName("Income Below 3L");
        criteria.setMandatory(true);
        criteria.setOperator(com.infosys.subsidy.enums.Operator.LESS_THAN_EQUAL);
        criteria.setExpectedValue("300000");
        testScheme.setCriteriaList(List.of(criteria));
        
        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        when(schemeRepository.findByStatus(SchemeStatus.ACTIVE)).thenReturn(List.of(testScheme));
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("eligibility for SOLAR"), "test@beneficiary.com");
        assertEquals("ELIGIBILITY", response.getActionMetadata());
        assertTrue(response.getReply().contains("Income Below 3L"));
    }
    
    @Test
    void testEligibilityIntent_ContextClarification() {
        EligibilityCriteria criteria = new EligibilityCriteria();
        criteria.setCriterionName("Income Below 3L");
        criteria.setMandatory(true);
        criteria.setOperator(com.infosys.subsidy.enums.Operator.LESS_THAN_EQUAL);
        criteria.setExpectedValue("300000");
        testScheme.setCriteriaList(List.of(criteria));
        
        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        when(schemeRepository.findByStatus(SchemeStatus.ACTIVE)).thenReturn(List.of(testScheme));
        
        ChatbotResponse clarifyResponse = chatbotService.processMessage(new ChatbotRequest("what is the eligibility rules?"), "test@beneficiary.com");
        assertEquals("CLARIFY_SCHEME", clarifyResponse.getActionMetadata());
        
        ChatbotResponse followUpResponse = chatbotService.processMessage(new ChatbotRequest("SOLAR"), "test@beneficiary.com");
        assertEquals("ELIGIBILITY", followUpResponse.getActionMetadata());
    }

    @Test
    void testMyApplicationIntent_Empty() {
        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        when(applicationRepository.findByBeneficiaryId(5L)).thenReturn(Collections.emptyList());
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("my application status"), "test@beneficiary.com");
        assertEquals("MY_APPLICATION_EMPTY", response.getActionMetadata());
    }
    
    @Test
    void testMyApplicationIntent_Found() {
        Application app = new Application();
        app.setId(64L);
        app.setSchemeId(10L);
        app.setStatus(ApplicationStatus.UNDER_VERIFICATION);
        app.setBeneficiaryId(5L);
        
        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        when(applicationRepository.findByBeneficiaryId(5L)).thenReturn(List.of(app));
        when(schemeRepository.findById(10L)).thenReturn(Optional.of(testScheme));
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("status of my application"), "test@beneficiary.com");
        assertEquals("MY_APPLICATION", response.getActionMetadata());
        assertTrue(response.getReply().contains("Under Verification"));
        assertTrue(response.getReply().contains("APP-00064"));
    }

    @Test
    void testMilestonesIntent_NoPlan() {
        Application app = new Application();
        app.setId(100L);
        app.setStatus(ApplicationStatus.PARTIALLY_DISBURSED);
        app.setBeneficiaryId(5L);
        
        lenient().when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        lenient().when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        lenient().when(applicationRepository.findById(100L)).thenReturn(Optional.of(app));
        lenient().when(planRepository.findByApplicationId(100L)).thenReturn(Optional.empty());
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("grant milestones of APP-00100"), "test@beneficiary.com");
        assertTrue(response.getReply().contains("doesn't have a generated disbursement plan"));
    }
    
    @Test
    void testMilestonesIntent_WithPlan() {
        Application app = new Application();
        app.setId(100L);
        app.setStatus(ApplicationStatus.PARTIALLY_DISBURSED);
        app.setBeneficiaryId(5L);
        
        DisbursementPlan plan = new DisbursementPlan();
        plan.setTotalAmount(new BigDecimal("10000"));
        DisbursementMilestone m = new DisbursementMilestone();
        m.setMilestoneName("Installation");
        m.setStatus(MilestoneStatus.DUE);
        m.setComplianceType(com.infosys.subsidy.enums.MilestoneComplianceType.DOCUMENTATION);
        plan.setMilestones(List.of(m));
        
        lenient().when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        lenient().when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        lenient().when(applicationRepository.findByBeneficiaryId(5L)).thenReturn(List.of(app));
        lenient().when(applicationRepository.findById(100L)).thenReturn(Optional.of(app));
        lenient().when(planRepository.findByApplicationId(100L)).thenReturn(Optional.of(plan));
        
        FundRelease release = new FundRelease();
        release.setAmount(new BigDecimal("5000"));
        lenient().when(fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(100L)).thenReturn(List.of(release));
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("milestones of APP-00100"), "test@beneficiary.com");
        assertEquals("MILESTONES", response.getActionMetadata());
        assertTrue(response.getReply().contains("10000"));
        assertTrue(response.getReply().contains("5000"));
        assertTrue(response.getReply().contains("DOCUMENTATION"));
    }

    @Test
    void testMilestonesIntent_AscendingOrder() {
        Application app = new Application();
        app.setId(101L);
        app.setStatus(ApplicationStatus.PARTIALLY_DISBURSED);
        app.setBeneficiaryId(5L);
        
        DisbursementPlan plan = new DisbursementPlan();
        plan.setTotalAmount(new BigDecimal("20000"));
        
        DisbursementMilestone m2 = new DisbursementMilestone();
        m2.setMilestoneNumber(2);
        m2.setMilestoneName("Installation");
        m2.setStatus(MilestoneStatus.PENDING);
        m2.setScheduledAmount(new BigDecimal("10000"));
        m2.setComplianceType(com.infosys.subsidy.enums.MilestoneComplianceType.DOCUMENTATION);

        DisbursementMilestone m1 = new DisbursementMilestone();
        m1.setMilestoneNumber(1);
        m1.setMilestoneName("Advance");
        m1.setStatus(MilestoneStatus.COMPLIANCE_SUBMITTED);
        m1.setScheduledAmount(new BigDecimal("10000"));
        
        plan.setMilestones(List.of(m2, m1));
        
        lenient().when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        lenient().when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        lenient().when(applicationRepository.findByBeneficiaryId(5L)).thenReturn(List.of(app));
        lenient().when(applicationRepository.findById(101L)).thenReturn(Optional.of(app));
        lenient().when(planRepository.findByApplicationId(101L)).thenReturn(Optional.of(plan));
        
        FundRelease release = new FundRelease();
        release.setAmount(new BigDecimal("10000"));
        lenient().when(fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(101L)).thenReturn(List.of(release));
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("grant milestones of APP-00101"), "test@beneficiary.com");
        
        assertTrue(response.getReply().indexOf("1. Stage 1 — Advance") < response.getReply().indexOf("2. Stage 2 — Installation"));
        assertTrue(response.getReply().contains("Compliance submitted"));
        assertTrue(response.getReply().contains("Remaining not yet released: ₹10000"));
    }

    @Test
    void testMissingComplianceNone_Suppressed() {
        Application app = new Application();
        app.setId(102L);
        app.setStatus(ApplicationStatus.GRANT_DISBURSED);
        app.setBeneficiaryId(5L);
        
        DisbursementPlan plan = new DisbursementPlan();
        plan.setTotalAmount(new BigDecimal("20000"));
        
        DisbursementMilestone m1 = new DisbursementMilestone();
        m1.setMilestoneNumber(1);
        m1.setMilestoneName("Advance");
        m1.setStatus(MilestoneStatus.DUE);
        m1.setScheduledAmount(new BigDecimal("20000"));
        
        // Explicitly setting NONE compliance for a DUE milestone which otherwise prompts Action Required
        m1.setComplianceType(com.infosys.subsidy.enums.MilestoneComplianceType.NONE);
        plan.setMilestones(List.of(m1));
        
        lenient().when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        lenient().when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        lenient().when(applicationRepository.findByBeneficiaryId(5L)).thenReturn(List.of(app));
        lenient().when(applicationRepository.findById(102L)).thenReturn(Optional.of(app));
        lenient().when(planRepository.findByApplicationId(102L)).thenReturn(Optional.of(plan));
        lenient().when(fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(102L)).thenReturn(Collections.emptyList());
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("grant milestones of APP-00102"), "test@beneficiary.com");
        assertFalse(response.getReply().contains("Action Required: Submit"));
    }

    @Test
    void testBothIntent_EligibilityAndDocuments() {
        EligibilityCriteria criteria = new EligibilityCriteria();
        criteria.setCriterionName("Income Below 3L");
        criteria.setMandatory(true);
        criteria.setOperator(com.infosys.subsidy.enums.Operator.LESS_THAN_EQUAL);
        criteria.setExpectedValue("300000");
        testScheme.setCriteriaList(List.of(criteria));

        SchemeRequiredDocument doc = new SchemeRequiredDocument();
        doc.setDocumentName("Aadhaar Card");
        doc.setMandatory(true);

        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        when(schemeRepository.findByStatus(SchemeStatus.ACTIVE)).thenReturn(List.of(testScheme));
        when(documentRepository.findBySchemeIdAndActiveTrue(10L)).thenReturn(List.of(doc));

        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("eligibility and documents for SOLAR"), "test@beneficiary.com");
        
        assertEquals("ELIGIBILITY_AND_DOCUMENTS", response.getActionMetadata());
        assertTrue(response.getReply().contains("must not exceed"));
        assertTrue(response.getReply().replace(",", "").contains("300000"));
        assertTrue(response.getReply().contains("Aadhaar Card"));
    }

    @Test
    void testStandaloneSchemeCodeContext() {
        EligibilityCriteria criteria = new EligibilityCriteria();
        criteria.setCriterionName("Income Below 3L");
        testScheme.setCriteriaList(List.of(criteria));
        
        SchemeRequiredDocument doc = new SchemeRequiredDocument();
        doc.setDocumentName("Aadhaar Card");
        doc.setMandatory(true);
        
        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        when(schemeRepository.findByStatus(SchemeStatus.ACTIVE)).thenReturn(List.of(testScheme));
        lenient().when(documentRepository.findBySchemeIdAndActiveTrue(10L)).thenReturn(List.of(doc));

        // Testing the context retention: send "DOCUMENTS" question, then a standalone Scheme Code
        ChatbotResponse first = chatbotService.processMessage(new ChatbotRequest("what are the documents?"), "test@beneficiary.com");
        assertEquals("CLARIFY_SCHEME", first.getActionMetadata());

        ChatbotResponse second = chatbotService.processMessage(new ChatbotRequest("SOLAR"), "test@beneficiary.com");
        assertEquals("DOCUMENTS", second.getActionMetadata());
        
        // Testing Standalone without context
        ChatbotResponse third = chatbotService.processMessage(new ChatbotRequest("SOLAR"), "test@beneficiary.com");
        assertEquals("SCHEME_INFO", third.getActionMetadata());
    }

    @Test
    void testPartialDisbursementSummary() {
        Application app = new Application();
        app.setId(105L);
        app.setSchemeId(10L);
        app.setStatus(ApplicationStatus.PARTIALLY_DISBURSED);
        app.setBeneficiaryId(5L);
        
        DisbursementPlan plan = new DisbursementPlan();
        plan.setTotalAmount(new BigDecimal("60000"));
        
        FundRelease release = new FundRelease();
        release.setAmount(new BigDecimal("20000"));

        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        when(applicationRepository.findById(105L)).thenReturn(Optional.of(app));
        when(schemeRepository.findById(10L)).thenReturn(Optional.of(testScheme));
        lenient().when(planRepository.findByApplicationId(105L)).thenReturn(Optional.of(plan));
        lenient().when(fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(105L)).thenReturn(List.of(release));
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("status of APP-00105"), "test@beneficiary.com");
        
        assertTrue(response.getReply().contains("Scheduled Grant: ₹60000"));
        assertTrue(response.getReply().contains("Released Funds: ₹20000"));
        assertTrue(response.getReply().contains("Disbursement is currently partial"));
        assertFalse(response.getReply().contains("completed"));
    }

    @Test
    void testOperatorReadableTranslation() {
        EligibilityCriteria criteria = new EligibilityCriteria();
        criteria.setCriterionName("Income Level");
        criteria.setOperator(com.infosys.subsidy.enums.Operator.GREATER_THAN_EQUAL);
        criteria.setExpectedValue("500000");
        
        EligibilityCriteria textMatch = new EligibilityCriteria();
        textMatch.setCriterionName("Education");
        textMatch.setOperator(com.infosys.subsidy.enums.Operator.EQUAL);
        textMatch.setExpectedValue("UNDERGRADUATE");

        testScheme.setCriteriaList(List.of(criteria, textMatch));

        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        when(schemeRepository.findByStatus(SchemeStatus.ACTIVE)).thenReturn(List.of(testScheme));
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("eligibility rules for SOLAR"), "test@beneficiary.com");
        assertTrue(response.getReply().contains("must be at least"));
        assertTrue(response.getReply().replace(",", "").contains("500000"));
        assertTrue(response.getReply().contains("Education must be UNDERGRADUATE"));
    }

    @Test
    void testOptionalDocumentHandling() {
        SchemeRequiredDocument docMandatory = new SchemeRequiredDocument();
        docMandatory.setDocumentName("Aadhaar");
        docMandatory.setMandatory(true);

        SchemeRequiredDocument docOptional = new SchemeRequiredDocument();
        docOptional.setDocumentName("Caste Certificate");
        docOptional.setMandatory(false);

        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        when(schemeRepository.findByStatus(SchemeStatus.ACTIVE)).thenReturn(List.of(testScheme));
        when(documentRepository.findBySchemeIdAndActiveTrue(10L)).thenReturn(List.of(docMandatory, docOptional));

        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("documents for SOLAR"), "test@beneficiary.com");
        
        assertTrue(response.getReply().contains("**Mandatory**: Aadhaar"));
        assertTrue(response.getReply().contains("**Optional**: Caste Certificate"));
    }

    @Test
    void testFallbackIntent() {
        when(userRepository.findByEmail("test@beneficiary.com")).thenReturn(Optional.of(testUser));
        when(beneficiaryRepository.findByUserId(1L)).thenReturn(Optional.of(testBeneficiary));
        
        ChatbotResponse response = chatbotService.processMessage(new ChatbotRequest("what is the weather like today"), "test@beneficiary.com");
        assertEquals("FALLBACK", response.getActionMetadata());
        assertTrue(response.getReply().contains("couldn't understand"));
    }
}
