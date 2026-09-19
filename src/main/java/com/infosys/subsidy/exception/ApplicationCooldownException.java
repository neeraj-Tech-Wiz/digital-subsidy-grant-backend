package com.infosys.subsidy.exception;

import java.time.LocalDateTime;

public class ApplicationCooldownException extends RuntimeException {
    
    private final Long schemeId;
    private final LocalDateTime rejectedAt;
    private final LocalDateTime cooldownExpiresAt;
    private final int remainingDays;

    public ApplicationCooldownException(String message, Long schemeId, LocalDateTime rejectedAt, LocalDateTime cooldownExpiresAt, int remainingDays) {
        super(message);
        this.schemeId = schemeId;
        this.rejectedAt = rejectedAt;
        this.cooldownExpiresAt = cooldownExpiresAt;
        this.remainingDays = remainingDays;
    }

    public Long getSchemeId() { return schemeId; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public LocalDateTime getCooldownExpiresAt() { return cooldownExpiresAt; }
    public int getRemainingDays() { return remainingDays; }
}
