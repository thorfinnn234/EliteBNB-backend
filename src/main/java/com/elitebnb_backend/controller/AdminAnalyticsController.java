package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AdminAnalyticsOverviewResponse;
import com.elitebnb_backend.dto.AnalyticsDataPointResponse;
import com.elitebnb_backend.service.AdminAnalyticsService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AdminAnalyticsOverviewResponse> getOverview() {

        return ResponseEntity.ok(
                adminAnalyticsService.getOverview()
        );
    }

    @GetMapping("/users/growth")
    public ResponseEntity<List<AnalyticsDataPointResponse>> getUserGrowth() {

        return ResponseEntity.ok(
                adminAnalyticsService.getUserGrowth()
        );
    }

    @GetMapping("/hosts/growth")
    public ResponseEntity<List<AnalyticsDataPointResponse>> getHostGrowth() {

        return ResponseEntity.ok(
                adminAnalyticsService.getHostGrowth()
        );
    }

    @GetMapping("/bookings/monthly")
    public ResponseEntity<List<AnalyticsDataPointResponse>>
    getMonthlyBookings() {

        return ResponseEntity.ok(
                adminAnalyticsService.getMonthlyBookings()
        );
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<List<AnalyticsDataPointResponse>>
    getMonthlyRevenue() {

        return ResponseEntity.ok(
                adminAnalyticsService.getMonthlyRevenue()
        );
    }

    @GetMapping("/bookings/statuses")
    public ResponseEntity<List<AnalyticsDataPointResponse>>
    getBookingStatuses() {

        return ResponseEntity.ok(
                adminAnalyticsService.getBookingStatuses()
        );
    }

    @GetMapping("/payments/statuses")
    public ResponseEntity<List<AnalyticsDataPointResponse>>
    getPaymentStatuses() {

        return ResponseEntity.ok(
                adminAnalyticsService.getPaymentStatuses()
        );
    }

    @GetMapping("/properties/statuses")
    public ResponseEntity<List<AnalyticsDataPointResponse>>
    getPropertyStatuses() {

        return ResponseEntity.ok(
                adminAnalyticsService.getPropertyStatuses()
        );
    }

    @GetMapping("/properties/approvals")
    public ResponseEntity<List<AnalyticsDataPointResponse>>
    getPropertyApprovals() {

        return ResponseEntity.ok(
                adminAnalyticsService.getPropertyApprovals()
        );
    }
}
