package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.Amenity;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public class PropertyResponse {

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

    private Set<Amenity> amenities;

    private List<String> images;

    private Long hostId;

    private String hostName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}