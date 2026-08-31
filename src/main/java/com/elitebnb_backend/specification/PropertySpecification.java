package com.elitebnb_backend.specification;

import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;

import org.springframework.data.jpa.domain.Specification;

public class PropertySpecification {

    public static Specification<Property> hasLocation(
            String location
    ) {
        return (root, query, criteriaBuilder) -> {

            if (location == null || location.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.like(
                    criteriaBuilder.lower(
                            root.get("location")
                    ),
                    "%" + location.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Property> hasPropertyType(
            PropertyType propertyType
    ) {
        return (root, query, criteriaBuilder) -> {

            if (propertyType == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("propertyType"),
                    propertyType
            );
        };
    }

    public static Specification<Property> hasMinimumPrice(
            Double minPrice
    ) {
        return (root, query, criteriaBuilder) -> {

            if (minPrice == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("pricePerNight"),
                    minPrice
            );
        };
    }

    public static Specification<Property> hasMaximumPrice(
            Double maxPrice
    ) {
        return (root, query, criteriaBuilder) -> {

            if (maxPrice == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.lessThanOrEqualTo(
                    root.get("pricePerNight"),
                    maxPrice
            );
        };
    }

    public static Specification<Property> hasMinimumBedrooms(
            Integer bedrooms
    ) {
        return (root, query, criteriaBuilder) -> {

            if (bedrooms == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("bedrooms"),
                    bedrooms
            );
        };
    }

    public static Specification<Property> supportsGuests(
            Integer guests
    ) {
        return (root, query, criteriaBuilder) -> {

            if (guests == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("maxGuests"),
                    guests
            );
        };
    }

    public static Specification<Property> isActive() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        PropertyStatus.ACTIVE
                );
    }
}