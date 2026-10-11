package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.ChatbotRequest;
import com.infosys.subsidy.dto.ChatbotResponse;
import com.infosys.subsidy.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/beneficiary/chatbot")
public class BeneficiaryChatbotController {

    private final ChatbotService chatbotService;

    public BeneficiaryChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    @PreAuthorize("hasRole('BENEFICIARY')")
    public ResponseEntity<ChatbotResponse> processMessage(
            @Valid @RequestBody ChatbotRequest request,
            Authentication authentication) {
        
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        ChatbotResponse response = chatbotService.processMessage(request, email);
        return ResponseEntity.ok(response);
    }
}
