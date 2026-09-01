package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FavoriteResponse {

    private Long id;

    private Long propertyId;
    private String propertyTitle;
    private String location;
    private Double pricePerNight;

    private String propertyType;

    private Integer bedrooms;
    private Integer bathrooms;
    private Integer maxGuests;

    private String coverImageUrl;

    private LocalDateTime savedAt;
}