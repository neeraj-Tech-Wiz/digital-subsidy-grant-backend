package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.LoginRequest;
import com.infosys.subsidy.entity.SystemAuditLog;
import com.infosys.subsidy.enums.AuditEventType;
import com.infosys.subsidy.service.AuditService;
import com.infosys.subsidy.service.AuthService;
import com.infosys.subsidy.repository.UserRepository;
import com.infosys.subsidy.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthAuditTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    @Test
    public void testFailedLogin_AuditCaptured() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("wrongpassword");

        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(java.util.Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        org.mockito.ArgumentCaptor<SystemAuditLog> captor = org.mockito.ArgumentCaptor.forClass(SystemAuditLog.class);
        verify(auditService).logAuthEventRequiresNew(captor.capture());
        SystemAuditLog capturedLog = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(AuditEventType.LOGIN_FAILED, capturedLog.getEventType());
        org.junit.jupiter.api.Assertions.assertEquals("nonexistent@example.com", capturedLog.getActorEmail());
    }
}
