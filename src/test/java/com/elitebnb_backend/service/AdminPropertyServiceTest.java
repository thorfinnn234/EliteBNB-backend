package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AdminPropertyResponse;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyApprovalStatus;
import com.elitebnb_backend.entity.PropertyImage;
import com.elitebnb_backend.entity.PropertyImageType;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserRepository userRepository;

    private AdminPropertyService adminPropertyService;

    @BeforeEach
    void setUp() {
        adminPropertyService =
                new AdminPropertyService(
                        propertyRepository,
                        auditLogService,
                        userRepository
                );
    }

    @Test
    void adminPropertyResponseUsesExistingCoverImageFlag() {
        Property property =
                propertyWithImages(
                        List.of(
                                propertyImage(
                                        "first-image.jpg",
                                        false
                                ),
                                propertyImage(
                                        "cover-image.jpg",
                                        true
                                )
                        )
                );

        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));

        AdminPropertyResponse response =
                adminPropertyService.getProperty(
                        property.getId()
                );

        assertThat(response.getCoverImageUrl())
                .isEqualTo("cover-image.jpg");
    }

    private Property propertyWithImages(
            List<PropertyImage> images
    ) {
        User host =
                User.builder()
                        .id(7L)
                        .firstName("Hakeem")
                        .lastName("Host")
                        .email("host@example.com")
                        .password("hashed-password")
                        .role(Role.HOST)
                        .accountStatus(AccountStatus.ACTIVE)
                        .emailVerified(true)
                        .build();

        return Property.builder()
                .id(100L)
                .title("Lagos Loft")
                .description("A bright city stay")
                .location("Lagos")
                .pricePerNight(120.0)
                .bedrooms(2)
                .bathrooms(2)
                .maxGuests(4)
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .approvalStatus(PropertyApprovalStatus.APPROVED)
                .host(host)
                .images(images)
                .build();
    }

    private PropertyImage propertyImage(
            String imageUrl,
            boolean coverImage
    ) {
        return PropertyImage.builder()
                .imageUrl(imageUrl)
                .coverImage(coverImage)
                .imageType(PropertyImageType.EXTERIOR)
                .build();
    }
}
