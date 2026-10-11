package com.infosys.subsidy.controller;

import com.infosys.subsidy.service.AdminAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AdminAuditControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private AdminAuditController adminAuditController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAuditController).build();
    }

    @Test
    public void testGetAuditLogs_Returns200() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        when(adminAuditService.getUnifiedAuditLogs(
                any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/audit"))
                .andExpect(status().isOk());
    }
}
