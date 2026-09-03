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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new beneficiary user.
     */
    public AuthResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException(
                    "An account with this email already exists");
        }

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(email);

        // Never store the plain-text password.
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // New registrations are beneficiaries by default.
        user.setRole(UserRole.BENEFICIARY);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return buildAuthResponse(savedUser, token);
    }

    /**
     * Authenticates an existing user.
     */
    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        return buildAuthResponse(user, token);
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
}