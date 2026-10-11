package com.infosys.subsidy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.subsidy.dto.ChatbotRequest;
import com.infosys.subsidy.dto.ChatbotResponse;
import com.infosys.subsidy.service.ChatbotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.security.core.Authentication;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class BeneficiaryChatbotControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatbotService chatbotService;

    @InjectMocks
    private BeneficiaryChatbotController chatbotController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatbotController).build();
    }

    @Test
    public void testUnauthorizedUser() throws Exception {
        ChatbotRequest request = new ChatbotRequest("hello");
        mockMvc.perform(post("/api/beneficiary/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // The controller logic explicitly returns 401 when auth is null
    }

    @Test
    public void testBeneficiaryIsAcceptedAndIdentityUsed() throws Exception {
        ChatbotRequest request = new ChatbotRequest("what are my milestones");
        ChatbotResponse mockResponse = new ChatbotResponse("Your next milestone is...", "MILESTONES");

        when(chatbotService.processMessage(any(ChatbotRequest.class), eq("beneficiary@example.com")))
                .thenReturn(mockResponse);

        Authentication auth = new UsernamePasswordAuthenticationToken("beneficiary@example.com", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_BENEFICIARY")));

        mockMvc.perform(post("/api/beneficiary/chatbot/message")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Your next milestone is..."))
                .andExpect(jsonPath("$.actionMetadata").value("MILESTONES"));
    }

    @Test
    public void testBlankMessageFailsValidation() throws Exception {
        ChatbotRequest request = new ChatbotRequest(" "); // Blank
        mockMvc.perform(post("/api/beneficiary/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

