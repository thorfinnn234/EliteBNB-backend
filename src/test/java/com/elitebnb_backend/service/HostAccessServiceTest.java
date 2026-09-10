package com.elitebnb_backend.service;

import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.HostVerification;
import com.elitebnb_backend.entity.HostVerificationStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.HostVerificationRepository;
import com.elitebnb_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostAccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HostVerificationRepository hostVerificationRepository;

    private HostAccessService hostAccessService;

    @BeforeEach
    void setUp() {
        hostAccessService =
                new HostAccessService(
                        userRepository,
                        hostVerificationRepository
                );
    }

    /**
     * PENDING verification means the Host is in the review queue, not yet
     * allowed to use normal business tools.
     */
    @Test
    void pendingHostFailsBusinessAccessCheck() {
        User host =
                host(AccountStatus.ACTIVE);

        when(hostVerificationRepository.findByHost(host))
                .thenReturn(Optional.of(
                        verification(
                                host,
                                HostVerificationStatus.PENDING
                        )
                ));

        assertThat(hostAccessService.hasBusinessAccess(host))
                .isFalse();
        assertThatThrownBy(() ->
                hostAccessService.requireVerifiedBusinessAccess(host)
        ).isInstanceOf(AccessDeniedException.class)
                .hasMessage(
                        "Host verification is required before using Host business features"
                );
    }

    /**
     * REJECTED verification keeps the Host authenticated for remediation, but
     * still locked out of business operations.
     */
    @Test
    void rejectedHostFailsBusinessAccessCheck() {
        User host =
                host(AccountStatus.ACTIVE);

        when(hostVerificationRepository.findByHost(host))
                .thenReturn(Optional.of(
                        verification(
                                host,
                                HostVerificationStatus.REJECTED
                        )
                ));

        assertThat(hostAccessService.hasBusinessAccess(host))
                .isFalse();
    }

    /**
     * VERIFIED is immediately authoritative because HostVerification is the
     * only source of truth for business access.
     */
    @Test
    void verifiedActiveHostPassesBusinessAccessCheck() {
        User host =
                host(AccountStatus.ACTIVE);

        when(hostVerificationRepository.findByHost(host))
                .thenReturn(Optional.of(
                        verification(
                                host,
                                HostVerificationStatus.VERIFIED
                        )
                ));

        assertThat(hostAccessService.hasBusinessAccess(host))
                .isTrue();
    }

    /**
     * Account suspension wins over verification status, so a suspended Host
     * cannot keep using business APIs after Admin action.
     */
    @Test
    void suspendedHostFailsEvenIfVerificationCouldBeVerified() {
        User host =
                host(AccountStatus.SUSPENDED);

        assertThat(hostAccessService.hasBusinessAccess(host))
                .isFalse();
        assertThatThrownBy(() ->
                hostAccessService.requireVerifiedBusinessAccess(host)
        ).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(hostVerificationRepository);
    }

    /**
     * Email-based enforcement is what controller-backed services use when they
     * receive only the authenticated principal name.
     */
    @Test
    void emailBasedCheckReturnsVerifiedHost() {
        User host =
                host(AccountStatus.ACTIVE);

        when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));
        when(hostVerificationRepository.findByHost(host))
                .thenReturn(Optional.of(
                        verification(
                                host,
                                HostVerificationStatus.VERIFIED
                        )
                ));

        assertThat(hostAccessService
                .requireVerifiedBusinessAccess(host.getEmail()))
                .isSameAs(host);
    }

    private User host(
            AccountStatus accountStatus
    ) {

        return User.builder()
                .id(10L)
                .firstName("Hakeem")
                .lastName("Host")
                .email("host@example.com")
                .password("hashed-password")
                .role(Role.HOST)
                .accountStatus(accountStatus)
                .emailVerified(true)
                .build();
    }

    private HostVerification verification(
            User host,
            HostVerificationStatus status
    ) {

        return HostVerification.builder()
                .id(20L)
                .host(host)
                .legalName("Hakeem Host")
                .documentType("PASSPORT")
                .status(status)
                .build();
    }
}
