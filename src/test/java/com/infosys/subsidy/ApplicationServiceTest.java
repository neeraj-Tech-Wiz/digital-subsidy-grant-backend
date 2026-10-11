package com.infosys.subsidy;

import com.infosys.subsidy.entity.*;
import com.infosys.subsidy.enums.*;
import com.infosys.subsidy.repository.*;
import com.infosys.subsidy.service.*;
import com.infosys.subsidy.dto.ApplicationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private SchemeRepository schemeRepository;
    @Mock
    private ApplicationEligibilityDataRepository eligibilityDataRepository;
    @Mock
    private DocumentValidationService documentValidationService;
    @Mock
    private VerificationRoutingService verificationRoutingService;
    @Mock
    private EligibilityService eligibilityService;
    @Mock
    private VerificationHistoryRepository verificationHistoryRepository;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void testEligibilityScoredOnInitialApply() {
        Beneficiary ben = new Beneficiary();
        ben.setId(10L);
        User u = new User();
        u.setId(5L);
        ben.setUser(u);
        
        Scheme scheme = new Scheme();
        scheme.setId(3L);
        
        EligibilityCriteria c = new EligibilityCriteria();
        c.setActive(true);
        c.setFieldName("income");
        c.setWeight(70);
        c.setMandatory(true);
        scheme.setCriteriaList(List.of(c));
        
        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(ben));
        when(schemeRepository.findById(3L)).thenReturn(Optional.of(scheme));
        when(eligibilityService.evaluateCriterion(any(), any())).thenReturn(true);
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> {
            Application a = i.getArgument(0);
            if (a.getId() == null) a.setId(99L);
            return a;
        });

        ApplicationRequest req = new ApplicationRequest();
        req.setEligibilityData(Map.of("income", "50000"));

        applicationService.applyForScheme(10L, 3L, req);

        ArgumentCaptor<VerificationHistory> captor = ArgumentCaptor.forClass(VerificationHistory.class);
        verify(verificationHistoryRepository).save(captor.capture());
        
        assertEquals("ELIGIBILITY_SCORED", captor.getValue().getAction());
    }

    @Test
    void testResubmittedAndRescored() {
        Application app = new Application();
        app.setId(99L);
        app.setSchemeId(3L);
        app.setBeneficiaryId(10L);
        app.setStatus(ApplicationStatus.RETURNED_TO_APPLICANT);
        
        User actor = new User();
        actor.setId(5L);
        actor.setEmail("ben@test.com");
        
        Beneficiary ben = new Beneficiary();
        ben.setId(10L);
        ben.setUser(actor);

        Scheme scheme = new Scheme();
        scheme.setId(3L);
        scheme.setGrantAmount(BigDecimal.valueOf(1000));
        
        EligibilityCriteria c = new EligibilityCriteria();
        c.setActive(true);
        c.setFieldName("income");
        c.setWeight(70);
        c.setMandatory(true);
        scheme.setCriteriaList(List.of(c));

        lenient().when(applicationRepository.findById(99L)).thenReturn(Optional.of(app));
        lenient().when(userRepository.findByEmail("ben@test.com")).thenReturn(Optional.of(actor));
        lenient().when(beneficiaryRepository.findByUserId(5L)).thenReturn(Optional.of(ben));
        lenient().when(documentValidationService.validateMandatoryDocumentsUploaded(3L, 99L)).thenReturn(true);
        lenient().when(schemeRepository.findById(3L)).thenReturn(Optional.of(scheme));
        lenient().when(eligibilityService.evaluateCriterion(any(), any())).thenReturn(true);
        lenient().when(verificationRoutingService.determineRoute(anyInt(), anyInt(), anyDouble()))
            .thenReturn(VerificationRoute.STANDARD);
        lenient().when(applicationRepository.save(any(Application.class))).thenReturn(app);

        ApplicationRequest req = new ApplicationRequest();
        req.setEligibilityData(Map.of("income", "50000"));

        applicationService.resubmitApplication(99L, req, "ben@test.com");

        ArgumentCaptor<VerificationHistory> captor = ArgumentCaptor.forClass(VerificationHistory.class);
        verify(verificationHistoryRepository, times(2)).save(captor.capture());
        
        List<VerificationHistory> savedEvents = captor.getAllValues();
        assertEquals(2, savedEvents.size(), "Should save precisely 2 events on resubmit: RESUBMITTED and ELIGIBILITY_SCORED");
        
        assertEquals("RESUBMITTED", savedEvents.get(0).getAction());
        assertEquals("RETURNED_TO_APPLICANT", savedEvents.get(0).getPreviousStatus());
        assertEquals("PENDING_VERIFICATION", savedEvents.get(0).getNewStatus());
        
        assertEquals("ELIGIBILITY_SCORED", savedEvents.get(1).getAction());
        assertEquals("PENDING_VERIFICATION", savedEvents.get(1).getPreviousStatus());
        assertEquals("PENDING_VERIFICATION", savedEvents.get(1).getNewStatus());
    }
}
