package com.infosys.subsidy;

import com.infosys.subsidy.dto.LoginOtpResponse;
import com.infosys.subsidy.dto.AuthResponse;
import com.infosys.subsidy.dto.RegisterRequest;
import com.infosys.subsidy.dto.VerifyOtpRequest;
import com.infosys.subsidy.entity.OtpChallenge;
import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.enums.OtpPurpose;
import com.infosys.subsidy.repository.UserRepository;
import com.infosys.subsidy.security.JwtService;
import com.infosys.subsidy.service.AuditService;
import com.infosys.subsidy.service.AuthService;
import com.infosys.subsidy.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AuthServiceRegistrationTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuditService auditService;
    @Mock private OtpService otpService;

    @InjectMocks private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void testRegistrationStartsOtpFlowNoJwtReturned() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test User");
        req.setEmail("test@email.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        
        OtpChallenge mockChallenge = new OtpChallenge();
        mockChallenge.setId("chal-123");
        when(otpService.createRegistrationOtp("Test User", "test@email.com", "hashed")).thenReturn(mockChallenge);

        LoginOtpResponse resp = authService.register(req);
        
        // Assert NO JWT returned natively natively inside LoginOtpResponse
        assertTrue(resp.isOtpRequired());
        assertEquals("chal-123", resp.getChallengeId());
        
        verify(otpService).createRegistrationOtp("Test User", "test@email.com", "hashed");
        verify(userRepository, never()).save(any());
    }

    @Test
    void testVerifyRegistrationOtpCreatesBeneficiary() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setChallengeId("chal-123");
        req.setOtp("123456");

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPurpose(OtpPurpose.REGISTRATION);
        challenge.setEmail("test@email.com");
        challenge.setRegistrationName("Test User");
        challenge.setRegistrationPassword("hashed");
        
        when(otpService.verifyOtp("chal-123", "123456")).thenReturn(challenge);
        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        
        authService.verifyRegistrationOtp(req);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertEquals("Test User", savedUser.getName());
        assertEquals("test@email.com", savedUser.getEmail());
        assertEquals("hashed", savedUser.getPassword());
        assertEquals("BENEFICIARY", savedUser.getRole().name());
        assertTrue(savedUser.isEmailVerified());
    }

    @Test
    void testDuplicateEmailRejectedOnVerify() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setChallengeId("chal-123");
        req.setOtp("123456");

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPurpose(OtpPurpose.REGISTRATION);
        challenge.setEmail("dup@email.com");

        when(otpService.verifyOtp("chal-123", "123456")).thenReturn(challenge);
        when(userRepository.existsByEmail("dup@email.com")).thenReturn(true);

        Exception ex = assertThrows(RuntimeException.class, () -> authService.verifyRegistrationOtp(req));
        assertEquals("An account with this email already exists.", ex.getMessage());
        
        verify(userRepository, never()).save(any());
    }
}
