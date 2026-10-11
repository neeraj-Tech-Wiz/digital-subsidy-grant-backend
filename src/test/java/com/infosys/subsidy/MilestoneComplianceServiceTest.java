package com.infosys.subsidy;

import com.infosys.subsidy.entity.*;
import com.infosys.subsidy.enums.*;
import com.infosys.subsidy.repository.*;
import com.infosys.subsidy.service.MilestoneComplianceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.infosys.subsidy.service.DocumentStorageService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
public class MilestoneComplianceServiceTest {

    @Mock
    private DisbursementMilestoneRepository milestoneRepository;
    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;
    @Mock
    private SchemeRequiredDocumentRepository schemeRequiredDocumentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private VerificationHistoryRepository verificationHistoryRepository;
    @Mock
    private MilestoneComplianceEvidenceRepository evidenceRepository;
    @Mock
    private DocumentStorageService documentStorageService;

    @InjectMocks
    private MilestoneComplianceService complianceService;

    private DisbursementMilestone docMilestone;
    private SchemeRequiredDocument mandatoryDoc1;
    private ApplicationDocument appDoc1;
    private ApplicationDocument passbookDoc;
    private Application application;
    private Beneficiary beneficiary;

    @BeforeEach
    void setUp() {
        docMilestone = new DisbursementMilestone();
        docMilestone.setId(1L);
        docMilestone.setComplianceType(MilestoneComplianceType.DOCUMENTATION);
        docMilestone.setStatus(MilestoneStatus.PENDING);

        mandatoryDoc1 = new SchemeRequiredDocument();
        mandatoryDoc1.setDocumentType(DocumentType.AADHAAR_CARD);
        mandatoryDoc1.setMandatory(true);

        appDoc1 = new ApplicationDocument();
        appDoc1.setDocumentType(DocumentType.AADHAAR_CARD);
        appDoc1.setDocumentStatus(DocumentStatus.VERIFIED);
        
        passbookDoc = new ApplicationDocument();
        passbookDoc.setDocumentType(DocumentType.BANK_PASSBOOK);
        passbookDoc.setDocumentStatus(DocumentStatus.VERIFIED);
        
        application = new Application();
        application.setId(10L);
        application.setBeneficiaryId(100L);
        
        beneficiary = new Beneficiary();
        beneficiary.setId(100L);
        beneficiary.setBankAccountNumber("1234567890");
        beneficiary.setBankIfscCode("SBIN0001234");
        beneficiary.setBankAccountHolderName("John Doe");
    }

    @Test
    void testDocumentationCompliance_AutoCompletes() {
        when(schemeRequiredDocumentRepository.findBySchemeIdAndActiveTrue(5L))
            .thenReturn(List.of(mandatoryDoc1));
        when(applicationDocumentRepository.findByApplicationId(10L))
            .thenReturn(List.of(appDoc1, passbookDoc));
        when(applicationRepository.findById(10L))
            .thenReturn(Optional.of(application));
        when(beneficiaryRepository.findById(100L))
            .thenReturn(Optional.of(beneficiary));

        complianceService.evaluateDocumentationCompliance(docMilestone, 10L, 5L);

        assertEquals(MilestoneStatus.COMPLETED, docMilestone.getStatus());
        assertNotNull(docMilestone.getComplianceVerifiedAt());
        verify(milestoneRepository).save(docMilestone);
    }

    @Test
    void testDocumentationCompliance_BlockedMissingDoc() {
        when(schemeRequiredDocumentRepository.findBySchemeIdAndActiveTrue(5L))
            .thenReturn(List.of(mandatoryDoc1));
        when(applicationDocumentRepository.findByApplicationId(10L))
            .thenReturn(List.of()); // Doc is missing
            
        // We do not strictly need application mocks because it breaks early at first missing document (AADHAAR).
        // Let's just run it.

        complianceService.evaluateDocumentationCompliance(docMilestone, 10L, 5L);

        assertEquals(MilestoneStatus.BLOCKED, docMilestone.getStatus());
        verify(milestoneRepository).save(docMilestone);
    }

    @Test
    void testDocumentationCompliance_BlockedNotVerified() {
        appDoc1.setDocumentStatus(DocumentStatus.UPLOADED);
        
        when(schemeRequiredDocumentRepository.findBySchemeIdAndActiveTrue(5L))
            .thenReturn(List.of(mandatoryDoc1));
        when(applicationDocumentRepository.findByApplicationId(10L))
            .thenReturn(List.of(appDoc1));

        complianceService.evaluateDocumentationCompliance(docMilestone, 10L, 5L);

        assertEquals(MilestoneStatus.BLOCKED, docMilestone.getStatus());
        verify(milestoneRepository).save(docMilestone);
    }

    @Test
    void testFirstUtilizationEvidenceSubmitted() {
        DisbursementMilestone mst = new DisbursementMilestone();
        mst.setId(22L);
        mst.setStatus(MilestoneStatus.PENDING);
        mst.setComplianceType(MilestoneComplianceType.UTILIZATION_PROOF);
        DisbursementPlan plan = new DisbursementPlan();
        plan.setApplicationId(10L);
        mst.setPlan(plan);
        
        User submitter = new User();
        submitter.setId(10L);
        submitter.setEmail("ben@test.com");
        
        when(userRepository.findByEmail("ben@test.com")).thenReturn(Optional.of(submitter));
        when(milestoneRepository.findById(22L)).thenReturn(Optional.of(mst));
        when(milestoneRepository.save(any(DisbursementMilestone.class))).thenReturn(mst);
        
        List<MultipartFile> files = List.of(new MockMultipartFile("f", "test.xml", "text/xml", "a".getBytes()));
        List<String> types = List.of("INVOICE");
        
        complianceService.submitComplianceEvidenceFiles(22L, files, types, "First submission", "ben@test.com");
        
        ArgumentCaptor<VerificationHistory> captor = ArgumentCaptor.forClass(VerificationHistory.class);
        verify(verificationHistoryRepository).save(captor.capture());
        assertEquals("UTILIZATION_PROOF_SUBMITTED", captor.getValue().getAction());
    }

    @Test
    void testCorrectedUtilizationEvidenceResubmitted() {
        DisbursementMilestone mst = new DisbursementMilestone();
        mst.setId(22L);
        mst.setStatus(MilestoneStatus.NON_COMPLIANT);
        mst.setComplianceType(MilestoneComplianceType.UTILIZATION_PROOF);
        DisbursementPlan plan = new DisbursementPlan();
        plan.setApplicationId(10L);
        mst.setPlan(plan);
        
        User submitter = new User();
        submitter.setId(10L);
        submitter.setEmail("ben@test.com");
        
        when(userRepository.findByEmail("ben@test.com")).thenReturn(Optional.of(submitter));
        when(milestoneRepository.findById(22L)).thenReturn(Optional.of(mst));
        when(milestoneRepository.save(any(DisbursementMilestone.class))).thenReturn(mst);
        
        List<MultipartFile> files = List.of(new MockMultipartFile("f", "test.xml", "text/xml", "a".getBytes()));
        List<String> types = List.of("INVOICE");
        
        complianceService.submitComplianceEvidenceFiles(22L, files, types, "Resubmission", "ben@test.com");
        
        ArgumentCaptor<VerificationHistory> captor = ArgumentCaptor.forClass(VerificationHistory.class);
        verify(verificationHistoryRepository).save(captor.capture());
        assertEquals("UTILIZATION_PROOF_RESUBMITTED", captor.getValue().getAction());
        assertEquals(MilestoneStatus.COMPLIANCE_SUBMITTED.name(), captor.getValue().getNewStatus());
    }
}
