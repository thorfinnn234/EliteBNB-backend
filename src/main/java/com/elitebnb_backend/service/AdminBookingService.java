package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AdminBookingResponse;
import com.elitebnb_backend.entity.AuditActionType;
import com.elitebnb_backend.entity.AuditTargetType;
import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.NotificationType;
import com.elitebnb_backend.entity.Payment;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public List<AdminBookingResponse> getBookings(
            String search,
            BookingStatus status,
            Long propertyId,
            Long guestId,
            Long hostId,
            LocalDate from,
            LocalDate to
    ) {

        if (from != null
                && to != null
                && to.isBefore(from)) {

            throw new RuntimeException(
                    "End date cannot be before start date"
            );
        }

        return bookingRepository
                .searchAdminBookings(
                        search,
                        status,
                        propertyId,
                        guestId,
                        hostId,
                        from,
                        to
                )
                .stream()
                .map(this::map)
                .toList();
    }

    public AdminBookingResponse getBooking(
            Long bookingId
    ) {

        Booking booking =
                getBookingEntity(bookingId);

        return map(booking);
    }

    public AdminBookingResponse updateStatus(
            Long bookingId,
            BookingStatus status,
            String adminEmail
    ) {

        if (status == null) {
            throw new RuntimeException(
                    "Booking status is required"
            );
        }

        Booking booking =
                getBookingEntity(bookingId);

        BookingStatus oldStatus =
                booking.getStatus();

        booking.setStatus(status);

        Booking saved =
                bookingRepository.save(booking);

        notifyGuestWhenStatusChanged(
                saved,
                oldStatus,
                status
        );

        auditLogService.record(
                adminEmail,
                AuditActionType.BOOKING_STATUS_UPDATED,
                AuditTargetType.BOOKING,
                saved.getId(),
                "Updated booking #"
                        + saved.getId()
                        + " status from "
                        + oldStatus.name()
                        + " to "
                        + status.name()
        );

        return map(saved);
    }

    private Booking getBookingEntity(
            Long bookingId
    ) {

        return bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found"
                        )
                );
    }

    private void notifyGuestWhenStatusChanged(
            Booking booking,
            BookingStatus oldStatus,
            BookingStatus newStatus
    ) {

        if (oldStatus == newStatus) {
            return;
        }

        NotificationType type;
        String title;
        String message;

        switch (newStatus) {

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
                                + newStatus.name()
                                + ".";
            }
        }

        notificationService.createNotification(
                booking.getGuest(),
                title,
                message,
                type,
                booking,
                booking.getProperty()
        );
    }

    private AdminBookingResponse map(
            Booking booking
    ) {

        String hostName =
                booking.getProperty().getHost().getFirstName()
                        + " "
                        + booking.getProperty().getHost().getLastName();

        String guestName =
                booking.getGuest().getFirstName()
                        + " "
                        + booking.getGuest().getLastName();

        Optional<Payment> payment =
                paymentRepository.findByBooking(booking);

        return new AdminBookingResponse(
                booking.getId(),

                booking.getProperty().getId(),
                booking.getProperty().getTitle(),
                booking.getProperty().getLocation(),

                booking.getProperty().getHost().getId(),
                hostName,
                booking.getProperty().getHost().getEmail(),

                booking.getGuest().getId(),
                guestName,
                booking.getGuest().getEmail(),

                booking.getCheckIn(),
                booking.getCheckOut(),

                booking.getGuests(),
                booking.getTotalAmount(),
                booking.getStatus(),

                payment.map(Payment::getId)
                        .orElse(null),
                payment.map(Payment::getStatus)
                        .orElse(null),
                payment.map(Payment::getAmount)
                        .orElse(null),
                payment.map(Payment::getCurrency)
                        .orElse(null),

                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
