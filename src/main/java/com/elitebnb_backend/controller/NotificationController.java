package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.NotificationResponse;
import com.elitebnb_backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>>
    getMyNotifications(Authentication authentication) {

        return ResponseEntity.ok(
                notificationService.getMyNotifications(authentication)
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                java.util.Map.of(
                        "count",
                        notificationService.getUnreadCount(authentication)
                )
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                notificationService.markAsRead(
                        notificationId,
                        authentication
                )
        );
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            Authentication authentication
    ) {
        notificationService.markAllAsRead(authentication);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        notificationService.deleteNotification(
                notificationId,
                authentication
        );

        return ResponseEntity.noContent().build();
    }
}