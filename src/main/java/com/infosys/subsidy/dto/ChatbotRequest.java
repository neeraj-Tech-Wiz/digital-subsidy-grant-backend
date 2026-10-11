package com.infosys.subsidy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatbotRequest {
    
    @NotBlank(message = "Message cannot be empty")
    @Size(max = 500, message = "Message is too long")
    private String message;

    public ChatbotRequest() {}

    public ChatbotRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
