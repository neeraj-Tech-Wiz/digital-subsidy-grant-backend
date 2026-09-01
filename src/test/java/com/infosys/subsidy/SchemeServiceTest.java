package com.infosys.subsidy;

import com.infosys.subsidy.dto.EligibilityCriteriaRequest;
import com.infosys.subsidy.dto.SchemeRequest;
import com.infosys.subsidy.entity.EligibilityCriteria;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.enums.BeneficiaryCategory;
import com.infosys.subsidy.enums.CriterionType;
import com.infosys.subsidy.enums.Operator;
import com.infosys.subsidy.enums.SchemeStatus;
import com.infosys.subsidy.exception.DuplicateSchemeException;
import com.infosys.subsidy.exception.InvalidCriteriaException;
import com.infosys.subsidy.exception.SchemeNotFoundException;
import com.infosys.subsidy.exception.ValidationException;
import com.infosys.subsidy.repository.EligibilityCriteriaRepository;
import com.infosys.subsidy.repository.SchemeRepository;
import com.infosys.subsidy.service.SchemeService;
import com.infosys.subsidy.service.SchemeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemeServiceTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private EligibilityCriteriaRepository criteriaRepository;

    private SchemeService schemeService;

    @BeforeEach
    void setUp() {
        schemeService = new SchemeServiceImpl(schemeRepository, criteriaRepository);
    }

    @Test
    void testCreateScheme_Success() {
        SchemeRequest request = new SchemeRequest(
                "SCH-TEST-01",
                "Test Scheme",
                "Description of test scheme",
                10000.0,
                500000.0,
                SchemeStatus.DRAFT,
                "All India",
                BeneficiaryCategory.FARMER
        );

        when(schemeRepository.existsBySchemeCode("SCH-TEST-01")).thenReturn(false);
        when(schemeRepository.existsBySchemeName("Test Scheme")).thenReturn(false);
        when(schemeRepository.save(any(Scheme.class))).thenAnswer(invocation -> {
            Scheme s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(new Scheme(
                1L, "SCH-TEST-01", "Test Scheme", "Description of test scheme",
                10000.0, 500000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.FARMER
        )));

        Scheme created = schemeService.createScheme(request);

        assertNotNull(created);
        assertEquals("SCH-TEST-01", created.getSchemeCode());
        assertEquals("Test Scheme", created.getSchemeName());
        assertEquals(10000.0, created.getGrantAmount());
        assertEquals(500000.0, created.getTotalBudget());
        verify(schemeRepository, times(1)).save(any(Scheme.class));
    }

    @Test
    void testCreateScheme_DuplicateCode_ThrowsException() {
        SchemeRequest request = new SchemeRequest(
                "SCH-DUP-01", "Unique Name", "Desc",
                5000.0, 50000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.GENERAL
        );

        when(schemeRepository.existsBySchemeCode("SCH-DUP-01")).thenReturn(true);

        assertThrows(DuplicateSchemeException.class, () -> schemeService.createScheme(request));
    }

    @Test
    void testCreateScheme_InvalidBudget_ThrowsException() {
        SchemeRequest request = new SchemeRequest(
                "SCH-BAD-01", "Bad Budget Scheme", "Desc",
                50000.0, 10000.0, // Budget < Grant amount
                SchemeStatus.DRAFT, "All India", BeneficiaryCategory.GENERAL
        );

        assertThrows(ValidationException.class, () -> schemeService.createScheme(request));
    }

    @Test
    void testAddCriterionToScheme_Success() {
        Scheme scheme = new Scheme(1L, "SCH-01", "Scheme 1", "Desc", 5000.0, 50000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.FARMER);
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));

        EligibilityCriteriaRequest critReq = new EligibilityCriteriaRequest(
                "Annual Income", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "250000", 30, true, true, "Income check"
        );

        when(criteriaRepository.save(any(EligibilityCriteria.class))).thenAnswer(invocation -> {
            EligibilityCriteria c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        EligibilityCriteria created = schemeService.addCriterionToScheme(1L, critReq);

        assertNotNull(created);
        assertEquals(10L, created.getId());
        assertEquals("Annual Income", created.getCriterionName());
        assertEquals(30, created.getWeight());
        assertTrue(created.isMandatory());
    }

    @Test
    void testAddCriterion_DuplicateNameInScheme_ThrowsException() {
        Scheme scheme = new Scheme(1L, "SCH-01", "Scheme 1", "Desc", 5000.0, 50000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.FARMER);
        EligibilityCriteria existing = new EligibilityCriteria(5L, "Income Limit", CriterionType.NUMERIC, Operator.LESS_THAN, "200000", 20, 20, true, true, "");
        scheme.addCriterion(existing);

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));

        EligibilityCriteriaRequest critReq = new EligibilityCriteriaRequest(
                "Income Limit", CriterionType.NUMERIC, Operator.LESS_THAN_EQUAL, "250000", 30, true, true, ""
        );

        assertThrows(InvalidCriteriaException.class, () -> schemeService.addCriterionToScheme(1L, critReq));
    }

    @Test
    void testChangeSchemeStatus_ActiveWithoutCriteria_ThrowsException() {
        Scheme scheme = new Scheme(1L, "SCH-01", "Scheme 1", "Desc", 5000.0, 50000.0, SchemeStatus.DRAFT, "All India", BeneficiaryCategory.FARMER);
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));

        assertThrows(ValidationException.class, () -> schemeService.changeSchemeStatus(1L, SchemeStatus.ACTIVE));
    }

    @Test
    void testRecordDisbursement_Success() {
        Scheme scheme = new Scheme(1L, "SCH-01", "Scheme 1", "Desc", 5000.0, 50000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.FARMER);
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(schemeRepository.save(any(Scheme.class))).thenAnswer(i -> i.getArgument(0));

        Scheme result = schemeService.recordDisbursement(1L, 15000.0);

        assertEquals(15000.0, result.getDisbursedAmount());
        assertEquals(35000.0, result.getRemainingBudget());
    }

    @Test
    void testRecordDisbursement_ExceedsBudget_ThrowsException() {
        Scheme scheme = new Scheme(1L, "SCH-01", "Scheme 1", "Desc", 5000.0, 50000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.FARMER);
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));

        assertThrows(ValidationException.class, () -> schemeService.recordDisbursement(1L, 60000.0));
    }
}
