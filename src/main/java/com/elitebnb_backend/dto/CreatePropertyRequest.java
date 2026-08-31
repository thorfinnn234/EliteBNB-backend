package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.PropertyType;
import lombok.Getter;
import lombok.Setter;
import com.elitebnb_backend.entity.Amenity;
import java.util.Set;

@Getter
@Setter
public class CreatePropertyRequest {

    private String title;
    private String description;
    private String location;
    private Double pricePerNight;
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer maxGuests;
    private PropertyType propertyType;
    private Set<Amenity> amenities;
}