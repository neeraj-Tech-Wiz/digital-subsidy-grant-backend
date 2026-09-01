package com.infosys.subsidy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.subsidy.controller.SchemeController;
import com.infosys.subsidy.dto.SchemeRequest;
import com.infosys.subsidy.entity.Scheme;
import com.infosys.subsidy.enums.BeneficiaryCategory;
import com.infosys.subsidy.enums.SchemeStatus;
import com.infosys.subsidy.service.SchemeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SchemeControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SchemeService schemeService;

    @BeforeEach
    void setUp() {
        SchemeController controller = new SchemeController(schemeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testCreateScheme_Endpoint() throws Exception {
        SchemeRequest request = new SchemeRequest(
                "SCH-TEST-01", "Kisan Support", "Agriculture grant",
                6000.0, 5000000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.FARMER
        );

        Scheme mockScheme = new Scheme(
                1L, "SCH-TEST-01", "Kisan Support", "Agriculture grant",
                6000.0, 5000000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.FARMER
        );

        when(schemeService.createScheme(any(SchemeRequest.class))).thenReturn(mockScheme);

        mockMvc.perform(post("/api/schemes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.schemeCode").value("SCH-TEST-01"))
                .andExpect(jsonPath("$.schemeName").value("Kisan Support"));
    }

    @Test
    void testGetAllSchemes_Endpoint() throws Exception {
        Scheme s1 = new Scheme(1L, "SCH-01", "Scheme One", "Desc 1", 5000.0, 50000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.GENERAL);
        when(schemeService.getAllSchemes()).thenReturn(List.of(s1));

        mockMvc.perform(get("/api/schemes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].schemeCode").value("SCH-01"));
    }

    @Test
    void testGetSchemeById_Endpoint() throws Exception {
        Scheme s1 = new Scheme(1L, "SCH-01", "Scheme One", "Desc 1", 5000.0, 50000.0, SchemeStatus.ACTIVE, "All India", BeneficiaryCategory.GENERAL);
        when(schemeService.getSchemeById(1L)).thenReturn(s1);

        mockMvc.perform(get("/api/schemes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeCode").value("SCH-01"))
                .andExpect(jsonPath("$.grantAmount").value(5000.0));
    }
}
