package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AdminPropertyResponse;
import com.elitebnb_backend.dto.UpdatePropertyApprovalRequest;
import com.elitebnb_backend.entity.AuditActionType;
import com.elitebnb_backend.entity.AuditTargetType;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyImage;
import com.elitebnb_backend.entity.PropertyApprovalStatus;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPropertyService {

    private final PropertyRepository propertyRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    // =========================
    // GET ALL / SEARCH / FILTER
    // =========================

    public List<AdminPropertyResponse> getProperties(
            String search,
            PropertyStatus status,
            PropertyApprovalStatus approvalStatus,
            PropertyType propertyType
    ) {

        return propertyRepository
                .searchAdminProperties(
                        search,
                        status,
                        approvalStatus,
                        propertyType
                )
                .stream()
                .map(this::map)
                .toList();
    }

    // =========================
    // GET ONE PROPERTY
    // =========================

    public AdminPropertyResponse getProperty(
            Long propertyId
    ) {

        Property property =
                getPropertyEntity(propertyId);

        return map(property);
    }

    // =========================
    // UPDATE STATUS
    // =========================

    public AdminPropertyResponse updateStatus(
            Long propertyId,
            PropertyStatus status,
            String adminEmail
    ) {

        if (status == null) {
            throw new RuntimeException(
                    "Property status is required"
            );
        }

        Property property =
                getPropertyEntity(propertyId);

        property.setStatus(status);

        Property saved =
                propertyRepository.save(property);

        auditLogService.record(
                adminEmail,
                AuditActionType.PROPERTY_STATUS_UPDATED,
                AuditTargetType.PROPERTY,
                saved.getId(),
                "Updated property "
                        + saved.getTitle()
                        + " status to "
                        + status.name()
        );

        return map(saved);
    }

    public List<AdminPropertyResponse> getPendingApprovals() {

        return propertyRepository
                .findByApprovalStatusOrderByCreatedAtDesc(
                        PropertyApprovalStatus.PENDING_REVIEW
                )
                .stream()
                .map(this::map)
                .toList();
    }

    public AdminPropertyResponse updateApproval(
            Long propertyId,
            UpdatePropertyApprovalRequest request,
            String adminEmail
    ) {

        if (request.getApprovalStatus() == null) {
            throw new RuntimeException(
                    "Property approval status is required"
            );
        }

        Property property =
                getPropertyEntity(propertyId);

        User admin =
                userRepository
                        .findByEmail(adminEmail)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Admin account not found"
                                )
                        );

        property.setApprovalStatus(
                request.getApprovalStatus()
        );

        property.setApprovalNote(
                normalizeNote(request.getApprovalNote())
        );

        property.setApprovalReviewedBy(admin);
        property.setApprovalReviewedAt(
                java.time.LocalDateTime.now()
        );

        Property saved =
                propertyRepository.save(property);

        auditLogService.record(
                adminEmail,
                AuditActionType.PROPERTY_APPROVAL_UPDATED,
                AuditTargetType.PROPERTY,
                saved.getId(),
                "Updated property "
                        + saved.getTitle()
                        + " approval status to "
                        + saved.getApprovalStatus().name()
        );

        return map(saved);
    }

    // =========================
    // HELPERS
    // =========================

    private Property getPropertyEntity(
            Long propertyId
    ) {

        return propertyRepository
                .findById(propertyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Property not found"
                        )
                );
    }

    private AdminPropertyResponse map(
            Property property
    ) {

        String hostName =
                property.getHost().getFirstName()
                        + " "
                        + property.getHost().getLastName();

        String coverImageUrl =
                property.getImages()
                        .stream()
                        .filter(PropertyImage::isCover)
                        .findFirst()
                        .map(PropertyImage::getImageUrl)
                        .orElseGet(() ->
                                property.getImages()
                                        .stream()
                                        .findFirst()
                                        .map(PropertyImage::getImageUrl)
                                        .orElse(null)
                        );

        User approvalReviewedBy =
                property.getApprovalReviewedBy();

        String approvalReviewedByName =
                approvalReviewedBy != null
                        ? approvalReviewedBy.getFirstName()
                        + " "
                        + approvalReviewedBy.getLastName()
                        : null;

        return new AdminPropertyResponse(
                property.getId(),
                property.getTitle(),
                property.getDescription(),
                property.getLocation(),
                property.getPricePerNight(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getMaxGuests(),
                property.getPropertyType(),
                property.getStatus(),
                effectiveApprovalStatus(property),
                property.getApprovalNote(),

                property.getHost().getId(),
                hostName,
                property.getHost().getEmail(),

                approvalReviewedBy != null
                        ? approvalReviewedBy.getId()
                        : null,
                approvalReviewedByName,
                property.getApprovalReviewedAt(),

                coverImageUrl,

                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }

    private String normalizeNote(
            String note
    ) {

        if (note == null
                || note.trim().isEmpty()) {
            return null;
        }

        return note.trim();
    }

    private PropertyApprovalStatus effectiveApprovalStatus(
            Property property
    ) {

        return property.getApprovalStatus() != null
                ? property.getApprovalStatus()
                : PropertyApprovalStatus.APPROVED;
    }
}
