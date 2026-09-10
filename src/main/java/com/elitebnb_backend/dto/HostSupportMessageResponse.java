package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class HostSupportMessageResponse {

    private Long id;

    private Long conversationId;

    private Long senderId;
    private String senderName;
    private Role senderRole;

    private String body;

    private boolean read;
    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
