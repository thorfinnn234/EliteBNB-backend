package com.elitebnb_backend.repository;

import com.elitebnb_backend.dto.AdminBookingResponse;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.service.AdminBookingService;
import com.elitebnb_backend.service.AuditLogService;
import com.elitebnb_backend.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class BookingRepositoryAdminSearchTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    private AdminBookingService adminBookingService;

    private Booking confirmedBooking;
    private Property confirmedProperty;
    private User confirmedGuest;
    private User confirmedHost;

    @BeforeEach
    void setUp() {
        adminBookingService =
                new AdminBookingService(
                        bookingRepository,
                        paymentRepository,
                        mock(NotificationService.class),
                        mock(AuditLogService.class)
                );

        confirmedGuest =
                userRepository.save(
                        user(
                                "Marvellous",
                                "Akinbola",
                                "marvellous@example.com",
                                Role.USER
                        )
                );

        User anotherGuest =
                userRepository.save(
                        user(
                                "Ada",
                                "Lovelace",
                                "ada@example.com",
                                Role.USER
                        )
                );

        confirmedHost =
                userRepository.save(
                        user(
                                "Henry",
                                "Host",
                                "henry.host@example.com",
                                Role.HOST
                        )
                );

        User abujaHost =
                userRepository.save(
                        user(
                                "Hannah",
                                "Host",
                                "hannah.host@example.com",
                                Role.HOST
                        )
                );

        confirmedProperty =
                propertyRepository.save(
                        property(
                                "Ocean Loft",
                                "Lagos Island",
                                confirmedHost
                        )
                );

        Property gardenVilla =
                propertyRepository.save(
                        property(
                                "Garden Villa",
                                "Abuja",
                                abujaHost
                        )
                );

        bookingRepository.save(
                        booking(
                                confirmedProperty,
                                anotherGuest,
                                BookingStatus.PENDING
                        )
        );

        confirmedBooking =
                bookingRepository.save(
                        booking(
                                confirmedProperty,
                                confirmedGuest,
                                BookingStatus.CONFIRMED
                        )
                );

        bookingRepository.save(
                booking(
                        gardenVilla,
                        anotherGuest,
                        BookingStatus.CANCELLED
                )
        );

        bookingRepository.flush();
    }

    @Test
    void numericIdSearchWorks() {

        assertThat(idsFor(
                String.valueOf(confirmedBooking.getId()),
                null
        )).containsExactly(confirmedBooking.getId());
    }

    @Test
    void hashIdSearchWorks() {

        assertThat(idsFor(
                "#" + confirmedBooking.getId(),
                null
        )).containsExactly(confirmedBooking.getId());
    }

    @Test
    void bookingReferenceSearchWorks() {

        assertThat(idsFor(
                "Booking #" + confirmedBooking.getId(),
                null
        )).containsExactly(confirmedBooking.getId());
    }

    @Test
    void lowerCaseBookingReferenceSearchWorks() {

        assertThat(idsFor(
                "booking #" + confirmedBooking.getId(),
                null
        )).containsExactly(confirmedBooking.getId());
    }

    @Test
    void bookingReferenceWithoutHashSearchWorks() {

        assertThat(idsFor(
                "booking " + confirmedBooking.getId(),
                null
        )).containsExactly(confirmedBooking.getId());
    }

    @Test
    void existingGuestNameSearchStillWorks() {

        assertThat(idsFor(
                "Marvellous",
                null
        )).containsExactly(confirmedBooking.getId());
    }

    @Test
    void existingPropertySearchStillWorks() {

        assertThat(idsFor(
                "Ocean Loft",
                null
        )).contains(confirmedBooking.getId());
    }

    @Test
    void idSearchComposesWithStatusFilter() {

        assertThat(idsFor(
                "Booking #" + confirmedBooking.getId(),
                BookingStatus.CONFIRMED
        )).containsExactly(confirmedBooking.getId());

        assertThat(idsFor(
                "Booking #" + confirmedBooking.getId(),
                BookingStatus.PENDING
        )).isEmpty();
    }

    @Test
    void idSearchComposesWithEntityAndDateFilters() {

        assertThat(idsFor(
                "Booking #" + confirmedBooking.getId(),
                null,
                confirmedProperty.getId(),
                confirmedGuest.getId(),
                confirmedHost.getId(),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5)
        )).containsExactly(confirmedBooking.getId());

        assertThat(idsFor(
                "Booking #" + confirmedBooking.getId(),
                null,
                999999L,
                confirmedGuest.getId(),
                confirmedHost.getId(),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5)
        )).isEmpty();
    }

    @Test
    void unrelatedBookingNumberDoesNotMatch() {

        assertThat(idsFor(
                "Booking #999999",
                null
        )).isEmpty();
    }

    private List<Long> idsFor(
            String search,
            BookingStatus status
    ) {

        return adminBookingService
                .getBookings(
                        search,
                        status,
                        null,
                        null,
                        null,
                        null,
                        null
                )
                .stream()
                .map(AdminBookingResponse::getId)
                .toList();
    }

    private List<Long> idsFor(
            String search,
            BookingStatus status,
            Long propertyId,
            Long guestId,
            Long hostId,
            LocalDate from,
            LocalDate to
    ) {

        return adminBookingService
                .getBookings(
                        search,
                        status,
                        propertyId,
                        guestId,
                        hostId,
                        from,
                        to
                )
                .stream()
                .map(AdminBookingResponse::getId)
                .toList();
    }

    private User user(
            String firstName,
            String lastName,
            String email,
            Role role
    ) {

        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password("encoded-password")
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    private Property property(
            String title,
            String location,
            User host
    ) {

        return Property.builder()
                .title(title)
                .description("A beautiful place to stay.")
                .location(location)
                .pricePerNight(120.0)
                .bedrooms(2)
                .bathrooms(2)
                .maxGuests(4)
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .host(host)
                .build();
    }

    private Booking booking(
            Property property,
            User guest,
            BookingStatus status
    ) {

        return Booking.builder()
                .property(property)
                .guest(guest)
                .checkIn(LocalDate.of(2026, 10, 1))
                .checkOut(LocalDate.of(2026, 10, 5))
                .guests(2)
                .totalAmount(480.0)
                .status(status)
                .build();
    }
}
