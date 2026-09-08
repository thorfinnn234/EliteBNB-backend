package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AdminAnalyticsOverviewResponse;
import com.elitebnb_backend.dto.AnalyticsDataPointResponse;
import com.elitebnb_backend.entity.*;
import com.elitebnb_backend.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ModerationReportRepository moderationReportRepository;
    private final HostVerificationRepository hostVerificationRepository;
    private final RefundRepository refundRepository;

    public AdminAnalyticsOverviewResponse getOverview() {

        return new AdminAnalyticsOverviewResponse(
                userRepository.count(),
                userRepository.countByRole(Role.HOST),
                propertyRepository.count(),
                propertyRepository.countByStatus(PropertyStatus.ACTIVE),
                propertyRepository.countByApprovalStatus(
                        PropertyApprovalStatus.PENDING_REVIEW
                ),
                bookingRepository.count(),
                bookingRepository.countByStatus(
                        BookingStatus.CONFIRMED
                ),
                paymentRepository.count(),
                paymentRepository.countByStatus(
                        PaymentStatus.SUCCESS
                ),
                moderationReportRepository.countByStatus(
                        ReportStatus.OPEN
                ),
                hostVerificationRepository.countByStatus(
                        HostVerificationStatus.PENDING
                ),
                refundRepository.countByStatus(
                        RefundStatus.REQUESTED
                ),
                paymentRepository.sumSuccessfulPayments()
        );
    }

    public List<AnalyticsDataPointResponse> getUserGrowth() {

        return monthlyUserCounts(null);
    }

    public List<AnalyticsDataPointResponse> getHostGrowth() {

        return monthlyUserCounts(Role.HOST);
    }

    public List<AnalyticsDataPointResponse> getMonthlyBookings() {

        LocalDateTime start =
                startOfCurrentMonth().minusMonths(11);

        return bookingRepository
                .countBookingsByMonth(start)
                .stream()
                .map(row ->
                        new AnalyticsDataPointResponse(
                                String.valueOf(row[0]),
                                BigDecimal.valueOf(
                                        ((Number) row[1]).longValue()
                                )
                        )
                )
                .toList();
    }

    public List<AnalyticsDataPointResponse> getMonthlyRevenue() {

        LocalDateTime start =
                startOfCurrentMonth().minusMonths(11);

        return paymentRepository
                .sumRevenueByMonth(start)
                .stream()
                .map(row ->
                        new AnalyticsDataPointResponse(
                                String.valueOf(row[0]),
                                (BigDecimal) row[1]
                        )
                )
                .toList();
    }

    public List<AnalyticsDataPointResponse> getBookingStatuses() {

        List<AnalyticsDataPointResponse> points =
                new ArrayList<>();

        for (BookingStatus status : BookingStatus.values()) {
            points.add(
                    new AnalyticsDataPointResponse(
                            status.name(),
                            BigDecimal.valueOf(
                                    bookingRepository.countByStatus(status)
                            )
                    )
            );
        }

        return points;
    }

    public List<AnalyticsDataPointResponse> getPaymentStatuses() {

        List<AnalyticsDataPointResponse> points =
                new ArrayList<>();

        for (PaymentStatus status : PaymentStatus.values()) {
            points.add(
                    new AnalyticsDataPointResponse(
                            status.name(),
                            BigDecimal.valueOf(
                                    paymentRepository.countByStatus(status)
                            )
                    )
            );
        }

        return points;
    }

    public List<AnalyticsDataPointResponse> getPropertyStatuses() {

        List<AnalyticsDataPointResponse> points =
                new ArrayList<>();

        for (PropertyStatus status : PropertyStatus.values()) {
            points.add(
                    new AnalyticsDataPointResponse(
                            status.name(),
                            BigDecimal.valueOf(
                                    propertyRepository.countByStatus(status)
                            )
                    )
            );
        }

        return points;
    }

    public List<AnalyticsDataPointResponse> getPropertyApprovals() {

        List<AnalyticsDataPointResponse> points =
                new ArrayList<>();

        for (PropertyApprovalStatus status
                : PropertyApprovalStatus.values()) {

            points.add(
                    new AnalyticsDataPointResponse(
                            status.name(),
                            BigDecimal.valueOf(
                                    propertyRepository
                                            .countByApprovalStatus(status)
                            )
                    )
            );
        }

        return points;
    }

    private List<AnalyticsDataPointResponse> monthlyUserCounts(
            Role role
    ) {

        LocalDateTime start =
                startOfCurrentMonth().minusMonths(11);

        return userRepository
                .countUsersByMonth(start, role)
                .stream()
                .map(row ->
                        new AnalyticsDataPointResponse(
                                String.valueOf(row[0]),
                                BigDecimal.valueOf(
                                        ((Number) row[1]).longValue()
                                )
                        )
                )
                .toList();
    }

    private LocalDateTime startOfCurrentMonth() {

        return LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }
}
