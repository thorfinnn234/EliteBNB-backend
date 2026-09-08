package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AdminNotificationResponse;
import com.elitebnb_backend.entity.AdminNotificationType;
import com.elitebnb_backend.service.AdminNotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping
    public ResponseEntity<List<AdminNotificationResponse>> getNotifications(
            @RequestParam(required = false)
            Boolean read,

            @RequestParam(required = false)
            AdminNotificationType type,

            Authentication authentication
    ) {

        return ResponseEntity.ok(
                adminNotificationService.getNotifications(
                        authentication.getName(),
                        read,
                        type
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                adminNotificationService.getUnreadCount(
                        authentication.getName()
                )
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<AdminNotificationResponse> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                adminNotificationService.markAsRead(
                        notificationId,
                        authentication.getName()
                )
        );
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            Authentication authentication
    ) {

        adminNotificationService.markAllAsRead(
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
