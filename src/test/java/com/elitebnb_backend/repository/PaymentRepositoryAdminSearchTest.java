package com.elitebnb_backend.repository;

import com.elitebnb_backend.dto.AdminPaymentResponse;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.Payment;
import com.elitebnb_backend.entity.PaymentProvider;
import com.elitebnb_backend.entity.PaymentStatus;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.service.AdminPaymentService;
import com.elitebnb_backend.service.AuditLogService;
import com.elitebnb_backend.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class PaymentRepositoryAdminSearchTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    private AdminPaymentService adminPaymentService;

    private Payment successfulPayment;
    private Booking successfulBooking;
    private Property successfulProperty;
    private User successfulGuest;
    private User successfulHost;

    @BeforeEach
    void setUp() {
        adminPaymentService =
                new AdminPaymentService(
                        paymentRepository,
                        bookingRepository,
                        mock(NotificationService.class),
                        mock(AuditLogService.class)
                );

        successfulGuest =
                userRepository.save(
                        user(
                                "Marvellous",
                                "Akinbola",
                                "marvellous@example.com",
                                Role.USER
                        )
                );

        User otherGuest =
                userRepository.save(
                        user(
                                "Ada",
                                "Lovelace",
                                "ada@example.com",
                                Role.USER
                        )
                );

        successfulHost =
                userRepository.save(
                        user(
                                "Henry",
                                "Host",
                                "henry.host@example.com",
                                Role.HOST
                        )
                );

        User otherHost =
                userRepository.save(
                        user(
                                "Hannah",
                                "Host",
                                "hannah.host@example.com",
                                Role.HOST
                        )
                );

        successfulProperty =
                propertyRepository.save(
                        property(
                                "Ocean Loft",
                                "Lagos Island",
                                successfulHost
                        )
                );

        Property otherProperty =
                propertyRepository.save(
                        property(
                                "Garden Villa",
                                "Abuja",
                                otherHost
                        )
                );

        successfulBooking =
                bookingRepository.save(
                        booking(
                                successfulProperty,
                                successfulGuest,
                                BookingStatus.CONFIRMED
                        )
                );

        Booking otherBooking =
                bookingRepository.save(
                        booking(
                                otherProperty,
                                otherGuest,
                                BookingStatus.PENDING
                        )
                );

        successfulPayment =
                paymentRepository.save(
                        payment(
                                successfulBooking,
                                successfulGuest,
                                PaymentStatus.SUCCESS,
                                "ELITEBNB-3-4B1E765B"
                        )
                );

        paymentRepository.save(
                payment(
                        otherBooking,
                        otherGuest,
                        PaymentStatus.PENDING,
                        "ELITEBNB-9-OTHER"
                )
        );

        paymentRepository.flush();
    }

    @Test
    void guestFirstNameSearchWorks() {

        assertThat(idsFor(
                "Marvellous",
                null
        )).containsExactly(successfulPayment.getId());
    }

    @Test
    void guestLastNameSearchWorks() {

        assertThat(idsFor(
                "Akinbola",
                null
        )).containsExactly(successfulPayment.getId());
    }

    @Test
    void guestFullNameSearchWorksWithTrimmedWhitespace() {

        assertThat(idsFor(
                "  Marvellous Akinbola  ",
                null
        )).containsExactly(successfulPayment.getId());
    }

    @Test
    void hostFullNameSearchWorks() {

        assertThat(idsFor(
                "Henry Host",
                null
        )).containsExactly(successfulPayment.getId());
    }

    @Test
    void fullNameSearchIsCaseInsensitive() {

        assertThat(idsFor(
                "MARVELLOUS AKINBOLA",
                null
        )).containsExactly(successfulPayment.getId());
    }

    @Test
    void emailSearchStillWorks() {

        assertThat(idsFor(
                "marvellous@example.com",
                null
        )).containsExactly(successfulPayment.getId());
    }

    @Test
    void paymentReferenceSearchStillWorks() {

        assertThat(idsFor(
                "4B1E765B",
                null
        )).containsExactly(successfulPayment.getId());
    }

    @Test
    void propertyTitleSearchStillWorks() {

        assertThat(idsFor(
                "Ocean Loft",
                null
        )).containsExactly(successfulPayment.getId());
    }

    @Test
    void searchComposesWithStatusFilter() {

        assertThat(idsFor(
                "Marvellous Akinbola",
                PaymentStatus.SUCCESS
        )).containsExactly(successfulPayment.getId());

        assertThat(idsFor(
                "Marvellous Akinbola",
                PaymentStatus.PENDING
        )).isEmpty();
    }

    @Test
    void searchComposesWithEntityFilters() {

        assertThat(idsFor(
                "Marvellous Akinbola",
                PaymentStatus.SUCCESS,
                PaymentProvider.PAYSTACK,
                successfulBooking.getId(),
                successfulGuest.getId(),
                successfulProperty.getId(),
                successfulHost.getId()
        )).containsExactly(successfulPayment.getId());

        assertThat(idsFor(
                "Marvellous Akinbola",
                PaymentStatus.SUCCESS,
                PaymentProvider.PAYSTACK,
                successfulBooking.getId(),
                successfulGuest.getId(),
                999999L,
                successfulHost.getId()
        )).isEmpty();
    }

    @Test
    void unrelatedNameDoesNotMatch() {

        assertThat(idsFor(
                "Grace Hopper",
                null
        )).isEmpty();
    }

    private List<Long> idsFor(
            String search,
            PaymentStatus status
    ) {

        return idsFor(
                search,
                status,
                null,
                null,
                null,
                null,
                null
        );
    }

    private List<Long> idsFor(
            String search,
            PaymentStatus status,
            PaymentProvider provider,
            Long bookingId,
            Long guestId,
            Long propertyId,
            Long hostId
    ) {

        return adminPaymentService
                .getPayments(
                        search,
                        status,
                        provider,
                        bookingId,
                        guestId,
                        propertyId,
                        hostId,
                        null,
                        null
                )
                .stream()
                .map(AdminPaymentResponse::getId)
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

    private Payment payment(
            Booking booking,
            User user,
            PaymentStatus status,
            String reference
    ) {

        return Payment.builder()
                .booking(booking)
                .user(user)
                .amount(BigDecimal.valueOf(480.00))
                .currency("NGN")
                .reference(reference)
                .status(status)
                .provider(PaymentProvider.PAYSTACK)
                .build();
    }
}
