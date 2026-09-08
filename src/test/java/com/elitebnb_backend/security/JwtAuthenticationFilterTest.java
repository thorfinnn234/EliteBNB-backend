package com.elitebnb_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtAuthenticationFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userDetailsService
                );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void suspendedUserDetailsDoNotAuthenticateExistingJwt()
            throws Exception {

        UserDetails suspendedUser =
                org.springframework.security.core.userdetails.User
                        .withUsername("suspended@example.com")
                        .password("hashed-password")
                        .roles("USER")
                        .disabled(true)
                        .build();

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer existing-token");
        when(jwtService.extractEmail("existing-token"))
                .thenReturn("suspended@example.com");
        when(userDetailsService.loadUserByUsername("suspended@example.com"))
                .thenReturn(suspendedUser);

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        assertThat(SecurityContextHolder
                .getContext()
                .getAuthentication())
                .isNull();

        verify(jwtService, never())
                .isTokenValid(
                        "existing-token",
                        suspendedUser
                );
        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void activeUserDetailsAuthenticateValidJwt()
            throws Exception {

        UserDetails activeAdmin =
                org.springframework.security.core.userdetails.User
                        .withUsername("admin@example.com")
                        .password("hashed-password")
                        .roles("ADMIN")
                        .build();

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer active-token");
        when(jwtService.extractEmail("active-token"))
                .thenReturn("admin@example.com");
        when(userDetailsService.loadUserByUsername("admin@example.com"))
                .thenReturn(activeAdmin);
        when(jwtService.isTokenValid(
                "active-token",
                activeAdmin
        )).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertThat(authentication)
                .isNotNull();
        assertThat(authentication.getName())
                .isEqualTo("admin@example.com");
        assertThat(authentication.getAuthorities())
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(
                                authority.getAuthority()
                        )
                );

        verify(filterChain)
                .doFilter(request, response);
    }
}
