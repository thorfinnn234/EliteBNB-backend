package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.AdminNotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminNotificationResponse {

    private Long id;
    private String title;
    private String message;
    private AdminNotificationType type;
    private boolean read;
    private Long targetId;
    private String targetType;
    private LocalDateTime createdAt;
}
