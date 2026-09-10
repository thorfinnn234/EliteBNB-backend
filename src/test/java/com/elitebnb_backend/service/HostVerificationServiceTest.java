package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.HostVerificationRequest;
import com.elitebnb_backend.dto.HostVerificationResponse;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.AdminNotificationType;
import com.elitebnb_backend.entity.HostVerification;
import com.elitebnb_backend.entity.HostVerificationStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.HostVerificationRepository;
import com.elitebnb_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostVerificationServiceTest {

    @Mock
    private HostVerificationRepository hostVerificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminNotificationService adminNotificationService;

    @Mock
    private AuditLogService auditLogService;

    private HostVerificationService hostVerificationService;

    @BeforeEach
    void setUp() {
        hostVerificationService =
                new HostVerificationService(
                        hostVerificationRepository,
                        userRepository,
                        adminNotificationService,
                        auditLogService
                );
    }

    /**
     * Verification submission now sits after Host onboarding in the lifecycle.
     * An incomplete onboarding flag must stop submission before any review row
     * is created or reset.
     */
    @Test
    void submissionFailsWhenOnboardingIncomplete() {
        User host =
                eligibleHost();
        host.setHostOnboardingCompleted(false);

        when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));

        assertThatThrownBy(() ->
                hostVerificationService.submitVerification(
                        validRequest(),
                        host.getEmail()
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Host profile/onboarding must be completed before verification submission"
                );

        verify(hostVerificationRepository, never())
                .findByHost(any(User.class));
        verify(hostVerificationRepository, never())
                .save(any(HostVerification.class));
        verifyNoInteractions(adminNotificationService);
    }

    /**
     * Email verification is part of the Host lifecycle gate. A Host can remain
     * authenticated while finishing setup, but cannot enter Admin review until
     * the email address has been confirmed.
     */
    @Test
    void submissionFailsWhenEmailIsNotVerified() {
        User host =
                eligibleHost();
        host.setEmailVerified(false);

        when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));

        assertThatThrownBy(() ->
                hostVerificationService.submitVerification(
                        validRequest(),
                        host.getEmail()
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Host profile/onboarding must be completed before verification submission"
                );

        verify(hostVerificationRepository, never())
                .findByHost(any(User.class));
        verify(hostVerificationRepository, never())
                .save(any(HostVerification.class));
        verifyNoInteractions(adminNotificationService);
    }

    /**
     * The onboarding flag alone is not enough; the actual required contact and
     * address fields must still be present when the Host submits verification.
     */
    @Test
    void submissionFailsWhenRequiredContactOrAddressDataIsIncomplete() {
        User host =
                eligibleHost();
        host.setPhoneNumber(" ");

        when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));

        assertThatThrownBy(() ->
                hostVerificationService.submitVerification(
                        validRequest(),
                        host.getEmail()
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Host profile/onboarding must be completed before verification submission"
                );

        verify(hostVerificationRepository, never())
                .save(any(HostVerification.class));
        verifyNoInteractions(adminNotificationService);
    }

    /**
     * Product now requires a profile photo before Admin receives a verification
     * request, using the existing profileImageUrl field.
     */
    @Test
    void submissionFailsWhenProfileImageIsMissing() {
        User host =
                eligibleHost();
        host.setProfileImageUrl(null);

        when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));

        assertThatThrownBy(() ->
                hostVerificationService.submitVerification(
                        validRequest(),
                        host.getEmail()
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Host profile/onboarding must be completed before verification submission"
                );

        verify(hostVerificationRepository, never())
                .save(any(HostVerification.class));
        verifyNoInteractions(adminNotificationService);
    }

    /**
     * Eligibility is checked before the existing verification row is fetched.
     * That order prevents incomplete resubmissions from resetting a previous
     * rejection back to PENDING or clearing Admin review metadata.
     */
    @Test
    void invalidSubmissionDoesNotLoadOrResetExistingVerificationRecord() {
        User host =
                eligibleHost();
        host.setPhoneNumber(null);

        HostVerification existingVerification =
                previouslyRejectedVerification(host);

        when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));

        assertThatThrownBy(() ->
                hostVerificationService.submitVerification(
                        validRequest(),
                        host.getEmail()
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Host profile/onboarding must be completed before verification submission"
                );

        verify(hostVerificationRepository, never())
                .findByHost(any(User.class));
        verify(hostVerificationRepository, never())
                .save(any(HostVerification.class));
        assertThat(existingVerification.getStatus())
                .isEqualTo(HostVerificationStatus.REJECTED);
        assertThat(existingVerification.getAdminNote())
                .isEqualTo("Upload a clearer document");
        assertThat(existingVerification.getReviewedBy().getRole())
                .isEqualTo(Role.ADMIN);
        assertThat(existingVerification.getReviewedAt())
                .isNotNull();
        verifyNoInteractions(adminNotificationService);
    }

    /**
     * A fully eligible Host can submit the existing verification payload. The
     * service still creates a single PENDING verification row and notifies Admin.
     */
    @Test
    void eligibleHostCanSubmitVerification() {
        User host =
                eligibleHost();

        when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));
        when(hostVerificationRepository.findByHost(host))
                .thenReturn(Optional.empty());
        when(hostVerificationRepository.save(any(HostVerification.class)))
                .thenAnswer(invocation -> {
                    HostVerification verification =
                            invocation.getArgument(0);

                    verification.setId(30L);

                    return verification;
                });

        HostVerificationResponse response =
                hostVerificationService.submitVerification(
                        validRequest(),
                        host.getEmail()
                );

        ArgumentCaptor<HostVerification> verificationCaptor =
                ArgumentCaptor.forClass(HostVerification.class);

        verify(hostVerificationRepository)
                .save(verificationCaptor.capture());

        HostVerification savedVerification =
                verificationCaptor.getValue();

        assertThat(savedVerification.getHost())
                .isSameAs(host);
        assertThat(savedVerification.getStatus())
                .isEqualTo(HostVerificationStatus.PENDING);
        assertThat(response.getStatus())
                .isEqualTo(HostVerificationStatus.PENDING);

        verify(adminNotificationService)
                .notifyAdmins(
                        eq("Host verification request"),
                        eq("Hakeem Host submitted verification details."),
                        eq(AdminNotificationType.HOST_VERIFICATION_REQUEST),
                        eq(30L),
                        eq("HOST_VERIFICATION")
                );
    }

    private User eligibleHost() {

        return User.builder()
                .id(10L)
                .firstName("Hakeem")
                .lastName("Host")
                .email("host@example.com")
                .password("hashed-password")
                .phoneNumber("08000000000")
                .profileImageUrl("https://cdn.example.com/profile.jpg")
                .address("15 Marina Road")
                .city("Lagos")
                .state("Lagos")
                .country("Nigeria")
                .role(Role.HOST)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .hostOnboardingCompleted(true)
                .build();
    }

    private HostVerificationRequest validRequest() {
        HostVerificationRequest request =
                new HostVerificationRequest();

        request.setLegalName("Hakeem Host");
        request.setBusinessName("Hakeem Homes");
        request.setDocumentType("PASSPORT");
        request.setDocumentUrl("https://cdn.example.com/document.pdf");

        return request;
    }

    /**
     * Builds a rejected verification fixture used to prove invalid resubmits do
     * not clear the previous Admin review state.
     */
    private HostVerification previouslyRejectedVerification(
            User host
    ) {

        return HostVerification.builder()
                .id(40L)
                .host(host)
                .legalName("Hakeem Host")
                .businessName("Hakeem Homes")
                .documentType("PASSPORT")
                .documentUrl("https://cdn.example.com/document.pdf")
                .status(HostVerificationStatus.REJECTED)
                .adminNote("Upload a clearer document")
                .reviewedBy(adminUser())
                .reviewedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    /**
     * Builds the reviewing Admin identity attached to the rejected verification
     * fixture.
     */
    private User adminUser() {

        return User.builder()
                .id(99L)
                .firstName("Ada")
                .lastName("Admin")
                .email("admin@example.com")
                .password("hashed-password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }
}
