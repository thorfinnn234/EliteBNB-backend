package com.elitebnb_backend.service;

import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyApprovalStatus;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropertyVisibilityService {

    private final PropertyRepository propertyRepository;

    /**
     * Loads a property by id and applies the same public visibility rule used
     * by listing and search endpoints. Public callers receive "Property not
     * found" for hidden records so moderation state is not exposed.
     */
    public Property requirePubliclyAccessible(
            Long propertyId
    ) {

        return requirePubliclyAccessible(
                propertyId,
                "Property not found"
        );
    }

    /**
     * Loads a property by id and applies public visibility with a caller-owned
     * error message. Booking uses this to explain that an existing property is
     * not currently bookable without exposing approval or suspension details.
     */
    public Property requirePubliclyAccessible(
            Long propertyId,
            String unavailableMessage
    ) {

        Property property =
                propertyRepository
                        .findById(propertyId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Property not found"
                                )
                        );

        requirePubliclyAccessible(
                property,
                unavailableMessage
        );

        return property;
    }

    /**
     * Applies the authoritative public visibility rule to an already loaded
     * property. This keeps raw-id paths such as bookings, favorites, images,
     * and availability aligned with public list/search/detail behavior.
     */
    public void requirePubliclyAccessible(
            Property property,
            String unavailableMessage
    ) {

        if (!isPubliclyAccessible(property)) {
            throw new RuntimeException(
                    unavailableMessage
            );
        }
    }

    /**
     * Answers whether a property can be exposed or booked through public USER
     * flows. Legacy rows with a null approvalStatus remain compatible because
     * existing public list/search behavior already treats them as approved.
     */
    public boolean isPubliclyAccessible(
            Property property
    ) {

        return property != null
                && property.getStatus() == PropertyStatus.ACTIVE
                && isApprovedOrLegacy(property);
    }

    /**
     * Preserves the existing legacy approval behavior: null approvalStatus is
     * displayed and filtered as APPROVED for older data that predates Admin
     * moderation.
     */
    public boolean isApprovedOrLegacy(
            Property property
    ) {

        return property != null
                && (
                        property.getApprovalStatus() == null
                                || property.getApprovalStatus()
                                == PropertyApprovalStatus.APPROVED
                );
    }

    /**
     * Converts the nullable legacy approval state into the frontend-facing
     * status already used by property response DTOs.
     */
    public PropertyApprovalStatus effectiveApprovalStatus(
            Property property
    ) {

        return property.getApprovalStatus() != null
                ? property.getApprovalStatus()
                : PropertyApprovalStatus.APPROVED;
    }
}
