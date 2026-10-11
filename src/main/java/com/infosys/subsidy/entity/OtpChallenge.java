package com.infosys.subsidy.entity;

import com.infosys.subsidy.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_challenge")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpChallenge {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private OtpPurpose purpose;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "used", nullable = false)
    private boolean used;

    @Column(name = "registration_name")
    private String registrationName;

    @Column(name = "registration_password")
    private String registrationPassword;
}
