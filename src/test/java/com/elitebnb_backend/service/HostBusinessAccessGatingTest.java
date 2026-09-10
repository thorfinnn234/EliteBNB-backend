package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.CreatePropertyRequest;
import com.elitebnb_backend.dto.HostDashboardResponse;
import com.elitebnb_backend.dto.HostOnboardingRequest;
import com.elitebnb_backend.dto.HostOnboardingResponse;
import com.elitebnb_backend.dto.HostProfileResponse;
import com.elitebnb_backend.dto.PropertyResponse;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyApprovalStatus;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.PropertyAvailabilityRepository;
import com.elitebnb_backend.repository.PropertyImageRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostBusinessAccessGatingTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyImageRepository propertyImageRepository;

    @Mock
    private PropertyAvailabilityRepository propertyAvailabilityRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private AdminNotificationService adminNotificationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private HostAccessService hostAccessService;

    private PropertyVisibilityService propertyVisibilityService;
    private User host;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        propertyVisibilityService =
                new PropertyVisibilityService(
                        propertyRepository
                );

        host =
                verifiedHost();

        authentication =
                new UsernamePasswordAuthenticationToken(
                        host.getEmail(),
                        null,
                        List.of()
                );
    }

    /**
     * Host profile is part of the verification journey, so it must remain
     * available before the Host has normal business access.
     */
    @Test
    void unverifiedHostCanStillReadProfile() {
        HostProfileService hostProfileService =
                new HostProfileService(
                        userRepository,
                        cloudinaryService
                );

        when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));

        HostProfileResponse response =
                hostProfileService.getProfile(
                        authentication
                );

        assertThat(response.getEmail())
                .isEqualTo(host.getEmail());
    }

    /**
     * Host onboarding is also part of the allowed pre-verification path. This
     * proves the new business gate is not applied to onboarding completion work.
     */
    @Test
    void unverifiedHostCanStillSaveOnboardingProgress() {
        HostOnboardingService hostOnboardingService =
                new HostOnboardingService(
                        userRepository
                );

        HostOnboardingRequest request =
                new HostOnboardingRequest();

        request.setAddress("15 Marina Road");
        request.setCity("Lagos");
        request.setState("Lagos");
        request.setCountry("Nigeria");
        request.setCurrentStep(4);

        when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));
        when(userRepository.save(host))
                .thenReturn(host);

        HostOnboardingResponse response =
                hostOnboardingService.saveOnboarding(
                        host.getEmail(),
                        request
                );

        assertThat(response.getAddress())
                .isEqualTo("15 Marina Road");
        assertThat(response.getHostOnboardingStep())
                .isEqualTo(4);
    }

    /**
     * Creating listings is a normal Host business operation. An authenticated
     * but unverified Host must receive a 403-style denial before persistence.
     */
    @Test
    void unverifiedHostCannotCreateProperty() {
        PropertyService propertyService =
                propertyService();

        when(hostAccessService.getVerifiedHost(authentication))
                .thenThrow(businessAccessDenied());

        assertThatThrownBy(() ->
                propertyService.createProperty(
                        createPropertyRequest(),
                        authentication
                )
        ).isInstanceOf(AccessDeniedException.class)
                .hasMessage(
                        "Host verification is required before using Host business features"
                );

        verify(propertyRepository, never())
                .save(any(Property.class));
    }

    /**
     * Dashboard access exposes business metrics, so it uses the same central
     * verified-host check as mutating Host operations.
     */
    @Test
    void unverifiedHostCannotAccessDashboard() {
        HostDashboardService hostDashboardService =
                new HostDashboardService(
                        bookingRepository,
                        propertyRepository,
                        hostAccessService
                );

        when(hostAccessService.getVerifiedHost(authentication))
                .thenThrow(businessAccessDenied());

        assertThatThrownBy(() ->
                hostDashboardService.getDashboard(authentication)
        ).isInstanceOf(AccessDeniedException.class);

        verify(propertyRepository, never())
                .countByHost(any(User.class));
    }

    /**
     * Host reservation lists are business data and stay locked until Admin
     * verification succeeds.
     */
    @Test
    void unverifiedHostCannotAccessReservations() {
        BookingService bookingService =
                bookingService();

        when(hostAccessService.getVerifiedHost(authentication))
                .thenThrow(businessAccessDenied());

        assertThatThrownBy(() ->
                bookingService.getHostBookings(authentication)
        ).isInstanceOf(AccessDeniedException.class);

        verify(bookingRepository, never())
                .findByPropertyHost(any(User.class));
    }

    /**
     * Earnings are business metrics and should not be available to a Host who
     * has not passed verification.
     */
    @Test
    void unverifiedHostCannotAccessEarnings() {
        EarningsService earningsService =
                new EarningsService(
                        bookingRepository,
                        hostAccessService
                );

        when(hostAccessService.getVerifiedHost(authentication))
                .thenThrow(businessAccessDenied());

        assertThatThrownBy(() ->
                earningsService.getHostEarnings(authentication)
        ).isInstanceOf(AccessDeniedException.class);

        verify(bookingRepository, never())
                .sumHostRevenueByStatus(
                        any(User.class),
                        any(BookingStatus.class)
                );
    }

    /**
     * Once Admin marks the Host VERIFIED, the central guard returns the Host and
     * existing listing creation behavior continues, including pending approval.
     */
    @Test
    void verifiedHostCanStillCreateProperty() {
        PropertyService propertyService =
                propertyService();

        when(hostAccessService.getVerifiedHost(authentication))
                .thenReturn(host);
        when(propertyRepository.save(any(Property.class)))
                .thenAnswer(invocation -> {
                    Property property =
                            invocation.getArgument(0);

                    property.setId(100L);

                    return property;
                });
        when(propertyImageRepository.findByPropertyId(100L))
                .thenReturn(List.of());

        PropertyResponse response =
                propertyService.createProperty(
                        createPropertyRequest(),
                        authentication
                );

        assertThat(response.getHostId())
                .isEqualTo(host.getId());
        assertThat(response.getStatus())
                .isEqualTo(PropertyStatus.ACTIVE);
        assertThat(response.getApprovalStatus())
                .isEqualTo(PropertyApprovalStatus.PENDING_REVIEW);
    }

    /**
     * Verified Hosts retain representative dashboard access and receive the
     * same summary shape as before.
     */
    @Test
    void verifiedHostCanStillAccessDashboard() {
        HostDashboardService hostDashboardService =
                new HostDashboardService(
                        bookingRepository,
                        propertyRepository,
                        hostAccessService
                );

        when(hostAccessService.getVerifiedHost(authentication))
                .thenReturn(host);
        when(propertyRepository.countByHost(host))
                .thenReturn(3L);
        when(propertyRepository.countByHostAndStatus(
                host,
                PropertyStatus.ACTIVE
        )).thenReturn(2L);
        when(bookingRepository.countByPropertyHost(host))
                .thenReturn(5L);
        when(bookingRepository.countByPropertyHostAndStatus(
                host,
                BookingStatus.PENDING
        )).thenReturn(1L);
        when(bookingRepository.countByPropertyHostAndStatus(
                host,
                BookingStatus.CONFIRMED
        )).thenReturn(2L);
        when(bookingRepository.countByPropertyHostAndStatus(
                host,
                BookingStatus.COMPLETED
        )).thenReturn(1L);
        when(bookingRepository.countByPropertyHostAndStatus(
                host,
                BookingStatus.CANCELLED
        )).thenReturn(1L);
        when(bookingRepository.sumHostRevenueByStatus(
                host,
                BookingStatus.COMPLETED
        )).thenReturn(900.0);

        HostDashboardResponse response =
                hostDashboardService.getDashboard(
                        authentication
                );

        assertThat(response.getTotalListings())
                .isEqualTo(3L);
        assertThat(response.getActiveListings())
                .isEqualTo(2L);
        assertThat(response.getTotalReservations())
                .isEqualTo(5L);
        assertThat(response.getTotalEarnings())
                .isEqualTo(900.0);
    }

    private PropertyService propertyService() {

        return new PropertyService(
                propertyRepository,
                propertyImageRepository,
                propertyAvailabilityRepository,
                hostAccessService,
                propertyVisibilityService,
                cloudinaryService,
                adminNotificationService
        );
    }

    private BookingService bookingService() {

        return new BookingService(
                bookingRepository,
                userRepository,
                propertyAvailabilityRepository,
                notificationService,
                hostAccessService,
                propertyVisibilityService
        );
    }

    private AccessDeniedException businessAccessDenied() {

        return new AccessDeniedException(
                "Host verification is required before using Host business features"
        );
    }

    private CreatePropertyRequest createPropertyRequest() {
        CreatePropertyRequest request =
                new CreatePropertyRequest();

        request.setTitle("Lagos Loft");
        request.setDescription("A bright city stay");
        request.setLocation("Lagos");
        request.setPricePerNight(120.0);
        request.setBedrooms(2);
        request.setBathrooms(2);
        request.setMaxGuests(4);
        request.setPropertyType(PropertyType.APARTMENT);

        return request;
    }

    private User verifiedHost() {

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
}
