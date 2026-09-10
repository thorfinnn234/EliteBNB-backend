package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.HostVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class HostSupportConversationResponse {

    private Long id;

    private Long hostId;
    private String hostName;
    private String hostEmail;
    private String hostProfileImageUrl;
    private HostVerificationStatus verificationStatus;

    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
