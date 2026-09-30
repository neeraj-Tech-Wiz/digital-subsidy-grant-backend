package com.infosys.subsidy;

import com.infosys.subsidy.dto.MilestoneRequestDTO;
import com.infosys.subsidy.dto.PlanRequestDTO;
import com.infosys.subsidy.entity.*;
import com.infosys.subsidy.enums.*;
import com.infosys.subsidy.repository.*;
import com.infosys.subsidy.service.GrantService;
import com.infosys.subsidy.service.MilestoneComplianceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GrantServiceTest {

    @Mock
    private GrantDisbursementRepository grantDisbursementRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private SchemeRepository schemeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private DisbursementPlanRepository planRepository;
    @Mock
    private DisbursementMilestoneRepository milestoneRepository;
    @Mock
    private FundReleaseRepository fundReleaseRepository;
    @Mock
    private MilestoneComplianceService complianceService;

    @InjectMocks
    private GrantService grantService;

    private User officer;
    private User beneficiaryUser;
    private Application application;
    private Scheme scheme;

    @BeforeEach
    void setUp() {
        officer = new User();
        officer.setId(1L);
        officer.setEmail("grant@test.com");
        officer.setRole(UserRole.GRANT_OFFICER);

        beneficiaryUser = new User();
        beneficiaryUser.setId(2L);
        beneficiaryUser.setEmail("ben@test.com");
        beneficiaryUser.setRole(UserRole.BENEFICIARY);

        scheme = new Scheme();
        scheme.setId(5L);
        scheme.setGrantAmount(new BigDecimal("50000.00"));
        scheme.setTotalBudget(new BigDecimal("100000.00"));
        scheme.setDisbursedAmount(BigDecimal.ZERO);

        application = new Application();
        application.setId(10L);
        application.setBeneficiaryId(100L);
        application.setSchemeId(5L);
        application.setStatus(ApplicationStatus.APPROVED);
        application.setVerificationCompletedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void testCreatePlan_Success() {
        when(userRepository.findByEmail("grant@test.com")).thenReturn(Optional.of(officer));
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(planRepository.existsByApplicationId(10L)).thenReturn(false);
        when(schemeRepository.findById(5L)).thenReturn(Optional.of(scheme));

        PlanRequestDTO req = new PlanRequestDTO();
        List<MilestoneRequestDTO> ms = new ArrayList<>();
        
        MilestoneRequestDTO m1 = new MilestoneRequestDTO();
        m1.setMilestoneNumber(1);
        m1.setComplianceType(MilestoneComplianceType.DOCUMENTATION);
        m1.setScheduledAmount(new BigDecimal("20000.00"));
        ms.add(m1);

        MilestoneRequestDTO m2 = new MilestoneRequestDTO();
        m2.setMilestoneNumber(2);
        m2.setComplianceType(MilestoneComplianceType.GROUND_VERIFICATION);
        m2.setScheduledAmount(new BigDecimal("30000.00"));
        ms.add(m2);
        
        req.setMilestones(ms);

        when(planRepository.save(Mockito.<DisbursementPlan>any())).thenAnswer(i -> {
            DisbursementPlan saved = (DisbursementPlan) i.getArguments()[0];
            saved.setId(1000L);
            return saved;
        });

        DisbursementPlan plan = grantService.createDisbursementPlan(10L, req, "grant@test.com");

        assertNotNull(plan);
        assertEquals(new BigDecimal("50000.00"), plan.getTotalAmount());
        assertEquals(2, plan.getMilestones().size());
        verify(applicationRepository).save(application);
        assertEquals(ApplicationStatus.PLAN_CONFIGURED, application.getStatus());
        verify(complianceService, times(1)).evaluateDocumentationCompliance(any(), any(), any());
    }

    @Test
    void testCreatePlan_InvalidTotal() {
        when(userRepository.findByEmail("grant@test.com")).thenReturn(Optional.of(officer));
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(schemeRepository.findById(5L)).thenReturn(Optional.of(scheme));

        PlanRequestDTO req = new PlanRequestDTO();
        List<MilestoneRequestDTO> ms = new ArrayList<>();
        MilestoneRequestDTO m1 = new MilestoneRequestDTO();
        m1.setScheduledAmount(new BigDecimal("10000.00")); // Mismatch with 50000
        ms.add(m1);
        req.setMilestones(ms);

        assertThrows(ResponseStatusException.class, () -> 
            grantService.createDisbursementPlan(10L, req, "grant@test.com")
        );
    }

    @Test
    void testReleaseMilestone_Success() {
        when(userRepository.findByEmail("grant@test.com")).thenReturn(Optional.of(officer));
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        
        DisbursementPlan plan = new DisbursementPlan();
        plan.setId(1000L);
        plan.setApplicationId(10L);
        plan.setTotalAmount(new BigDecimal("50000.00"));
        plan.setStatus(PlanStatus.ACTIVE);
        when(planRepository.findByApplicationId(10L)).thenReturn(Optional.of(plan));

        DisbursementMilestone milestone = new DisbursementMilestone();
        milestone.setId(1L);
        milestone.setPlan(plan);
        milestone.setScheduledAmount(new BigDecimal("20000.00"));
        milestone.setStatus(MilestoneStatus.COMPLETED); // Compliance must be completed
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        when(fundReleaseRepository.existsByMilestoneId(1L)).thenReturn(false);
        when(fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(10L)).thenReturn(new ArrayList<>());
        when(schemeRepository.findByIdWithLock(5L)).thenReturn(Optional.of(scheme));
        
        when(fundReleaseRepository.save(any(FundRelease.class))).thenAnswer(i -> i.getArguments()[0]);

        FundRelease release = grantService.releaseMilestone(10L, 1L, "grant@test.com");
        
        assertNotNull(release);
        assertEquals(new BigDecimal("20000.00"), release.getAmount());
        assertEquals(MilestoneStatus.RELEASED, milestone.getStatus());
        assertEquals(new BigDecimal("20000.00"), scheme.getDisbursedAmount());
        assertEquals(ApplicationStatus.PARTIALLY_DISBURSED, application.getStatus());
    }

    @Test
    void testReleaseMilestone_DuplicateBlocked() {
        when(userRepository.findByEmail("grant@test.com")).thenReturn(Optional.of(officer));
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        
        DisbursementPlan plan = new DisbursementPlan();
        plan.setId(1000L);
        plan.setStatus(PlanStatus.ACTIVE);
        when(planRepository.findByApplicationId(10L)).thenReturn(Optional.of(plan));

        DisbursementMilestone milestone = new DisbursementMilestone();
        milestone.setId(1L);
        milestone.setPlan(plan);
        milestone.setStatus(MilestoneStatus.COMPLETED);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        when(fundReleaseRepository.existsByMilestoneId(1L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> 
            grantService.releaseMilestone(10L, 1L, "grant@test.com")
        );
    }
    
    @Test
    void testReleaseMilestone_ExceedsTotal() {
        when(userRepository.findByEmail("grant@test.com")).thenReturn(Optional.of(officer));
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        
        DisbursementPlan plan = new DisbursementPlan();
        plan.setId(1000L);
        plan.setApplicationId(10L);
        plan.setTotalAmount(new BigDecimal("50000.00"));
        plan.setStatus(PlanStatus.ACTIVE);
        when(planRepository.findByApplicationId(10L)).thenReturn(Optional.of(plan));

        DisbursementMilestone milestone = new DisbursementMilestone();
        milestone.setId(1L);
        milestone.setPlan(plan);
        milestone.setScheduledAmount(new BigDecimal("20000.00"));
        milestone.setStatus(MilestoneStatus.COMPLETED); // Compliance must be completed
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        when(fundReleaseRepository.existsByMilestoneId(1L)).thenReturn(false);
        
        FundRelease existingRelease = new FundRelease();
        existingRelease.setAmount(new BigDecimal("40000.00"));
        List<FundRelease> releases = List.of(existingRelease);
        
        when(fundReleaseRepository.findByApplicationIdOrderByReleasedAtAsc(10L)).thenReturn(releases);
        
        assertThrows(ResponseStatusException.class, () -> 
            grantService.releaseMilestone(10L, 1L, "grant@test.com")
        );
    }
}
