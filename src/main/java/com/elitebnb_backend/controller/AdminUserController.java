package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AdminUserResponse;
import com.elitebnb_backend.dto.UpdateAccountStatusRequest;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.service.AdminUserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // =========================
    // ALL USERS / SEARCH / FILTER
    // =========================

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getUsers(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            Role role,

            @RequestParam(required = false)
            AccountStatus status
    ) {

        return ResponseEntity.ok(
                adminUserService.getUsers(
                        search,
                        role,
                        status
                )
        );
    }

    // =========================
    // ONE USER
    // =========================

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserResponse> getUser(
            @PathVariable Long userId
    ) {

        return ResponseEntity.ok(
                adminUserService.getUser(userId)
        );
    }

    // =========================
    // SUSPEND / REACTIVATE
    // =========================

    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserResponse> updateStatus(
            @PathVariable Long userId,
            @RequestBody UpdateAccountStatusRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                adminUserService.updateStatus(
                        userId,
                        request.getStatus(),
                        authentication.getName()
                )
        );
    }
}