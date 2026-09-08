package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AdminAnalyticsOverviewResponse {

    private long totalUsers;
    private long totalHosts;
    private long totalProperties;
    private long activeProperties;
    private long pendingPropertyApprovals;
    private long totalBookings;
    private long confirmedBookings;
    private long totalPayments;
    private long successfulPayments;
    private long openReports;
    private long pendingHostVerifications;
    private long requestedRefunds;
    private BigDecimal totalRevenue;
}
