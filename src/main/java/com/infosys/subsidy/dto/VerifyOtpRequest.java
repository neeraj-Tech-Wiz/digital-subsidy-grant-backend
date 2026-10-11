package com.infosys.subsidy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerifyOtpRequest {
    @NotBlank(message = "Challenge ID is required")
    private String challengeId;
    
    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String otp;

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
