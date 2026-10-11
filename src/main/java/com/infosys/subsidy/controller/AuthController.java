package com.infosys.subsidy.controller;

import com.infosys.subsidy.dto.AuthResponse;
import com.infosys.subsidy.dto.LoginRequest;
import com.infosys.subsidy.dto.LoginOtpResponse;
import com.infosys.subsidy.dto.OtpResendRequest;
import com.infosys.subsidy.dto.VerifyOtpRequest;
import com.infosys.subsidy.dto.RegisterRequest;
import com.infosys.subsidy.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new beneficiary account.
     */
    @PostMapping("/register")
    public ResponseEntity<LoginOtpResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        LoginOtpResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-registration-otp")
    public ResponseEntity<Void> verifyRegistrationOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyRegistrationOtp(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Login using email and password.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Logout explicitly.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.ok().build();
    }

    /**
     * Login using email and password, generates OTP for verification.
     */
    @PostMapping("/login-otp")
    public ResponseEntity<LoginOtpResponse> loginOtp(
            @Valid @RequestBody LoginRequest request) {
        LoginOtpResponse response = authService.loginOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Resends an OTP challenge.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<LoginOtpResponse> resendOtp(
            @Valid @RequestBody OtpResendRequest request) {
        LoginOtpResponse response = authService.resendOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies the OTP challenge and gives the JWT upon success.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }
}