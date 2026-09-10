package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.BookingResponse;
import com.elitebnb_backend.dto.CreateBookingRequest;
import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.NotificationType;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.PropertyAvailabilityRepository;
import com.elitebnb_backend.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PropertyAvailabilityRepository availabilityRepository;
    private final NotificationService notificationService;
    private final HostAccessService hostAccessService;
    private final PropertyVisibilityService propertyVisibilityService;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            PropertyAvailabilityRepository availabilityRepository,
            NotificationService notificationService,
            HostAccessService hostAccessService,
            PropertyVisibilityService propertyVisibilityService
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.availabilityRepository = availabilityRepository;
        this.notificationService = notificationService;
        this.hostAccessService = hostAccessService;
        this.propertyVisibilityService = propertyVisibilityService;
    }

    // CREATE BOOKING
    public BookingResponse createBooking(
            CreateBookingRequest request,
            Authentication authentication
    ) {

        // 1. Find logged-in user
        String email = authentication.getName();

        User guest = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        // 2. Find a public/bookable property. This raw-id path uses the same
        // visibility rule as public list/search/detail so pending, rejected,
        // inactive, or suspended listings cannot be booked by URL guessing.
        Property property =
                propertyVisibilityService
                        .requirePubliclyAccessible(
                                request.getPropertyId(),
                                "Property is not currently bookable"
                        );

        // 3. Validate dates
        LocalDate today = LocalDate.now();

        if (request.getCheckIn() == null
                || request.getCheckOut() == null) {

            throw new RuntimeException(
                    "Check-in and check-out dates are required"
            );
        }

        if (request.getCheckIn().isBefore(today)) {
            throw new RuntimeException(
                    "Check-in date cannot be in the past"
            );
        }

        if (!request.getCheckOut()
                .isAfter(request.getCheckIn())) {

            throw new RuntimeException(
                    "Check-out date must be after check-in date"
            );
        }

        // 4. Validate guest count
        if (request.getGuests() == null
                || request.getGuests() <= 0) {

            throw new RuntimeException(
                    "Guest count must be at least 1"
            );
        }

        if (request.getGuests() > property.getMaxGuests()) {
            throw new RuntimeException(
                    "This property allows a maximum of "
                            + property.getMaxGuests()
                            + " guests"
            );
        }

        // 5. Prevent host from booking own property
        if (property.getHost()
                .getId()
                .equals(guest.getId())) {

            throw new RuntimeException(
                    "You cannot book your own property"
            );
        }

        // 6. Check overlapping bookings
        boolean unavailable =
                bookingRepository.existsOverlappingBooking(
                        property.getId(),
                        request.getCheckIn(),
                        request.getCheckOut(),
                        List.of(
                                BookingStatus.PENDING,
                                BookingStatus.CONFIRMED
                        )
                );

        if (unavailable) {
            throw new RuntimeException(
                    "Property is not available for the selected dates"
            );
        }

        // 7. Check manually blocked dates
        boolean blockedByHost =
                availabilityRepository.existsOverlappingBlock(
                        property.getId(),
                        request.getCheckIn(),
                        request.getCheckOut()
                );

        if (blockedByHost) {
            throw new RuntimeException(
                    "Property is unavailable because the host blocked the selected dates"
            );
        }

        // 8. Calculate nights
        long nights = ChronoUnit.DAYS.between(
                request.getCheckIn(),
                request.getCheckOut()
        );

        // 9. Calculate amount
        double totalAmount =
                nights * property.getPricePerNight();

        // 10. Create booking
        Booking booking = Booking.builder()
                .property(property)
                .guest(guest)
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .guests(request.getGuests())
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .build();

        Booking savedBooking =
                bookingRepository.save(booking);

        // 11. Notify host
        notificationService.createNotification(
                property.getHost(),
                "New reservation request",
                guest.getFirstName()
                        + " "
                        + guest.getLastName()
                        + " requested to book "
                        + property.getTitle()
                        + " from "
                        + request.getCheckIn()
                        + " to "
                        + request.getCheckOut()
                        + ".",
                NotificationType.NEW_BOOKING,
                savedBooking,
                property
        );

        return mapToResponse(savedBooking);
    }

    // USER: GET MY BOOKINGS
    public List<BookingResponse> getMyBookings(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User guest = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return bookingRepository
                .findByGuest(guest)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // USER: CANCEL OWN PENDING BOOKING
    public BookingResponse cancelPendingBooking(
            Long bookingId,
            Authentication authentication
    ) {

        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found")
                );

        String email = authentication.getName();

        User guest = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        // Ownership is checked before status changes so a USER can only cancel
        // reservations that were returned by their own authenticated account.
        if (!booking.getGuest()
                .getId()
                .equals(guest.getId())) {

            throw new RuntimeException(
                    "You are not allowed to cancel this booking"
            );
        }

        // Bookings are cancelled instead of deleted so reservation history,
        // payment context, reviews, and host records remain auditable.
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException(
                    "Only pending reservations can be cancelled"
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Booking updatedBooking =
                bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getProperty().getHost(),
                "Reservation request cancelled",
                guest.getFirstName()
                        + " "
                        + guest.getLastName()
                        + " cancelled their pending reservation request for "
                        + booking.getProperty().getTitle()
                        + ".",
                NotificationType.BOOKING_CANCELLED,
                updatedBooking,
                booking.getProperty()
        );

        return mapToResponse(updatedBooking);
    }

    // HOST: GET RESERVATIONS
    public List<BookingResponse> getHostBookings(
            Authentication authentication
    ) {

        User host =
                hostAccessService.getVerifiedHost(
                        authentication
                );

        return bookingRepository
                .findByPropertyHost(host)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // HOST: UPDATE BOOKING STATUS
    public BookingResponse updateBookingStatus(
            Long bookingId,
            BookingStatus status,
            Authentication authentication
    ) {

        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found")
                );

        String email = authentication.getName();

        if (!booking.getProperty()
                .getHost()
                .getEmail()
                .equals(email)) {

            throw new RuntimeException(
                    "You are not allowed to manage this booking"
            );
        }

        hostAccessService.requireVerifiedBusinessAccess(email);

        BookingStatus oldStatus =
                booking.getStatus();

        booking.setStatus(status);

        Booking updatedBooking =
                bookingRepository.save(booking);

        // Only notify when status really changed
        if (oldStatus != status) {

            NotificationType type;
            String title;
            String message;

            switch (status) {

                case CONFIRMED -> {
                    type =
                            NotificationType.BOOKING_CONFIRMED;

                    title =
                            "Reservation confirmed";

                    message =
                            "Your reservation for "
                                    + booking.getProperty().getTitle()
                                    + " has been confirmed.";
                }

                case CANCELLED -> {
                    type =
                            NotificationType.BOOKING_CANCELLED;

                    title =
                            "Reservation cancelled";

                    message =
                            "Your reservation for "
                                    + booking.getProperty().getTitle()
                                    + " has been cancelled.";
                }

                case COMPLETED -> {
                    type =
                            NotificationType.BOOKING_COMPLETED;

                    title =
                            "Stay completed";

                    message =
                            "Your stay at "
                                    + booking.getProperty().getTitle()
                                    + " has been marked as completed.";
                }

                default -> {
                    type =
                            NotificationType.SYSTEM;

                    title =
                            "Reservation updated";

                    message =
                            "Your reservation for "
                                    + booking.getProperty().getTitle()
                                    + " is now "
                                    + status.name()
                                    + ".";
                }
            }

            notificationService.createNotification(
                    booking.getGuest(),
                    title,
                    message,
                    type,
                    updatedBooking,
                    booking.getProperty()
            );
        }

        return mapToResponse(updatedBooking);
    }

    // MAP BOOKING TO DTO
    private BookingResponse mapToResponse(
            Booking booking
    ) {

        return new BookingResponse(
                booking.getId(),

                booking.getProperty().getId(),
                booking.getProperty().getTitle(),

                booking.getGuest().getId(),
                booking.getGuest().getFirstName()
                        + " "
                        + booking.getGuest().getLastName(),

                booking.getCheckIn(),
                booking.getCheckOut(),

                booking.getGuests(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
