package com.infosys.subsidy.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class HealthSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    public void testHealthEndpoint_PermitAllWithoutToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void testHealthEndpointHead_PermitAllWithoutToken() throws Exception {
        mockMvc.perform(head("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    public void testHealthEndpointPost_DeniedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/health"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testProtectedBeneficiaryEndpoint_DeniedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/beneficiaries/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testProtectedAdminEndpoint_DeniedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/officers"))
                .andExpect(status().isForbidden());
    }
}
