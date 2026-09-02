package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AdminDashboardResponse;
import com.elitebnb_backend.entity.*;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.PaymentRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboardResponse getDashboard() {

        // =========================
        // USERS
        // =========================

        long totalUsers =
                userRepository.count();

        long totalHosts =
                userRepository.countByRole(Role.HOST);

        long totalGuests =
                userRepository.countByRole(Role.USER);

        // =========================
        // PROPERTIES
        // =========================

        long totalProperties =
                propertyRepository.count();

        long activeProperties =
                propertyRepository.countByStatus(
                        PropertyStatus.ACTIVE
                );

        // =========================
        // BOOKINGS
        // =========================

        long totalBookings =
                bookingRepository.count();

        long pendingBookings =
                bookingRepository.countByStatus(
                        BookingStatus.PENDING
                );

        long confirmedBookings =
                bookingRepository.countByStatus(
                        BookingStatus.CONFIRMED
                );

        long completedBookings =
                bookingRepository.countByStatus(
                        BookingStatus.COMPLETED
                );

        long cancelledBookings =
                bookingRepository.countByStatus(
                        BookingStatus.CANCELLED
                );

        // =========================
        // PAYMENTS
        // =========================

        long totalPayments =
                paymentRepository.count();

        long successfulPayments =
                paymentRepository.countByStatus(
                        PaymentStatus.SUCCESS
                );

        long pendingPayments =
                paymentRepository.countByStatus(
                        PaymentStatus.PENDING
                );

        long failedPayments =
                paymentRepository.countByStatus(
                        PaymentStatus.FAILED
                );

        BigDecimal totalRevenue =
                paymentRepository.sumSuccessfulPayments();

        // =========================
        // RESPONSE
        // =========================

        return new AdminDashboardResponse(
                totalUsers,
                totalHosts,
                totalGuests,

                totalProperties,
                activeProperties,

                totalBookings,
                pendingBookings,
                confirmedBookings,
                completedBookings,
                cancelledBookings,

                totalPayments,
                successfulPayments,
                pendingPayments,
                failedPayments,

                totalRevenue
        );
    }
}