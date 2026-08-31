package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostDashboardResponse {

    private long totalListings;
    private long activeListings;

    private long totalReservations;
    private long pendingReservations;
    private long confirmedReservations;
    private long completedReservations;
    private long cancelledReservations;

    private Double totalEarnings;
}