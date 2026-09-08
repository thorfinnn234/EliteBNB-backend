package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AuthResponse;
import com.elitebnb_backend.dto.LoginRequest;
import com.elitebnb_backend.dto.RegisterRequest;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.UserRepository;
import com.elitebnb_backend.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService =
                new AuthService(
                        userRepository,
                        passwordEncoder,
                        jwtService,
                        emailService
                );
    }

    @Test
    void publicRegistrationRejectsAdminRole() {
        RegisterRequest request =
                registerRequest(Role.ADMIN);

        assertThatThrownBy(() ->
                authService.register(request)
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Admin accounts cannot be created through public registration"
                );

        verify(userRepository, never())
                .save(any(User.class));
        verifyNoInteractions(emailService);
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {
                    "USER",
                    "HOST"
            }
    )
    void publicRegistrationAllowsUserAndHostRoles(
            Role role
    ) {
        RegisterRequest request =
                registerRequest(role);

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);
        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("hashed-password");

        authService.register(request);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(userCaptor.capture());
        verify(emailService)
                .sendVerificationCode(
                        eq(request.getEmail()),
                        anyString()
                );

        assertThat(userCaptor.getValue().getRole())
                .isEqualTo(role);
    }

    @Test
    void suspendedAccountCannotLogin() {
        LoginRequest request =
                loginRequest("suspended@example.com");

        User suspendedUser =
                user(
                        "suspended@example.com",
                        Role.USER,
                        AccountStatus.SUSPENDED
                );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(suspendedUser));
        when(passwordEncoder.matches(
                request.getPassword(),
                suspendedUser.getPassword()
        )).thenReturn(true);

        assertThatThrownBy(() ->
                authService.login(request)
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Account is suspended");

        verify(jwtService, never())
                .generateToken(any(User.class));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void activeVerifiedAccountsCanLogin(
            Role role
    ) {
        LoginRequest request =
                loginRequest(role.name().toLowerCase() + "@example.com");

        User activeUser =
                user(
                        request.getEmail(),
                        role,
                        AccountStatus.ACTIVE
                );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(
                request.getPassword(),
                activeUser.getPassword()
        )).thenReturn(true);
        when(jwtService.generateToken(activeUser))
                .thenReturn("jwt-token");

        AuthResponse response =
                authService.login(request);

        assertThat(response.getToken())
                .isEqualTo("jwt-token");
        assertThat(response.getEmail())
                .isEqualTo(activeUser.getEmail());
        assertThat(response.getRole())
                .isEqualTo(role);
    }

    private RegisterRequest registerRequest(
            Role role
    ) {
        RegisterRequest request =
                new RegisterRequest();

        request.setFirstName("Ada");
        request.setLastName("Guest");
        request.setEmail("ada@example.com");
        request.setPassword("password");
        request.setPhoneNumber("08000000000");
        request.setRole(role);

        return request;
    }

    private LoginRequest loginRequest(
            String email
    ) {
        LoginRequest request =
                new LoginRequest();

        request.setEmail(email);
        request.setPassword("password");

        return request;
    }

    private User user(
            String email,
            Role role,
            AccountStatus accountStatus
    ) {
        User user =
                User.builder()
                        .id(1L)
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .email(email)
                        .password("hashed-password")
                        .role(role)
                        .accountStatus(accountStatus)
                        .emailVerified(true)
                        .build();

        return user;
    }
}
