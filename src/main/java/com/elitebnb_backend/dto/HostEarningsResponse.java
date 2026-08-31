package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostEarningsResponse {

    private Double totalEarnings;
    private Double confirmedRevenue;
    private Double completedRevenue;
    private Double pendingRevenue;

    private Long totalReservations;
    private Long completedReservations;
}