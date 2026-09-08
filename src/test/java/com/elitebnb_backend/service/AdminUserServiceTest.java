package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AdminUserResponse;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService =
                new AdminUserService(
                        userRepository,
                        auditLogService
                );
    }

    @Test
    void getCurrentAdminReturnsAuthenticatedAdminProfile() {
        User admin =
                user(
                        9L,
                        "admin@example.com",
                        Role.ADMIN
                );

        when(userRepository.findByEmail(admin.getEmail()))
                .thenReturn(Optional.of(admin));

        AdminUserResponse response =
                adminUserService.getCurrentAdmin(
                        admin.getEmail()
                );

        assertThat(response.getId())
                .isEqualTo(admin.getId());
        assertThat(response.getEmail())
                .isEqualTo(admin.getEmail());
        assertThat(response.getRole())
                .isEqualTo(Role.ADMIN);
    }

    @Test
    void getCurrentAdminRejectsNonAdminAccount() {
        User guest =
                user(
                        1L,
                        "guest@example.com",
                        Role.USER
                );

        when(userRepository.findByEmail(guest.getEmail()))
                .thenReturn(Optional.of(guest));

        assertThatThrownBy(() ->
                adminUserService.getCurrentAdmin(
                        guest.getEmail()
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Admin account required");
    }

    private User user(
            Long id,
            String email,
            Role role
    ) {
        return User.builder()
                .id(id)
                .firstName("Amina")
                .lastName("Admin")
                .email(email)
                .password("hashed-password")
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }
}
