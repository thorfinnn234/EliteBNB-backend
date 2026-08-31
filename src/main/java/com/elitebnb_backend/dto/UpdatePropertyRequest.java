package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import lombok.Getter;
import lombok.Setter;
import com.elitebnb_backend.entity.Amenity;
import java.util.Set;

@Getter
@Setter
public class UpdatePropertyRequest {

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
}