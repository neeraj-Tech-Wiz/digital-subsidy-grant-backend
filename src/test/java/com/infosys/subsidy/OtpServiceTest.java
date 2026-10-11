package com.infosys.subsidy;

import com.infosys.subsidy.entity.OtpChallenge;
import com.infosys.subsidy.repository.OtpChallengeRepository;
import com.infosys.subsidy.service.BrevoEmailService;
import com.infosys.subsidy.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class OtpServiceTest {

    @Mock
    private OtpChallengeRepository otpChallengeRepository;

    @Mock
    private BrevoEmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testOtpGenerationAndHashing() {
        ArgumentCaptor<String> rawOtpCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OtpChallenge> challengeCaptor = ArgumentCaptor.forClass(OtpChallenge.class);

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp");
        when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> i.getArgument(0));

        OtpChallenge challenge = otpService.createAndSendOtp(1L, "test@example.com");

        verify(emailService).sendOtpEmail(eq("test@example.com"), rawOtpCaptor.capture());
        verify(otpChallengeRepository).save(challengeCaptor.capture());

        String rawOtpSent = rawOtpCaptor.getValue();
        OtpChallenge savedChallenge = challengeCaptor.getValue();

        // exactly 6 digits, allows leading zeros
        assertTrue(Pattern.matches("\\d{6}", rawOtpSent));
        // hashed before persistence
        assertEquals("hashed_otp", savedChallenge.getOtpHash());
        // plaintext never persisted
        assertNotEquals(rawOtpSent, savedChallenge.getOtpHash());
        // OTP never returned explicitly
        assertFalse(challenge.toString().contains(rawOtpSent));
    }

    @Test
    void testVerifyOtpSuccess() {
        OtpChallenge challenge = new OtpChallenge();
        challenge.setId("challenge-123");
        challenge.setOtpHash("hashed_otp");
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setMaxAttempts(5);
        challenge.setAttemptCount(0);
        challenge.setUsed(false);

        when(otpChallengeRepository.findById("challenge-123")).thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("123456", "hashed_otp")).thenReturn(true);
        when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> i.getArgument(0));

        OtpChallenge result = otpService.verifyOtp("challenge-123", "123456");

        assertTrue(result.isUsed());
        assertNotNull(result.getVerifiedAt());
        // verifies that OTP verify works successfully and gives no JWT token automatically here
    }

    @Test
    void testVerifyOtpInvalidIncrementsAttempt() {
        OtpChallenge challenge = new OtpChallenge();
        challenge.setId("challenge-123");
        challenge.setOtpHash("hashed_otp");
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setMaxAttempts(5);
        challenge.setAttemptCount(2);

        when(otpChallengeRepository.findById("challenge-123")).thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("wrong", "hashed_otp")).thenReturn(false);

        Exception exception = assertThrows(RuntimeException.class, () -> otpService.verifyOtp("challenge-123", "wrong"));
        assertEquals("Invalid OTP.", exception.getMessage());
        
        // Attempt counter increment
        verify(otpChallengeRepository, times(1)).save(challenge);
        assertEquals(3, challenge.getAttemptCount());
    }

    @Test
    void testVerifyOtpMaxAttemptsBlocked() {
        OtpChallenge challenge = new OtpChallenge();
        challenge.setId("challenge-123");
        challenge.setMaxAttempts(5);
        challenge.setAttemptCount(5); // already blocked

        when(otpChallengeRepository.findById("challenge-123")).thenReturn(Optional.of(challenge));

        Exception exception = assertThrows(RuntimeException.class, () -> otpService.verifyOtp("challenge-123", "123456"));
        assertEquals("Maximum OTP attempts reached. Please request a new OTP.", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void testVerifyOtpExpired() {
        OtpChallenge challenge = new OtpChallenge();
        challenge.setId("challenge-123");
        challenge.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired
        challenge.setMaxAttempts(5);
        challenge.setAttemptCount(0);

        when(otpChallengeRepository.findById("challenge-123")).thenReturn(Optional.of(challenge));

        Exception exception = assertThrows(RuntimeException.class, () -> otpService.verifyOtp("challenge-123", "123456"));
        assertEquals("OTP has expired. Please request a new OTP.", exception.getMessage());
    }

    @Test
    void testVerifyOtpUsedRejects() {
        OtpChallenge challenge = new OtpChallenge();
        challenge.setId("challenge-123");
        challenge.setUsed(true);

        when(otpChallengeRepository.findById("challenge-123")).thenReturn(Optional.of(challenge));

        Exception exception = assertThrows(RuntimeException.class, () -> otpService.verifyOtp("challenge-123", "123456"));
        assertEquals("OTP has already been used.", exception.getMessage());
    }

    @Test
    void testResendOtpCooldownEnforced() {
        OtpChallenge challenge = new OtpChallenge();
        challenge.setId("challenge-123");
        challenge.setCreatedAt(LocalDateTime.now().minusSeconds(30)); // 30s ago, too soon

        when(otpChallengeRepository.findById("challenge-123")).thenReturn(Optional.of(challenge));

        Exception exception = assertThrows(RuntimeException.class, () -> otpService.resendOtp("challenge-123"));
        assertEquals("Please wait 60 seconds before requesting a new OTP.", exception.getMessage());
    }

    @Test
    void testResendOtpSuccessInvalidatesOld() {
        OtpChallenge challenge = new OtpChallenge();
        challenge.setId("challenge-123");
        challenge.setCreatedAt(LocalDateTime.now().minusSeconds(90)); // 90s ago
        challenge.setUserId(1L);
        challenge.setEmail("test@ex.com");
        challenge.setUsed(false);

        when(otpChallengeRepository.findById("challenge-123")).thenReturn(Optional.of(challenge));
        when(passwordEncoder.encode(anyString())).thenReturn("new_hash");
        when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> i.getArgument(0));

        OtpChallenge newChallenge = otpService.resendOtp("challenge-123");

        // Old challenge invalidated
        assertTrue(challenge.isUsed());
        verify(otpChallengeRepository, times(2)).save(any(OtpChallenge.class)); // 1 for old, 1 for new

        // New challenge assertions
        assertNotNull(newChallenge);
    }

    @Test
    void testWrongChallengeIdRejects() {
        when(otpChallengeRepository.findById("missing")).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> otpService.verifyOtp("missing", "123456"));
        assertEquals("Invalid challenge ID.", exception.getMessage());
    }
    
    @Test
    void testBrevoFailureDeletesChallenge() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> i.getArgument(0));
        
        doThrow(new RuntimeException("SMTP Crash")).when(emailService).sendOtpEmail(anyString(), anyString());
        
        Exception ex = assertThrows(RuntimeException.class, () -> otpService.createRegistrationOtp("Name", "test@test.com", "hash"));
        assertEquals("Failed to send verification email. Please try again later.", ex.getMessage());
        
        verify(otpChallengeRepository).delete(any(OtpChallenge.class));
    }
}
