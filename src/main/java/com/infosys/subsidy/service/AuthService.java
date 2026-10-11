package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.AuthResponse;
import com.infosys.subsidy.dto.LoginRequest;
import com.infosys.subsidy.dto.RegisterRequest;
import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.repository.UserRepository;
import com.infosys.subsidy.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import com.infosys.subsidy.entity.SystemAuditLog;
import com.infosys.subsidy.enums.AuditEventType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.infosys.subsidy.entity.OtpChallenge;
import com.infosys.subsidy.dto.LoginOtpResponse;
import com.infosys.subsidy.dto.VerifyOtpRequest;
import com.infosys.subsidy.dto.OtpResendRequest;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final OtpService otpService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            OtpService otpService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.otpService = otpService;
    }

    /**
     * Initiates registration by sending an OTP. User is NOT created yet.
     */
    public LoginOtpResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        HttpServletRequest httpRequest = getRequest();
        String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : null;
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("An account with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        OtpChallenge challenge = otpService.createRegistrationOtp(request.getName().trim(), email, passwordHash);
        
        logAuthEvent(email, null, "ANONYMOUS", "EMAIL_OTP", AuditEventType.REGISTRATION_STARTED);
        logAuthEvent(email, null, "ANONYMOUS", "EMAIL_OTP", AuditEventType.REGISTRATION_OTP_SENT);

        String maskedEmail = email.replaceAll("(^[^@]{1,3})[^@]*(@.*)", "$1***$2");
        return new LoginOtpResponse(true, challenge.getId(), maskedEmail, 300);
    }

    /**
     * Authenticates an existing user.
     */
    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        
        HttpServletRequest httpRequest = getRequest();
        String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : null;
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;

        User user;
        try {
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));

            if (!user.isActive()) {
                throw new RuntimeException("Account is deactivated");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid email or password");
            }
        } catch (RuntimeException e) {
            logAuthFailure(email, e.getMessage(), ipAddress, userAgent, "PASSWORD");
            throw e;
        }

        String token = jwtService.generateToken(user);

        logAuthSuccess(user, ipAddress, userAgent, "PASSWORD");

        return buildAuthResponse(user, token);
    }

    public void logout() {
        HttpServletRequest httpRequest = getRequest();
        String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : null;
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;

        // Try getting user info from SecurityContext
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String email = "Anonymous";
        String role = "UNAUTHENTICATED";
        Long userId = null;

        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            email = authentication.getName();
            if (!authentication.getAuthorities().isEmpty()) {
                role = authentication.getAuthorities().iterator().next().getAuthority();
            }
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        SystemAuditLog log = new SystemAuditLog();
        log.setEventType(AuditEventType.LOGOUT);
        log.setActorUserId(userId);
        log.setActorEmail(email);
        log.setActorRole(role);
        log.setActorType("USER");
        log.setResourceType("AUTHENTICATION");
        log.setSuccess(true);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        auditService.logEvent(log);
    }

    private AuthResponse buildAuthResponse(
            User user,
            String token) {

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    public LoginOtpResponse loginOtp(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        HttpServletRequest httpRequest = getRequest();
        String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : null;
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;
        
        User user;
        try {
            user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Invalid email or password"));
            if (!user.isActive()) throw new RuntimeException("Account is deactivated");
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) throw new RuntimeException("Invalid email or password");
        } catch (RuntimeException e) {
            logAuthFailure(email, e.getMessage(), ipAddress, userAgent, "EMAIL_OTP");
            throw e;
        }

        OtpChallenge challenge = otpService.createAndSendOtp(user.getId(), user.getEmail());
        logAuthEvent(email, user.getId(), user.getRole().name(), "EMAIL_OTP", AuditEventType.OTP_SENT);
        
        String maskedEmail = email.replaceAll("(^[^@]{1,3})[^@]*(@.*)", "$1***$2");
        return new LoginOtpResponse(true, challenge.getId(), maskedEmail, 300);
    }

    public LoginOtpResponse resendOtp(OtpResendRequest request) {
        OtpChallenge challenge = otpService.resendOtp(request.getChallengeId());
        
        AuditEventType eventType = (challenge.getPurpose() == com.infosys.subsidy.enums.OtpPurpose.REGISTRATION)
                ? AuditEventType.REGISTRATION_OTP_RESENT : AuditEventType.OTP_RESENT;
                
        logAuthEvent(challenge.getEmail(), challenge.getUserId(), "UNKNOWN", "EMAIL_OTP", eventType);
        
        String maskedEmail = challenge.getEmail().replaceAll("(^[^@]{1,3})[^@]*(@.*)", "$1***$2");
        return new LoginOtpResponse(true, challenge.getId(), maskedEmail, 300);
    }

    public void verifyRegistrationOtp(VerifyOtpRequest request) {
        HttpServletRequest httpRequest = getRequest();
        
        try {
            OtpChallenge challenge = otpService.verifyOtp(request.getChallengeId(), request.getOtp());
            if (challenge.getPurpose() != com.infosys.subsidy.enums.OtpPurpose.REGISTRATION) {
                throw new RuntimeException("Invalid challenge purpose.");
            }

            if (userRepository.existsByEmail(challenge.getEmail())) {
                throw new RuntimeException("An account with this email already exists.");
            }

            User user = new User();
            user.setName(challenge.getRegistrationName());
            user.setEmail(challenge.getEmail());
            user.setPassword(challenge.getRegistrationPassword());
            user.setRole(UserRole.BENEFICIARY);
            user.setEmailVerified(true);
            userRepository.save(user);

            logAuthEvent(user.getEmail(), user.getId(), user.getRole().name(), "EMAIL_OTP", AuditEventType.REGISTRATION_OTP_VERIFICATION_SUCCESS);
            logAuthEvent(user.getEmail(), user.getId(), user.getRole().name(), "EMAIL_OTP", AuditEventType.REGISTRATION_COMPLETED);

        } catch (RuntimeException e) {
            logAuthEvent("unknown", null, "ANONYMOUS", "EMAIL_OTP", AuditEventType.REGISTRATION_OTP_VERIFICATION_FAILED);
            throw e;
        }
    }

    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        try {
            OtpChallenge challenge = otpService.verifyOtp(request.getChallengeId(), request.getOtp());
            
            if (challenge.getPurpose() != com.infosys.subsidy.enums.OtpPurpose.LOGIN) {
                throw new RuntimeException("Invalid challenge purpose.");
            }
            
            User user = userRepository.findById(challenge.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
            String token = jwtService.generateToken(user);
            
            HttpServletRequest httpRequest = getRequest();
            String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : null;
            String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;

            logAuthSuccess(user, ipAddress, userAgent, "EMAIL_OTP");
            logAuthEvent(user.getEmail(), user.getId(), user.getRole().name(), "EMAIL_OTP", AuditEventType.OTP_VERIFICATION_SUCCESS);
            
            return buildAuthResponse(user, token);
        } catch (RuntimeException e) {
            logAuthEvent("unknown", null, "UNKNOWN", "EMAIL_OTP", AuditEventType.OTP_VERIFICATION_FAILED);
            throw e;
        }
    }

    private void logAuthFailure(String email, String reason, String ip, String userAgent, String authMethod) {
        SystemAuditLog log = new SystemAuditLog();
        log.setEventType(AuditEventType.LOGIN_FAILED);
        log.setActorEmail(email);
        log.setActorType("USER");
        log.setResourceType("AUTHENTICATION");
        log.setSuccess(false);
        log.setFailureReason(reason);
        log.setAuthenticationMethod(authMethod);
        log.setIpAddress(ip);
        log.setUserAgent(userAgent);
        auditService.logAuthEventRequiresNew(log);
    }

    private void logAuthSuccess(User user, String ip, String userAgent, String authMethod) {
        SystemAuditLog log = new SystemAuditLog();
        log.setEventType(AuditEventType.LOGIN_SUCCESS);
        log.setActorUserId(user.getId());
        log.setActorEmail(user.getEmail());
        log.setActorRole(user.getRole().name());
        log.setActorType("USER");
        log.setResourceType("AUTHENTICATION");
        log.setAuthenticationMethod(authMethod);
        log.setSuccess(true);
        log.setIpAddress(ip);
        log.setUserAgent(userAgent);
        auditService.logEvent(log);
    }

    private void logAuthEvent(String email, Long userId, String role, String method, AuditEventType eventType) {
        try {
            SystemAuditLog log = new SystemAuditLog();
            log.setEventType(eventType);
            log.setActorUserId(userId);
            log.setActorEmail(email);
            log.setActorRole(role);
            log.setActorType("USER");
            log.setResourceType("AUTHENTICATION_OTP");
            log.setAuthenticationMethod(method);
            log.setSuccess(true);
            HttpServletRequest httpRequest = getRequest();
            if (httpRequest != null) {
                log.setIpAddress(httpRequest.getRemoteAddr());
                log.setUserAgent(httpRequest.getHeader("User-Agent"));
            }
            auditService.logAuthEventRequiresNew(log);
        } catch (Exception ex) {}
    }
}