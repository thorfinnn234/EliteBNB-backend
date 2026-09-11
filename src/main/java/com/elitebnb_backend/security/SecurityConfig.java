package com.elitebnb_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // =========================
                // CORS
                // =========================
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // =========================
                        // PUBLIC AUTH ENDPOINTS
                        // =========================
                        .requestMatchers(
                                "/api/auth/**",
                                "/error"
                        )
                        .permitAll()

                        // =========================
                        // MONITORING / ACTUATOR
                        // =========================
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus"
                        )
                        .permitAll()

                        // =========================
                        // PROPERTY - HOST
                        // =========================

                        // HOST'S OWN PROPERTIES
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/properties/my"
                        )
                        .hasRole("HOST")

                        // CREATE PROPERTY
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/properties"
                        )
                        .hasRole("HOST")

                        // REAL PROPERTY IMAGE UPLOAD
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/properties/*/images/upload"
                        )
                        .hasRole("HOST")

                        // OLD URL-BASED PROPERTY IMAGE
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/properties/*/images"
                        )
                        .hasRole("HOST")

                        // UPDATE PROPERTY
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/properties/**"
                        )
                        .hasRole("HOST")

                        // HOST - DELETE PROPERTY IMAGE
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/properties/*/images/*"
                        )
                        .hasRole("HOST")

                        // HOST - CHANGE PROPERTY COVER IMAGE
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/properties/*/images/*/cover"
                        )
                        .hasRole("HOST")

                        // =========================
                        // PROPERTY AVAILABILITY
                        // =========================

                        // HOST - BLOCK DATES
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/properties/*/availability/block"
                        )
                        .hasRole("HOST")

                        // HOST - UNBLOCK DATES
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/properties/*/availability/*"
                        )
                        .hasRole("HOST")

                        // =========================
                        // BOOKINGS
                        // =========================

                        // USER - CREATE BOOKING
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/bookings"
                        )
                        .hasRole("USER")

                        // USER - VIEW OWN BOOKINGS
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/bookings/my"
                        )
                        .hasRole("USER")

                        // USER - CANCEL OWN PENDING RESERVATION
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/bookings/*/cancel"
                        )
                        .hasRole("USER")

                        // HOST - VIEW RESERVATIONS
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/bookings/host"
                        )
                        .hasRole("HOST")

                        // HOST - UPDATE RESERVATION STATUS
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/bookings/*/status"
                        )
                        .hasRole("HOST")

                        // =========================
                        // HOST EARNINGS
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/host/earnings"
                        )
                        .hasRole("HOST")

                        // =========================
                        // PUBLIC PROPERTY ENDPOINTS
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/properties",
                                "/api/properties/**"
                        )
                        .permitAll()

                        // =========================
                        // HOST DASHBOARD / PROFILE
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/host/dashboard"
                        )
                        .hasRole("HOST")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/host/profile"
                        )
                        .hasRole("HOST")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/host/profile"
                        )
                        .hasRole("HOST")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/host/profile/image"
                        )
                        .hasRole("HOST")

                        // =========================
                        // NOTIFICATIONS
                        // =========================

                        .requestMatchers("/api/notifications/**")
                        .authenticated()

                        // =========================
                        // REVIEWS
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/properties/*/reviews"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/reviews"
                        )
                        .hasRole("USER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reviews/my"
                        )
                        .hasRole("USER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/host/reviews"
                        )
                        .hasRole("HOST")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/reviews/*/response"
                        )
                        .hasRole("HOST")

                        // =========================
                        // USER PROFILE
                        // =========================

                        .requestMatchers("/api/user/profile/**")
                        .hasRole("USER")

                        // =========================
                        // HOST ONBOARDING
                        // =========================

                        .requestMatchers("/api/host/onboarding/**")
                        .hasRole("HOST")

                        // =========================
                        // HOST VERIFICATION
                        // =========================

                        .requestMatchers("/api/host/verification/**")
                        .hasRole("HOST")

                        // =========================
                        // HOST SUPPORT
                        // =========================

                        .requestMatchers(
                                "/api/host/support-conversation",
                                "/api/host/support-conversation/**"
                        )
                        .hasRole("HOST")

                        // =========================
                        // PAYMENTS / REFUNDS
                        // =========================

                        .requestMatchers("/api/payments/**")
                        .hasRole("USER")

                        .requestMatchers("/api/refunds/**")
                        .hasRole("USER")

                        // =========================
                        // ADMIN
                        // =========================

                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // =========================
                        // EVERYTHING ELSE
                        // =========================

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // =====================================================
    // CORS CONFIGURATION
    // =====================================================

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

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));

        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}