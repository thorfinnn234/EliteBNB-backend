package com.elitebnb_backend.service;

import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyApprovalStatus;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PropertyVisibilityServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    private PropertyVisibilityService propertyVisibilityService;

    /**
     * Builds the visibility service with a mocked repository. These tests focus
     * on the business rule itself without booting a database-backed context.
     */
    @BeforeEach
    void setUp() {
        propertyVisibilityService =
                new PropertyVisibilityService(
                        propertyRepository
                );
    }

    /**
     * ACTIVE plus APPROVED is the normal public state for current properties.
     */
    @Test
    void activeApprovedPropertyIsPubliclyAccessible() {
        assertThat(propertyVisibilityService.isPubliclyAccessible(
                property(
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.APPROVED
                )
        )).isTrue();
    }

    /**
     * ACTIVE plus PENDING_REVIEW means operationally enabled but not published.
     */
    @Test
    void activePendingReviewPropertyIsNotPubliclyAccessible() {
        assertThat(propertyVisibilityService.isPubliclyAccessible(
                property(
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.PENDING_REVIEW
                )
        )).isFalse();
    }

    /**
     * ACTIVE plus REJECTED stays hidden even though the Host has not disabled
     * the listing operationally.
     */
    @Test
    void activeRejectedPropertyIsNotPubliclyAccessible() {
        assertThat(propertyVisibilityService.isPubliclyAccessible(
                property(
                        PropertyStatus.ACTIVE,
                        PropertyApprovalStatus.REJECTED
                )
        )).isFalse();
    }

    /**
     * INACTIVE plus APPROVED is approved content that the platform should not
     * expose or book while it is operationally disabled.
     */
    @Test
    void inactiveApprovedPropertyIsNotPubliclyAccessible() {
        assertThat(propertyVisibilityService.isPubliclyAccessible(
                property(
                        PropertyStatus.INACTIVE,
                        PropertyApprovalStatus.APPROVED
                )
        )).isFalse();
    }

    /**
     * SUSPENDED plus APPROVED remains hidden because operational suspension
     * wins over previous approval.
     */
    @Test
    void suspendedApprovedPropertyIsNotPubliclyAccessible() {
        assertThat(propertyVisibilityService.isPubliclyAccessible(
                property(
                        PropertyStatus.SUSPENDED,
                        PropertyApprovalStatus.APPROVED
                )
        )).isFalse();
    }

    /**
     * Legacy rows with null approvalStatus keep the previous public behavior:
     * they are treated like approved records when status is ACTIVE.
     */
    @Test
    void activeLegacyNullApprovalPropertyIsPubliclyAccessible() {
        assertThat(propertyVisibilityService.isPubliclyAccessible(
                property(
                        PropertyStatus.ACTIVE,
                        null
                )
        )).isTrue();
    }

    /**
     * Creates the minimal property state required for visibility decisions.
     */
    private Property property(
            PropertyStatus status,
            PropertyApprovalStatus approvalStatus
    ) {

        return Property.builder()
                .status(status)
                .approvalStatus(approvalStatus)
                .build();
    }
}
