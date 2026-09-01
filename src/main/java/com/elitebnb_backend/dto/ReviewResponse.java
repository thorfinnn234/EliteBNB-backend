package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewResponse {

    private Long id;

    private Long bookingId;

    private Long propertyId;
    private String propertyTitle;

    private Long guestId;
    private String guestName;
    private String guestProfileImageUrl;

    private Integer rating;
    private String comment;

    private String hostResponse;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}