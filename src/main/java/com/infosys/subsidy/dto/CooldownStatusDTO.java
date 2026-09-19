package com.infosys.subsidy.dto;

import java.time.LocalDateTime;

public class CooldownStatusDTO {
    private boolean canApply;
    private boolean cooldownActive;
    private String reason;
    private LocalDateTime rejectedAt;
    private LocalDateTime cooldownExpiresAt;
    private Integer remainingDays;
    private Integer configuredCooldownDays;

    public CooldownStatusDTO() {}

    public CooldownStatusDTO(boolean canApply, boolean cooldownActive, String reason, LocalDateTime rejectedAt, LocalDateTime cooldownExpiresAt, Integer remainingDays, Integer configuredCooldownDays) {
        this.canApply = canApply;
        this.cooldownActive = cooldownActive;
        this.reason = reason;
        this.rejectedAt = rejectedAt;
        this.cooldownExpiresAt = cooldownExpiresAt;
        this.remainingDays = remainingDays;
        this.configuredCooldownDays = configuredCooldownDays;
    }

    public boolean isCanApply() { return canApply; }
    public void setCanApply(boolean canApply) { this.canApply = canApply; }

    public boolean isCooldownActive() { return cooldownActive; }
    public void setCooldownActive(boolean cooldownActive) { this.cooldownActive = cooldownActive; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public LocalDateTime getCooldownExpiresAt() { return cooldownExpiresAt; }
    public void setCooldownExpiresAt(LocalDateTime cooldownExpiresAt) { this.cooldownExpiresAt = cooldownExpiresAt; }

    public Integer getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Integer remainingDays) { this.remainingDays = remainingDays; }

    public Integer getConfiguredCooldownDays() { return configuredCooldownDays; }
    public void setConfiguredCooldownDays(Integer configuredCooldownDays) { this.configuredCooldownDays = configuredCooldownDays; }
}
