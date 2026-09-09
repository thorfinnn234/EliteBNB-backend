package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AdminPaymentResponse;
import com.elitebnb_backend.entity.AuditActionType;
import com.elitebnb_backend.entity.AuditTargetType;
import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.NotificationType;
import com.elitebnb_backend.entity.Payment;
import com.elitebnb_backend.entity.PaymentProvider;
import com.elitebnb_backend.entity.PaymentStatus;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public List<AdminPaymentResponse> getPayments(
            String search,
            PaymentStatus status,
            PaymentProvider provider,
            Long bookingId,
            Long guestId,
            Long propertyId,
            Long hostId,
            LocalDate from,
            LocalDate to
    ) {

        String normalizedSearch =
                normalizeSearch(search);

        if (from != null
                && to != null
                && to.isBefore(from)) {

            throw new RuntimeException(
                    "End date cannot be before start date"
            );
        }

        LocalDateTime fromDateTime =
                from != null
                        ? from.atStartOfDay()
                        : null;

        LocalDateTime toDateTime =
                to != null
                        ? to.atTime(LocalTime.MAX)
                        : null;

        return paymentRepository
                .searchAdminPayments(
                        normalizedSearch,
                        status,
                        provider,
                        bookingId,
                        guestId,
                        propertyId,
                        hostId
                )
                .stream()
                .map(this::map)
                .toList();
    }

    /**
     * Trims Admin payment search once before it reaches the repository query.
     * Returning null for blank input keeps the existing "no search filter"
     * behavior while allowing multi-word full-name searches to match cleanly.
     */
    private String normalizeSearch(
            String search
    ) {

        if (search == null) {
            return null;
        }

        String trimmedSearch =
                search.trim();

        return trimmedSearch.isEmpty()
                ? null
                : trimmedSearch;
    }

    public AdminPaymentResponse getPayment(
            Long paymentId
    ) {

        Payment payment =
                getPaymentEntity(paymentId);

        return map(payment);
    }

    public AdminPaymentResponse updateStatus(
            Long paymentId,
            PaymentStatus status,
            String adminEmail
    ) {

        if (status == null) {
            throw new RuntimeException(
                    "Payment status is required"
            );
        }

        if (status == PaymentStatus.SUCCESS) {
            throw new RuntimeException(
                    "Successful payments must be confirmed through Paystack verification"
            );
        }

        Payment payment =
                getPaymentEntity(paymentId);

        PaymentStatus oldStatus =
                payment.getStatus();

        payment.setStatus(status);

        if (status == PaymentStatus.SUCCESS
                && payment.getPaidAt() == null) {

            payment.setPaidAt(
                    LocalDateTime.now()
            );
        }

        if (status != PaymentStatus.SUCCESS) {
            payment.setPaidAt(null);
        }

        Payment saved =
                paymentRepository.save(payment);

        syncBookingStatus(saved);

        notifyGuestWhenStatusChanged(
                saved,
                oldStatus,
                status
        );

        auditLogService.record(
                adminEmail,
                AuditActionType.PAYMENT_STATUS_UPDATED,
                AuditTargetType.PAYMENT,
                saved.getId(),
                "Updated payment "
                        + saved.getReference()
                        + " status from "
                        + oldStatus.name()
                        + " to "
                        + status.name()
        );

        return map(saved);
    }

    private Payment getPaymentEntity(
            Long paymentId
    ) {

        return paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found"
                        )
                );
    }

    private void syncBookingStatus(
            Payment payment
    ) {

        Booking booking =
                payment.getBooking();

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            booking.setStatus(
                    BookingStatus.CONFIRMED
            );

            bookingRepository.save(booking);

            return;
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(
                    BookingStatus.PENDING
            );

            bookingRepository.save(booking);
        }
    }

    private void notifyGuestWhenStatusChanged(
            Payment payment,
            PaymentStatus oldStatus,
            PaymentStatus newStatus
    ) {

        if (oldStatus == newStatus) {
            return;
        }

        String message =
                "Your payment for "
                        + payment.getBooking().getProperty().getTitle()
                        + " is now "
                        + newStatus.name()
                        + ".";

        notificationService.createNotification(
                payment.getUser(),
                "Payment updated",
                message,
                NotificationType.SYSTEM,
                payment.getBooking(),
                payment.getBooking().getProperty()
        );
    }

    private AdminPaymentResponse map(
            Payment payment
    ) {

        Booking booking =
                payment.getBooking();

        String guestName =
                payment.getUser().getFirstName()
                        + " "
                        + payment.getUser().getLastName();

        String hostName =
                booking.getProperty().getHost().getFirstName()
                        + " "
                        + booking.getProperty().getHost().getLastName();

        return new AdminPaymentResponse(
                payment.getId(),
                booking.getId(),

                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),

                payment.getStatus(),
                payment.getProvider(),

                payment.getUser().getId(),
                guestName,
                payment.getUser().getEmail(),

                booking.getProperty().getId(),
                booking.getProperty().getTitle(),
                booking.getProperty().getLocation(),

                booking.getProperty().getHost().getId(),
                hostName,
                booking.getProperty().getHost().getEmail(),

                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getStatus(),

                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
