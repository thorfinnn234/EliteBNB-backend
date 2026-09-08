package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyApprovalStatus;
import com.elitebnb_backend.entity.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminPropertyResponse {

    private Long id;

    private String title;
    private String description;
    private String location;

    private Double pricePerNight;

    private Integer bedrooms;
    private Integer bathrooms;
    private Integer maxGuests;

    private PropertyType propertyType;
    private PropertyStatus status;
    private PropertyApprovalStatus approvalStatus;
    private String approvalNote;

    private Long hostId;
    private String hostName;
    private String hostEmail;

    private Long approvalReviewedById;
    private String approvalReviewedByName;
    private LocalDateTime approvalReviewedAt;

    private String coverImageUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
