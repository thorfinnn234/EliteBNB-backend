package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.HostDashboardResponse;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class HostDashboardService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public HostDashboardService(
            BookingRepository bookingRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    public HostDashboardResponse getDashboard(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User host = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Host not found")
                );

        long totalListings =
                propertyRepository.countByHost(host);

        long activeListings =
                propertyRepository.countByHostAndStatus(
                        host,
                        PropertyStatus.ACTIVE
                );

        long totalReservations =
                bookingRepository.countByPropertyHost(host);

        long pendingReservations =
                bookingRepository
                        .countByPropertyHostAndStatus(
                                host,
                                BookingStatus.PENDING
                        );

        long confirmedReservations =
                bookingRepository
                        .countByPropertyHostAndStatus(
                                host,
                                BookingStatus.CONFIRMED
                        );

        long completedReservations =
                bookingRepository
                        .countByPropertyHostAndStatus(
                                host,
                                BookingStatus.COMPLETED
                        );

        long cancelledReservations =
                bookingRepository
                        .countByPropertyHostAndStatus(
                                host,
                                BookingStatus.CANCELLED
                        );

        Double totalEarnings =
                bookingRepository
                        .sumHostRevenueByStatus(
                                host,
                                BookingStatus.COMPLETED
                        );

        if (totalEarnings == null) {
            totalEarnings = 0.0;
        }

        return new HostDashboardResponse(
                totalListings,
                activeListings,
                totalReservations,
                pendingReservations,
                confirmedReservations,
                completedReservations,
                cancelledReservations,
                totalEarnings
        );
    }
}