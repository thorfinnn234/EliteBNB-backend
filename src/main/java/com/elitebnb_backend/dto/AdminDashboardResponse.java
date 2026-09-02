package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AdminDashboardResponse {

    private long totalUsers;
    private long totalHosts;
    private long totalGuests;

    private long totalProperties;
    private long activeProperties;

    private long totalBookings;
    private long pendingBookings;
    private long confirmedBookings;
    private long completedBookings;
    private long cancelledBookings;

    private long totalPayments;
    private long successfulPayments;
    private long pendingPayments;
    private long failedPayments;

    private BigDecimal totalRevenue;
}