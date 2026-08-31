package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.HostEarningsResponse;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class EarningsService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public EarningsService(
            BookingRepository bookingRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    public HostEarningsResponse getHostEarnings(
            Authentication authentication
    ) {

        User host = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Host not found")
                );

        double confirmedRevenue =
                bookingRepository.sumHostRevenueByStatus(
                        host,
                        BookingStatus.CONFIRMED
                );

        double completedRevenue =
                bookingRepository.sumHostRevenueByStatus(
                        host,
                        BookingStatus.COMPLETED
                );

        double pendingRevenue =
                bookingRepository.sumHostRevenueByStatus(
                        host,
                        BookingStatus.PENDING
                );

        double totalEarnings =
                confirmedRevenue + completedRevenue;

        long totalReservations =
                bookingRepository.countByPropertyHost(host);

        long completedReservations =
                bookingRepository
                        .countByPropertyHostAndStatus(
                                host,
                                BookingStatus.COMPLETED
                        );

        return new HostEarningsResponse(
                totalEarnings,
                confirmedRevenue,
                completedRevenue,
                pendingRevenue,
                totalReservations,
                completedReservations
        );
    }
}