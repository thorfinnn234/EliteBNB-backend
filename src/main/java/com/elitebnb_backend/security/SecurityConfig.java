package com.elitebnb_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                        // PROPERTY - HOST
                        // =========================

                        // HOST'S OWN PROPERTIES
                        // Must stay before public property GET rules
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

                        // DELETE PROPERTY
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

                        // Includes:
                        // GET /api/properties
                        // GET /api/properties/{id}
                        // GET /api/properties/{id}/images
                        // GET /api/properties/{id}/availability
                        // GET /api/properties/search
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/properties",
                                "/api/properties/**"
                        )
                        .permitAll()


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

                                .requestMatchers("/api/notifications/**")
                                .authenticated()

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
}