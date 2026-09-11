package com.infosys.subsidy.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =========================
    // CORS CONFIGURATION
    // =========================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // =========================
    // SECURITY CONFIGURATION
    // =========================
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // Enable CORS
                .cors(Customizer.withDefaults())

                // Disable CSRF for REST API
                .csrf(csrf -> csrf.disable())

                // JWT = Stateless authentication
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // =========================
                // AUTHORIZATION RULES
                // =========================
                .authorizeHttpRequests(auth -> auth

                        // Allow CORS preflight requests
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // =========================
                        // PUBLIC AUTH APIs
                        // =========================
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        // =========================
                        // ADMIN OFFICER MANAGEMENT
                        // =========================
                        .requestMatchers("/api/admin/officers", "/api/admin/officers/**")
                        .hasRole("ADMIN")

                        // =========================
                        // BENEFICIARY ACTIONS
                        // =========================

                        // Profile creation — BENEFICIARY only
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/beneficiaries/profile"
                        ).hasRole("BENEFICIARY")

                        // Get own profile — BENEFICIARY only
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/beneficiaries/me"
                        ).hasRole("BENEFICIARY")

                        // Apply for scheme, submit documents, upload documents — BENEFICIARY only
                        .requestMatchers(
                                "/api/applications/apply/**",
                                "/api/applications/submit-documents/**",
                                "/api/documents/upload/**"
                        ).hasRole("BENEFICIARY")

                        // =========================
                        // OFFICER ACTIONS
                        // =========================
                        .requestMatchers("/api/verifications/**", "/api/documents/verify/**")
                        .hasAnyRole("LEVEL_1_OFFICER", "LEVEL_2_OFFICER", "LEVEL_3_OFFICER", "FINAL_APPROVAL_OFFICER")

                        // =========================
                        // GENERAL APPS & DOCS VIEWING
                        // =========================
                        .requestMatchers("/api/applications/**", "/api/documents/**")
                        .authenticated()

                        // =========================
                        // BENEFICIARY + ADMIN
                        // Can view active schemes
                        // =========================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/schemes/active"
                        )
                        .hasAnyRole(
                                "BENEFICIARY",
                                "ADMIN"
                        )

                        .requestMatchers("/api/schemes/**").permitAll()

                        // =========================
                        // ADMIN ONLY
                        // Create Scheme
                        // =========================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/schemes",
                                "/api/schemes/**"
                        )
                        .hasRole("ADMIN")

                        // =========================
                        // ADMIN ONLY
                        // Update Scheme / Criteria
                        // =========================
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/schemes/**"
                        )
                        .hasRole("ADMIN")

                        // =========================
                        // ADMIN ONLY
                        // Change status / toggle criteria
                        // =========================
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/schemes/**"
                        )
                        .hasRole("ADMIN")

                        // =========================
                        // ADMIN ONLY
                        // Delete Scheme / Criteria
                        // =========================
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/schemes/**"
                        )
                        .hasRole("ADMIN")

                        // =========================
                        // ADMIN ONLY
                        // View all schemes and criteria
                        // =========================
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/schemes/**"
                        )
                        .hasRole("ADMIN")


                        // All other APIs require login
                        .anyRequest().authenticated()
                )

                // Add JWT filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}