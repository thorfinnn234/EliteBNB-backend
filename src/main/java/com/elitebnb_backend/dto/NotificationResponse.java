package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;

    private Long bookingId;
    private Long propertyId;

    private LocalDateTime createdAt;
}