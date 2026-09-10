package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.HostProfileResponse;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    private HostProfileService hostProfileService;

    @BeforeEach
    void setUp() {
        hostProfileService =
                new HostProfileService(
                        userRepository,
                        cloudinaryService
                );
    }

    /**
     * GET /api/host/profile delegates to this mapper, so this verifies the
     * response contract uses the authenticated Host's persisted verification
     * state rather than a default or frontend-supplied value.
     */
    @Test
    void getProfileMapsAuthenticatedHostsRealEmailVerifiedValue() {
        assertEmailVerifiedMapping(true);
        assertEmailVerifiedMapping(false);
    }

    /**
     * Builds a separate authenticated Host for each boolean value so the test
     * proves both possible persisted states are returned unchanged.
     */
    private void assertEmailVerifiedMapping(
            boolean emailVerified
    ) {

        User host =
                User.builder()
                        .id(emailVerified ? 1L : 2L)
                        .firstName("Demo")
                        .lastName("Host")
                        .email(emailVerified
                                ? "verified-host@example.test"
                                : "unverified-host@example.test")
                        .password("encoded-password")
                        .role(Role.HOST)
                        .emailVerified(emailVerified)
                        .phoneNumber("+10000000000")
                        .bio("Host bio")
                        .location("Demo City")
                        .profileImageUrl("https://example.test/profile.jpg")
                        .build();

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        host.getEmail(),
                        null,
                        List.of()
                );

        when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));

        HostProfileResponse response =
                hostProfileService.getProfile(
                        authentication
                );

        assertThat(response.isEmailVerified())
                .isEqualTo(emailVerified);
    }
}
