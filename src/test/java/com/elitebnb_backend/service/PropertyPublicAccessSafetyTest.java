package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.BookingResponse;
import com.elitebnb_backend.dto.CreateBookingRequest;
import com.elitebnb_backend.dto.FavoriteResponse;
import com.elitebnb_backend.dto.PropertyResponse;
import com.elitebnb_backend.dto.UpdatePropertyRequest;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.Favorite;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyApprovalStatus;
import com.elitebnb_backend.entity.PropertyImage;
import com.elitebnb_backend.entity.PropertyImageType;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.FavoriteRepository;
import com.elitebnb_backend.repository.PropertyAvailabilityRepository;
import com.elitebnb_backend.repository.PropertyImageRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyPublicAccessSafetyTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyImageRepository propertyImageRepository;

    @Mock
    private PropertyAvailabilityRepository availabilityRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private HostAccessService hostAccessService;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private AdminNotificationService adminNotificationService;

    private PropertyVisibilityService propertyVisibilityService;
    private User guest;
    private User host;

    /**
     * Builds the shared visibility service with the same mocked repository used
     * by the services under test. This proves raw property-id flows all depend
     * on one authoritative rule instead of test-only stubbing.
     */
    @BeforeEach
    void setUp() {
        propertyVisibilityService =
                new PropertyVisibilityService(
                        propertyRepository
                );

        guest =
                user(
                        1L,
                        "guest@example.com",
                        "Jane",
                        "Guest",
                        Role.USER
                );

        host =
                user(
                        2L,
                        "host@example.com",
                        "Hakeem",
                        "Host",
                        Role.HOST
                );
    }

    /**
     * Public detail uses the shared visibility guard, so pending properties are
     * hidden even when a caller knows the raw id.
     */
    @Test
    void publicDetailDoesNotExposePendingReviewProperty() {
        Property property =
                property(
                        100L,
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.PENDING_REVIEW
                );

        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));

        assertThatThrownBy(() ->
                propertyService().getPropertyById(property.getId())
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Property not found");

        verify(propertyImageRepository, never())
                .findByPropertyId(property.getId());
    }

    /**
     * ACTIVE plus APPROVED still returns the same public detail response shape
     * for Explore and property-detail pages.
     */
    @Test
    void publicDetailReturnsActiveApprovedProperty() {
        Property property =
                property(
                        100L,
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.APPROVED
                );

        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));
        when(propertyImageRepository.findByPropertyId(property.getId()))
                .thenReturn(List.of());

        PropertyResponse response =
                propertyService().getPropertyById(property.getId());

        assertThat(response.getId())
                .isEqualTo(property.getId());
        assertThat(response.getApprovalStatus())
                .isEqualTo(PropertyApprovalStatus.APPROVED);
    }

    /**
     * A raw property id cannot book an operationally active listing that is
     * still awaiting Admin publication approval.
     */
    @Test
    void userCannotBookPendingReviewPropertyByRawId() {
        assertBookingRejectedFor(
                property(
                        100L,
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.PENDING_REVIEW
                )
        );
    }

    /**
     * A Host-disabled listing cannot be booked even if Admin had approved it.
     */
    @Test
    void userCannotBookInactivePropertyByRawId() {
        assertBookingRejectedFor(
                property(
                        100L,
                        PropertyStatus.INACTIVE,
                        PropertyApprovalStatus.APPROVED
                )
        );
    }

    /**
     * A suspended listing cannot be booked even if a stale frontend still has
     * the property id.
     */
    @Test
    void userCannotBookSuspendedPropertyByRawId() {
        assertBookingRejectedFor(
                property(
                        100L,
                        PropertyStatus.SUSPENDED,
                        PropertyApprovalStatus.APPROVED
                )
        );
    }

    /**
     * Rejected properties are also non-bookable through direct API requests.
     */
    @Test
    void userCannotBookRejectedPropertyByRawId() {
        assertBookingRejectedFor(
                property(
                        100L,
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.REJECTED
                )
        );
    }

    /**
     * Valid public properties keep the existing booking behavior: date and
     * guest checks run, amount is calculated, and a PENDING booking is saved.
     */
    @Test
    void userCanStillBookActiveApprovedProperty() {
        Property property =
                property(
                        100L,
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.APPROVED
                );

        when(userRepository.findByEmail(guest.getEmail()))
                .thenReturn(Optional.of(guest));
        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking booking =
                            invocation.getArgument(0);

                    booking.setId(500L);

                    return booking;
                });

        BookingResponse response =
                bookingService().createBooking(
                        validBookingRequest(property.getId()),
                        authenticationFor(guest)
                );

        assertThat(response.getId())
                .isEqualTo(500L);
        assertThat(response.getPropertyId())
                .isEqualTo(property.getId());
        assertThat(response.getTotalAmount())
                .isEqualTo(240.0);
    }

    /**
     * Wishlist creation now shares public visibility semantics with property
     * detail and booking, so hidden listings cannot be newly saved.
     */
    @Test
    void userCannotNewlyFavoriteNonPublicProperty() {
        Property property =
                property(
                        100L,
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.PENDING_REVIEW
                );

        when(userRepository.findByEmail(guest.getEmail()))
                .thenReturn(Optional.of(guest));
        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));

        assertThatThrownBy(() ->
                favoriteService().addFavorite(
                        property.getId(),
                        guest.getEmail()
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Property not found");

        verify(favoriteRepository, never())
                .save(any(Favorite.class));
    }

    /**
     * Existing favorite rows are not deleted when a listing becomes hidden, but
     * USER-facing wishlist responses stop surfacing the hidden property.
     */
    @Test
    void favoritesListOmitsNoLongerPublicPropertiesWithoutDeletingFavorite() {
        Property publicProperty =
                property(
                        100L,
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.APPROVED
                );
        Property hiddenProperty =
                property(
                        101L,
                        PropertyStatus.SUSPENDED,
                        PropertyApprovalStatus.APPROVED
                );

        Favorite publicFavorite =
                favorite(10L, guest, publicProperty);
        Favorite hiddenFavorite =
                favorite(11L, guest, hiddenProperty);

        when(userRepository.findByEmail(guest.getEmail()))
                .thenReturn(Optional.of(guest));
        when(favoriteRepository.findByUserOrderByCreatedAtDesc(guest))
                .thenReturn(List.of(hiddenFavorite, publicFavorite));

        List<FavoriteResponse> responses =
                favoriteService().getMyFavorites(guest.getEmail());

        assertThat(responses)
                .extracting(FavoriteResponse::getPropertyId)
                .containsExactly(publicProperty.getId());
        verify(favoriteRepository, never())
                .delete(any(Favorite.class));
    }

    /**
     * Public image reads are also raw-id public exposure, so a pending property
     * must not leak gallery URLs.
     */
    @Test
    void publicImageRetrievalDoesNotExposeNonPublicProperty() {
        Property property =
                property(
                        100L,
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.PENDING_REVIEW
                );

        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));

        assertThatThrownBy(() ->
                propertyService().getPropertyImages(property.getId())
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Property not found");

        verify(propertyImageRepository, never())
                .findByPropertyId(property.getId());
    }

    /**
     * Public availability reads expose bookability context, so they use the
     * same public rule before returning Host-blocked dates.
     */
    @Test
    void publicAvailabilityRetrievalDoesNotExposeNonPublicProperty() {
        Property property =
                property(
                        100L,
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.PENDING_REVIEW
                );

        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));

        assertThatThrownBy(() ->
                availabilityService().getBlockedDates(property.getId())
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Property not found");

        verify(availabilityRepository, never())
                .findByPropertyId(property.getId());
    }

    /**
     * Host management remains separate from public visibility. A verified owner
     * can still edit a PENDING_REVIEW listing while waiting for Admin approval.
     */
    @Test
    void verifiedOwningHostCanStillManagePendingReviewProperty() {
        Property property =
                property(
                        100L,
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.PENDING_REVIEW
                );
        UpdatePropertyRequest request =
                new UpdatePropertyRequest();

        request.setTitle("Updated Pending Loft");

        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));
        when(propertyRepository.save(property))
                .thenReturn(property);
        when(propertyImageRepository.findByPropertyId(property.getId()))
                .thenReturn(List.of());

        PropertyResponse response =
                propertyService().updateProperty(
                        property.getId(),
                        request,
                        authenticationFor(host)
                );

        assertThat(response.getTitle())
                .isEqualTo("Updated Pending Loft");
        assertThat(response.getApprovalStatus())
                .isEqualTo(PropertyApprovalStatus.PENDING_REVIEW);
        verify(hostAccessService)
                .requireVerifiedBusinessAccess(host.getEmail());
    }

    /**
     * Runs the booking rejection assertion for every non-public state.
     */
    private void assertBookingRejectedFor(
            Property property
    ) {

        when(userRepository.findByEmail(guest.getEmail()))
                .thenReturn(Optional.of(guest));
        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));

        assertThatThrownBy(() ->
                bookingService().createBooking(
                        validBookingRequest(property.getId()),
                        authenticationFor(guest)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Property is not currently bookable");

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    /**
     * Builds the booking service with real visibility enforcement and mocked
     * persistence collaborators.
     */
    private BookingService bookingService() {

        return new BookingService(
                bookingRepository,
                userRepository,
                availabilityRepository,
                notificationService,
                hostAccessService,
                propertyVisibilityService
        );
    }

    /**
     * Builds the property service used by public detail/image and Host
     * management tests.
     */
    private PropertyService propertyService() {

        return new PropertyService(
                propertyRepository,
                propertyImageRepository,
                availabilityRepository,
                hostAccessService,
                propertyVisibilityService,
                cloudinaryService,
                adminNotificationService
        );
    }

    /**
     * Builds the favorite service with the real visibility rule.
     */
    private FavoriteService favoriteService() {

        return new FavoriteService(
                favoriteRepository,
                propertyRepository,
                userRepository,
                propertyVisibilityService
        );
    }

    /**
     * Builds the availability service used by the public blocked-date read
     * regression test.
     */
    private AvailabilityService availabilityService() {

        return new AvailabilityService(
                availabilityRepository,
                propertyRepository,
                userRepository,
                hostAccessService,
                propertyVisibilityService
        );
    }

    /**
     * Creates a future two-night booking request for a known property id.
     */
    private CreateBookingRequest validBookingRequest(
            Long propertyId
    ) {

        CreateBookingRequest request =
                new CreateBookingRequest();

        request.setPropertyId(propertyId);
        request.setCheckIn(LocalDate.now().plusDays(3));
        request.setCheckOut(LocalDate.now().plusDays(5));
        request.setGuests(2);

        return request;
    }

    /**
     * Builds an authenticated Spring Security principal for service calls.
     */
    private Authentication authenticationFor(
            User user
    ) {

        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of()
        );
    }

    /**
     * Builds a public-facing or Host-owned property fixture with enough fields
     * for booking, favorites, and property response mapping.
     */
    private Property property(
            Long id,
            PropertyStatus status,
            PropertyApprovalStatus approvalStatus
    ) {

        return Property.builder()
                .id(id)
                .title("Lagos Loft")
                .description("A bright city stay")
                .location("Lagos")
                .pricePerNight(120.0)
                .bedrooms(2)
                .bathrooms(2)
                .maxGuests(4)
                .propertyType(PropertyType.APARTMENT)
                .status(status)
                .approvalStatus(approvalStatus)
                .host(host)
                .images(List.of(
                        propertyImage("cover.jpg", true)
                ))
                .build();
    }

    /**
     * Builds a favorite row without needing a database-generated timestamp.
     */
    private Favorite favorite(
            Long id,
            User user,
            Property property
    ) {

        return Favorite.builder()
                .id(id)
                .user(user)
                .property(property)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Builds an image fixture used by favorite response mapping.
     */
    private PropertyImage propertyImage(
            String imageUrl,
            boolean coverImage
    ) {

        return PropertyImage.builder()
                .id(200L)
                .imageUrl(imageUrl)
                .coverImage(coverImage)
                .imageType(PropertyImageType.EXTERIOR)
                .build();
    }

    /**
     * Builds a user fixture for guest and Host service scenarios.
     */
    private User user(
            Long id,
            String email,
            String firstName,
            String lastName,
            Role role
    ) {

        return User.builder()
                .id(id)
                .email(email)
                .password("encoded-password")
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }
}
