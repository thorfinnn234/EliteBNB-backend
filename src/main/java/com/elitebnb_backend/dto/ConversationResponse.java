package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ConversationResponse {

    private Long id;

    private Long propertyId;
    private String propertyTitle;
    private String propertyImageUrl;

    private Long bookingId;

    private Long guestId;
    private String guestName;
    private String guestProfileImageUrl;

    private Long hostId;
    private String hostName;
    private String hostProfileImageUrl;

    private String otherParticipantName;
    private String otherParticipantProfileImageUrl;

    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
