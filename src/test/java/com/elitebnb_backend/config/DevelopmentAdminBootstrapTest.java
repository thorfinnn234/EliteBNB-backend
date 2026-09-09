package com.elitebnb_backend.config;

import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevelopmentAdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    private DevelopmentAdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        bootstrap =
                new DevelopmentAdminBootstrap(
                        userRepository,
                        passwordEncoder,
                        environment
                );
    }

    @Test
    void disabledBootstrapDoesNotCreateAdmin() {

        when(environment.getProperty(
                DevelopmentAdminBootstrap.ENABLED_PROPERTY,
                "false"
        )).thenReturn("false");

        bootstrap.run(null);

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void enabledBootstrapCreatesActiveVerifiedAdminWithEncodedPassword() {

        when(environment.getProperty(
                DevelopmentAdminBootstrap.ENABLED_PROPERTY,
                "false"
        )).thenReturn("true");
        when(environment.getProperty(
                DevelopmentAdminBootstrap.EMAIL_PROPERTY
        )).thenReturn("admin@example.com");
        when(environment.getProperty(
                DevelopmentAdminBootstrap.PASSWORD_PROPERTY
        )).thenReturn("plain-password");
        when(environment.getProperty(
                DevelopmentAdminBootstrap.FIRST_NAME_PROPERTY
        )).thenReturn("Ada");
        when(environment.getProperty(
                DevelopmentAdminBootstrap.LAST_NAME_PROPERTY
        )).thenReturn("Admin");
        when(userRepository.existsByEmail("admin@example.com"))
                .thenReturn(false);
        when(userRepository.countByRole(Role.ADMIN))
                .thenReturn(0L);
        when(passwordEncoder.encode("plain-password"))
                .thenReturn("encoded-password");

        bootstrap.run(null);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(userCaptor.capture());

        User createdAdmin =
                userCaptor.getValue();

        assertThat(createdAdmin.getEmail())
                .isEqualTo("admin@example.com");
        assertThat(createdAdmin.getFirstName())
                .isEqualTo("Ada");
        assertThat(createdAdmin.getLastName())
                .isEqualTo("Admin");
        assertThat(createdAdmin.getRole())
                .isEqualTo(Role.ADMIN);
        assertThat(createdAdmin.getAccountStatus())
                .isEqualTo(AccountStatus.ACTIVE);
        assertThat(createdAdmin.isEmailVerified())
                .isTrue();
        assertThat(createdAdmin.getPassword())
                .isEqualTo("encoded-password")
                .isNotEqualTo("plain-password");
    }

    @Test
    void enabledBootstrapDoesNotModifyExistingEmail() {

        when(environment.getProperty(
                DevelopmentAdminBootstrap.ENABLED_PROPERTY,
                "false"
        )).thenReturn("true");
        when(environment.getProperty(
                DevelopmentAdminBootstrap.EMAIL_PROPERTY
        )).thenReturn("existing@example.com");
        when(environment.getProperty(
                DevelopmentAdminBootstrap.PASSWORD_PROPERTY
        )).thenReturn("plain-password");
        when(userRepository.existsByEmail("existing@example.com"))
                .thenReturn(true);

        bootstrap.run(null);

        verify(userRepository, never())
                .countByRole(Role.ADMIN);
        verify(userRepository, never())
                .save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void enabledBootstrapDoesNotCreateSecondAdmin() {

        when(environment.getProperty(
                DevelopmentAdminBootstrap.ENABLED_PROPERTY,
                "false"
        )).thenReturn("true");
        when(environment.getProperty(
                DevelopmentAdminBootstrap.EMAIL_PROPERTY
        )).thenReturn("second-admin@example.com");
        when(environment.getProperty(
                DevelopmentAdminBootstrap.PASSWORD_PROPERTY
        )).thenReturn("plain-password");
        when(userRepository.existsByEmail("second-admin@example.com"))
                .thenReturn(false);
        when(userRepository.countByRole(Role.ADMIN))
                .thenReturn(1L);

        bootstrap.run(null);

        verify(userRepository, never())
                .save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void enabledBootstrapFailsSafelyWhenRequiredCredentialsAreMissing() {

        when(environment.getProperty(
                DevelopmentAdminBootstrap.ENABLED_PROPERTY,
                "false"
        )).thenReturn("true");
        when(environment.getProperty(
                DevelopmentAdminBootstrap.EMAIL_PROPERTY
        )).thenReturn("admin@example.com");
        when(environment.getProperty(
                DevelopmentAdminBootstrap.PASSWORD_PROPERTY
        )).thenReturn("   ");

        assertThatThrownBy(() ->
                bootstrap.run(null)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Development admin bootstrap requires "
                                + DevelopmentAdminBootstrap.PASSWORD_PROPERTY
                                + " when "
                                + DevelopmentAdminBootstrap.ENABLED_PROPERTY
                                + " is true"
                );

        verifyNoInteractions(
                userRepository,
                passwordEncoder
        );
    }
}
