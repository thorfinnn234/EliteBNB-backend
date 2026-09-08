package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AdminUserResponse;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.security.CustomUserDetailsService;
import com.elitebnb_backend.security.JwtAuthenticationFilter;
import com.elitebnb_backend.security.JwtService;
import com.elitebnb_backend.security.SecurityConfig;
import com.elitebnb_backend.service.AdminDashboardService;
import com.elitebnb_backend.service.AdminUserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@Import({
        SecurityConfig.class,
        AdminDashboardControllerTest.NoOpJwtFilterConfig.class
})
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    void currentAdminRequiresAdminRole()
            throws Exception {

        mockMvc.perform(
                get("/api/admin/me")
                        .with(user("guest@example.com")
                                .roles("USER"))
        ).andExpect(status().isForbidden());
    }

    @Test
    void currentAdminReturnsAuthenticatedAdmin()
            throws Exception {

        AdminUserResponse response =
                new AdminUserResponse(
                        1L,
                        "Ada",
                        "Admin",
                        "admin@example.com",
                        "08000000000",
                        "Lagos",
                        "https://example.com/admin.jpg",
                        Role.ADMIN,
                        AccountStatus.ACTIVE,
                        true,
                        false
                );

        when(adminUserService.getCurrentAdmin("admin@example.com"))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/admin/me")
                        .with(user("admin@example.com")
                                .roles("ADMIN"))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));

        verify(adminUserService)
                .getCurrentAdmin("admin@example.com");
    }

    @TestConfiguration
    static class NoOpJwtFilterConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(
                    new JwtService(),
                    new CustomUserDetailsService(null)
            ) {
                @Override
                protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain
                ) throws ServletException, IOException {

                    filterChain.doFilter(
                            request,
                            response
                    );
                }
            };
        }
    }
}
