package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyApprovalStatus;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.specification.PropertySpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PropertyPublicVisibilityRepositoryTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    private Property activeApproved;
    private Property activeLegacyNull;
    private Property activePendingReview;
    private Property activeRejected;
    private Property inactiveApproved;
    private Property suspendedApproved;

    /**
     * Seeds representative public and hidden property states in H2 so the
     * repository-level public list query and search specification can be
     * compared without relying on developer data.
     */
    @BeforeEach
    void setUp() {
        User host =
                userRepository.save(
                        host()
                );

        activeApproved =
                propertyRepository.save(
                        property(
                                "Approved Lagos Loft",
                                "Lagos",
                                PropertyStatus.ACTIVE,
                                PropertyApprovalStatus.APPROVED,
                                host
                        )
                );
        activeLegacyNull =
                propertyRepository.save(
                        property(
                                "Legacy Lagos Loft",
                                "Lagos",
                                PropertyStatus.ACTIVE,
                                null,
                                host
                        )
                );
        activePendingReview =
                propertyRepository.save(
                        property(
                                "Pending Lagos Loft",
                                "Lagos",
                                PropertyStatus.ACTIVE,
                                PropertyApprovalStatus.PENDING_REVIEW,
                                host
                        )
                );
        activeRejected =
                propertyRepository.save(
                        property(
                                "Rejected Lagos Loft",
                                "Lagos",
                                PropertyStatus.ACTIVE,
                                PropertyApprovalStatus.REJECTED,
                                host
                        )
                );
        inactiveApproved =
                propertyRepository.save(
                        property(
                                "Inactive Lagos Loft",
                                "Lagos",
                                PropertyStatus.INACTIVE,
                                PropertyApprovalStatus.APPROVED,
                                host
                        )
                );
        suspendedApproved =
                propertyRepository.save(
                        property(
                                "Suspended Lagos Loft",
                                "Lagos",
                                PropertyStatus.SUSPENDED,
                                PropertyApprovalStatus.APPROVED,
                                host
                        )
                );

        propertyRepository.flush();
    }

    /**
     * Documents the current public list behavior: ACTIVE plus APPROVED is
     * visible, and legacy ACTIVE plus null approvalStatus remains visible.
     */
    @Test
    void publicListQueryReturnsOnlyVisibleProperties() {
        List<Long> propertyIds =
                propertyRepository
                        .findPublicVisibleProperties()
                        .stream()
                        .map(Property::getId)
                        .toList();

        assertThat(propertyIds)
                .contains(activeApproved.getId(), activeLegacyNull.getId())
                .doesNotContain(
                        activePendingReview.getId(),
                        activeRejected.getId(),
                        inactiveApproved.getId(),
                        suspendedApproved.getId()
                );
    }

    /**
     * Documents that public search uses equivalent visibility semantics before
     * applying the normal Explore filters.
     */
    @Test
    void publicSearchSpecificationUsesEquivalentVisibilitySemantics() {
        Specification<Property> spec =
                PropertySpecification
                        .isPubliclyAccessible()
                        .and(PropertySpecification.hasLocation("Lagos"));

        List<Long> propertyIds =
                propertyRepository
                        .findAll(spec)
                        .stream()
                        .map(Property::getId)
                        .toList();

        assertThat(propertyIds)
                .contains(activeApproved.getId(), activeLegacyNull.getId())
                .doesNotContain(
                        activePendingReview.getId(),
                        activeRejected.getId(),
                        inactiveApproved.getId(),
                        suspendedApproved.getId()
                );
    }

    /**
     * Builds a Host user because every property requires a real owner.
     */
    private User host() {

        return User.builder()
                .firstName("Hakeem")
                .lastName("Host")
                .email("host@example.com")
                .password("encoded-password")
                .role(Role.HOST)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    /**
     * Builds a property with the supplied moderation and operational state.
     */
    private Property property(
            String title,
            String location,
            PropertyStatus status,
            PropertyApprovalStatus approvalStatus,
            User host
    ) {

        return Property.builder()
                .title(title)
                .description("A public visibility test listing")
                .location(location)
                .pricePerNight(100.0)
                .bedrooms(1)
                .bathrooms(1)
                .maxGuests(2)
                .propertyType(PropertyType.APARTMENT)
                .status(status)
                .approvalStatus(approvalStatus)
                .host(host)
                .build();
    }
}
