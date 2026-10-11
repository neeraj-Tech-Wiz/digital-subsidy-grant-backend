package com.infosys.subsidy.dto;

public class LoginOtpResponse {
    private boolean otpRequired;
    private String challengeId;
    private String maskedEmail;
    private int expiresInSeconds;

    public LoginOtpResponse(boolean otpRequired, String challengeId, String maskedEmail, int expiresInSeconds) {
        this.otpRequired = otpRequired;
        this.challengeId = challengeId;
        this.maskedEmail = maskedEmail;
        this.expiresInSeconds = expiresInSeconds;
    }

    public boolean isOtpRequired() { return otpRequired; }
    public void setOtpRequired(boolean otpRequired) { this.otpRequired = otpRequired; }
    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getMaskedEmail() { return maskedEmail; }
    public void setMaskedEmail(String maskedEmail) { this.maskedEmail = maskedEmail; }
    public int getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(int expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
}
