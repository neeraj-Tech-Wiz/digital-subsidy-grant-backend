package com.infosys.subsidy.service;

import com.infosys.subsidy.entity.OtpChallenge;
import com.infosys.subsidy.enums.OtpPurpose;
import com.infosys.subsidy.repository.OtpChallengeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OtpService {

    private final OtpChallengeRepository otpChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrevoEmailService brevoEmailService;
    private final SecureRandom secureRandom;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    public OtpService(OtpChallengeRepository otpChallengeRepository,
                      PasswordEncoder passwordEncoder,
                      BrevoEmailService brevoEmailService) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.brevoEmailService = brevoEmailService;
        this.secureRandom = new SecureRandom();
    }

    public OtpChallenge createAndSendOtp(Long userId, String email) {
        return createAndSendOtpInternal(userId, email, OtpPurpose.LOGIN, null, null);
    }

    public OtpChallenge createRegistrationOtp(String name, String email, String passwordHash) {
        return createAndSendOtpInternal(null, email, OtpPurpose.REGISTRATION, name, passwordHash);
    }

    private OtpChallenge createAndSendOtpInternal(Long userId, String email, OtpPurpose purpose, String regName, String regPass) {
        String rawOtp = generateDigitOtp();
        String hash = passwordEncoder.encode(rawOtp);

        OtpChallenge challenge = new OtpChallenge();
        challenge.setId(UUID.randomUUID().toString());
        challenge.setUserId(userId);
        challenge.setEmail(email);
        challenge.setOtpHash(hash);
        challenge.setPurpose(purpose);
        challenge.setRegistrationName(regName);
        challenge.setRegistrationPassword(regPass);
        challenge.setCreatedAt(LocalDateTime.now());
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(MAX_ATTEMPTS);
        challenge.setUsed(false);
        
        OtpChallenge saved = otpChallengeRepository.save(challenge);
        
        try {
            brevoEmailService.sendOtpEmail(email, rawOtp);
        } catch (Exception e) {
            otpChallengeRepository.delete(saved);
            throw new RuntimeException("Failed to send verification email. Please try again later.");
        }
        
        return saved;
    }

    private String generateDigitOtp() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    public OtpChallenge verifyOtp(String challengeId, String rawOtp) {
        OtpChallenge challenge = otpChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Invalid challenge ID."));

        if (challenge.isUsed()) {
            throw new RuntimeException("OTP has already been used.");
        }

        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            throw new RuntimeException("Maximum OTP attempts reached. Please request a new OTP.");
        }

        if (LocalDateTime.now().isAfter(challenge.getExpiresAt())) {
            throw new RuntimeException("OTP has expired. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(rawOtp, challenge.getOtpHash())) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);
            otpChallengeRepository.save(challenge);
            throw new RuntimeException("Invalid OTP.");
        }

        // Successfully verified logic ends strictly isolated. Returning challenge mapping verified_at metrics
        challenge.setUsed(true);
        challenge.setVerifiedAt(LocalDateTime.now());
        return otpChallengeRepository.save(challenge);
    }

    public OtpChallenge resendOtp(String challengeId) {
        OtpChallenge oldChallenge = otpChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Invalid challenge ID."));

        if (oldChallenge.isUsed()) {
            throw new RuntimeException("OTP already used.");
        }

        long secondsSinceCreation = java.time.Duration.between(oldChallenge.getCreatedAt(), LocalDateTime.now()).getSeconds();
        if (secondsSinceCreation < 60) {
            throw new RuntimeException("Please wait 60 seconds before requesting a new OTP.");
        }

        // Invalidate old
        oldChallenge.setUsed(true);
        otpChallengeRepository.save(oldChallenge);

        // Create new
        return createAndSendOtpInternal(
                oldChallenge.getUserId(), 
                oldChallenge.getEmail(), 
                oldChallenge.getPurpose(),
                oldChallenge.getRegistrationName(),
                oldChallenge.getRegistrationPassword()
        );
    }
}
