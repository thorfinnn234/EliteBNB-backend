package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.HostDashboardResponse;
import com.elitebnb_backend.service.HostDashboardService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/host")
public class HostDashboardController {

    private final HostDashboardService hostDashboardService;

    public HostDashboardController(
            HostDashboardService hostDashboardService
    ) {
        this.hostDashboardService =
                hostDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<HostDashboardResponse> getDashboard(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostDashboardService.getDashboard(
                        authentication
                )
        );
    }
}