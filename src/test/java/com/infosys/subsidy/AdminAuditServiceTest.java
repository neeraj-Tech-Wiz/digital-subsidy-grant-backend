package com.infosys.subsidy;

import com.infosys.subsidy.service.AdminAuditService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminAuditServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query countQuery;

    @Mock
    private Query dataQuery;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    private AdminAuditService adminAuditService;

    @BeforeEach
    void setUp() {
        adminAuditService = new AdminAuditService(entityManager);
        
        lenient().when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.startsWith("SELECT COUNT")) {
                return countQuery;
            } else {
                return dataQuery;
            }
        });
        
        lenient().when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        lenient().when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        
        // Default returning 0 records
        lenient().when(countQuery.getSingleResult()).thenReturn(0L);
        lenient().when(dataQuery.getResultList()).thenReturn(Collections.emptyList());
    }

    @Test
    void testAuthenticationCategory() {
        adminAuditService.getUnifiedAuditLogs("AUTHENTICATION", null, null, null, null, null, null, 0, 10);
        
        verify(entityManager, times(2)).createNativeQuery(sqlCaptor.capture());
        String dataSql = sqlCaptor.getAllValues().get(1);
        
        assertTrue(dataSql.contains("event_type IN :systemEvents"));
        assertTrue(dataSql.contains("AND 1=0")); // For VerificationHistory, which shouldn't have authentication
        
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(dataQuery).setParameter(eq("systemEvents"), listCaptor.capture());
        
        List<String> sysEvents = listCaptor.getValue();
        assertTrue(sysEvents.contains("LOGIN_SUCCESS"));
        assertTrue(sysEvents.contains("LOGIN_FAILED"));
        assertTrue(sysEvents.contains("LOGOUT"));
    }

    @Test
    void testApplicationCategory() {
        adminAuditService.getUnifiedAuditLogs("APPLICATION", null, null, null, null, null, null, 0, 10);
        
        verify(entityManager, times(2)).createNativeQuery(sqlCaptor.capture());
        String dataSql = sqlCaptor.getAllValues().get(1);
        
        assertTrue(dataSql.contains("AND 1=0")); // For SystemAuditLog, no application events
        assertTrue(dataSql.contains("action IN :historyActions"));
        
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(dataQuery).setParameter(eq("historyActions"), listCaptor.capture());
        
        List<String> histActions = listCaptor.getValue();
        assertTrue(histActions.contains("APPLICATION_SUBMITTED"));
        assertTrue(histActions.contains("APPROVED"));
    }

    @Test
    void testVerificationCategory() {
        adminAuditService.getUnifiedAuditLogs("VERIFICATION", null, null, null, null, null, null, 0, 10);
        
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(dataQuery).setParameter(eq("historyActions"), listCaptor.capture());
        assertTrue(listCaptor.getValue().contains("DOCUMENT_VERIFIED"));
    }

    @Test
    void testComplianceCategoryAndAppIdFilter() {
        adminAuditService.getUnifiedAuditLogs("COMPLIANCE", null, null, "APP-123", null, null, null, 0, 10);
        
        verify(entityManager, times(2)).createNativeQuery(sqlCaptor.capture());
        String dataSql = sqlCaptor.getAllValues().get(1);
        
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(dataQuery).setParameter(eq("historyActions"), listCaptor.capture());
        assertTrue(listCaptor.getValue().contains("UTILIZATION_PROOF_SUBMITTED"));
        
        verify(dataQuery).setParameter(eq("appId"), eq("APP-123"));
    }

    @Test
    void testFinancialCategoryAndRole() {
        adminAuditService.getUnifiedAuditLogs("FINANCIAL", null, "GRANT_OFFICER", null, null, null, null, 0, 10);
        
        verify(entityManager, times(2)).createNativeQuery(sqlCaptor.capture());
        String dataSql = sqlCaptor.getAllValues().get(1);
        
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(dataQuery).setParameter(eq("historyActions"), listCaptor.capture());
        assertTrue(listCaptor.getValue().contains("MILESTONE_RELEASED"));
        
        assertTrue(dataSql.contains("baseLegacySql") || dataSql.contains("LEGACY_DISBURS"));
        assertTrue(dataSql.contains("legacyWhere.append(\" AND 1=1 \")") || dataSql.contains("AND 1=1"));
        
        verify(dataQuery).setParameter(eq("actorRole"), eq("GRANT_OFFICER"));
        verify(dataQuery).setParameter(eq("rolePrefixed"), eq("ROLE_GRANT_OFFICER"));
    }

    @Test
    void testAdministrationCategory() {
        adminAuditService.getUnifiedAuditLogs("ADMINISTRATION", null, null, null, null, null, null, 0, 10);
        
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(dataQuery).setParameter(eq("systemEvents"), listCaptor.capture());
        assertTrue(listCaptor.getValue().contains("SCHEME_CREATED"));
        assertTrue(listCaptor.getValue().contains("OFFICER_UPDATED"));
    }

    @Test
    void testSecurityCategoryAndFailed() {
        adminAuditService.getUnifiedAuditLogs("SECURITY", null, null, null, false, null, null, 0, 10);
        
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(dataQuery).setParameter(eq("systemEvents"), listCaptor.capture());
        assertTrue(listCaptor.getValue().contains("ACCESS_DENIED"));
        
        verify(dataQuery).setParameter(eq("success"), eq(false));
    }

    @Test
    void testDateFilters() {
        adminAuditService.getUnifiedAuditLogs(null, null, null, null, null, "2026-10-06", "2026-10-07", 0, 10);
        
        verify(dataQuery).setParameter(eq("fromDate"), any()); // Will match timestamp
        verify(dataQuery).setParameter(eq("toDate"), any());   // Will match timestamp
    }
    
    @Test
    void testEmptyResultBehavesCorrectly() {
        Map<String, Object> result = adminAuditService.getUnifiedAuditLogs("AUTHENTICATION", null, null, null, null, null, null, 0, 10);
        assertEquals(0L, result.get("totalElements"));
        assertEquals(0, result.get("totalPages"));
        assertTrue(((List<?>)result.get("content")).isEmpty());
    }
}
