package com.infosys.subsidy.dto;

import jakarta.validation.constraints.NotBlank;

public class OtpResendRequest {
    @NotBlank(message = "Challenge ID is required")
    private String challengeId;

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
}
