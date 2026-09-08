package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AdminDashboardResponse;
import com.elitebnb_backend.dto.AdminUserResponse;
import com.elitebnb_backend.service.AdminDashboardService;
import com.elitebnb_backend.service.AdminUserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminUserService adminUserService;

    @GetMapping("/me")
    public ResponseEntity<AdminUserResponse> getCurrentAdmin(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                adminUserService.getCurrentAdmin(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {

        return ResponseEntity.ok(
                adminDashboardService.getDashboard()
        );
    }
}
