package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AddPropertyImageRequest;
import com.elitebnb_backend.dto.CreatePropertyRequest;
import com.elitebnb_backend.dto.PropertyResponse;
import com.elitebnb_backend.dto.UpdatePropertyRequest;

import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyImage;
import com.elitebnb_backend.entity.PropertyImageType;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.User;

import com.elitebnb_backend.repository.PropertyImageRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;

import com.elitebnb_backend.specification.PropertySpecification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final CloudinaryService cloudinaryService;

    public PropertyService(
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            PropertyImageRepository propertyImageRepository,
            CloudinaryService cloudinaryService
    ) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.cloudinaryService = cloudinaryService;
    }

    // CREATE PROPERTY
    public PropertyResponse createProperty(
            CreatePropertyRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User host = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Host not found")
                );

        Property property = Property.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .pricePerNight(request.getPricePerNight())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .maxGuests(request.getMaxGuests())
                .propertyType(request.getPropertyType())
                .amenities(
                        request.getAmenities() != null
                                ? request.getAmenities()
                                : new HashSet<>()
                )
                .status(PropertyStatus.ACTIVE)
                .host(host)
                .build();

        Property savedProperty =
                propertyRepository.save(property);

        return mapToResponse(savedProperty);
    }

    // GET ALL PROPERTIES
    public List<PropertyResponse> getAllProperties() {

        return propertyRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // SEARCH / FILTER PROPERTIES
    public List<PropertyResponse> searchProperties(
            String location,
            PropertyType propertyType,
            Double minPrice,
            Double maxPrice,
            Integer bedrooms,
            Integer guests
    ) {

        Specification<Property> spec =
                Specification
                        .where(PropertySpecification.isActive())
                        .and(PropertySpecification.hasLocation(location))
                        .and(PropertySpecification.hasPropertyType(propertyType))
                        .and(PropertySpecification.hasMinimumPrice(minPrice))
                        .and(PropertySpecification.hasMaximumPrice(maxPrice))
                        .and(PropertySpecification.hasMinimumBedrooms(bedrooms))
                        .and(PropertySpecification.supportsGuests(guests));

        return propertyRepository
                .findAll(spec)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET PROPERTY BY ID
    public PropertyResponse getPropertyById(Long id) {

        Property property = propertyRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Property not found"
                        )
                );

        return mapToResponse(property);
    }

    // GET HOST'S OWN PROPERTIES
    public List<PropertyResponse> getMyProperties(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User host = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Host not found")
                );

        return propertyRepository.findByHost(host)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // UPDATE PROPERTY
    public PropertyResponse updateProperty(
            Long id,
            UpdatePropertyRequest request,
            Authentication authentication
    ) {

        Property property = propertyRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Property not found"
                        )
                );

        String email = authentication.getName();

        if (!property.getHost()
                .getEmail()
                .equals(email)) {

            throw new RuntimeException(
                    "You are not allowed to update this property"
            );
        }

        if (request.getTitle() != null) {
            property.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            property.setDescription(
                    request.getDescription()
            );
        }

        if (request.getLocation() != null) {
            property.setLocation(
                    request.getLocation()
            );
        }

        if (request.getPricePerNight() != null) {
            property.setPricePerNight(
                    request.getPricePerNight()
            );
        }

        if (request.getBedrooms() != null) {
            property.setBedrooms(
                    request.getBedrooms()
            );
        }

        if (request.getBathrooms() != null) {
            property.setBathrooms(
                    request.getBathrooms()
            );
        }

        if (request.getMaxGuests() != null) {
            property.setMaxGuests(
                    request.getMaxGuests()
            );
        }

        if (request.getPropertyType() != null) {
            property.setPropertyType(
                    request.getPropertyType()
            );
        }

        if (request.getStatus() != null) {
            property.setStatus(
                    request.getStatus()
            );
        }

        if (request.getAmenities() != null) {
            property.setAmenities(
                    request.getAmenities()
            );
        }

        Property updatedProperty =
                propertyRepository.save(property);

        return mapToResponse(updatedProperty);
    }

    // DELETE PROPERTY
    public void deleteProperty(
            Long id,
            Authentication authentication
    ) {

        Property property = propertyRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Property not found"
                        )
                );

        String email = authentication.getName();

        if (!property.getHost()
                .getEmail()
                .equals(email)) {

            throw new RuntimeException(
                    "You are not allowed to delete this property"
            );
        }

        propertyRepository.delete(property);
    }

    // ADD PROPERTY IMAGE USING URL
    public void addPropertyImage(
            Long propertyId,
            AddPropertyImageRequest request,
            Authentication authentication
    ) {

        Property property = propertyRepository
                .findById(propertyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Property not found"
                        )
                );

        String email = authentication.getName();

        if (!property.getHost()
                .getEmail()
                .equals(email)) {

            throw new RuntimeException(
                    "You are not allowed to add images to this property"
            );
        }

        if (request.isCoverImage()) {

            List<PropertyImage> existingImages =
                    propertyImageRepository
                            .findByPropertyId(propertyId);

            existingImages.forEach(image ->
                    image.setCoverImage(false)
            );

            propertyImageRepository.saveAll(
                    existingImages
            );
        }

        PropertyImage image =
                PropertyImage.builder()
                        .imageUrl(request.getImageUrl())
                        .coverImage(request.isCoverImage())
                        .imageType(request.getImageType())
                        .property(property)
                        .build();

        propertyImageRepository.save(image);
    }

    // UPLOAD PROPERTY IMAGE TO CLOUDINARY
    public PropertyImage uploadPropertyImage(
            Long propertyId,
            MultipartFile file,
            PropertyImageType imageType,
            boolean coverImage,
            Authentication authentication
    ) {

        Property property = propertyRepository
                .findById(propertyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Property not found"
                        )
                );

        String email = authentication.getName();

        if (!property.getHost()
                .getEmail()
                .equals(email)) {

            throw new RuntimeException(
                    "You are not allowed to upload images to this property"
            );
        }

        if (file == null || file.isEmpty()) {
            throw new RuntimeException(
                    "Image file is required"
            );
        }

        String imageUrl =
                cloudinaryService.uploadImage(file);

        if (coverImage) {

            List<PropertyImage> existingImages =
                    propertyImageRepository
                            .findByPropertyId(propertyId);

            existingImages.forEach(image ->
                    image.setCoverImage(false)
            );

            propertyImageRepository.saveAll(
                    existingImages
            );
        }

        PropertyImage image =
                PropertyImage.builder()
                        .imageUrl(imageUrl)
                        .coverImage(coverImage)
                        .imageType(imageType)
                        .property(property)
                        .build();

        return propertyImageRepository.save(image);
    }

    // GET PROPERTY IMAGES
    public List<PropertyImage> getPropertyImages(
            Long propertyId
    ) {

        if (!propertyRepository.existsById(propertyId)) {
            throw new RuntimeException(
                    "Property not found"
            );
        }

        return propertyImageRepository
                .findByPropertyId(propertyId);
    }


    // DELETE PROPERTY IMAGE
    public void deletePropertyImage(
            Long propertyId,
            Long imageId,
            Authentication authentication
    ) {

        Property property = propertyRepository
                .findById(propertyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Property not found"
                        )
                );

        String email = authentication.getName();

        if (!property.getHost()
                .getEmail()
                .equals(email)) {

            throw new RuntimeException(
                    "You are not allowed to manage images for this property"
            );
        }

        PropertyImage image = propertyImageRepository
                .findById(imageId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Property image not found"
                        )
                );

        if (!image.getProperty()
                .getId()
                .equals(propertyId)) {

            throw new RuntimeException(
                    "This image does not belong to this property"
            );
        }

        long imageCount =
                propertyImageRepository
                        .countByPropertyId(propertyId);

        if (imageCount <= 1) {
            throw new RuntimeException(
                    "A property must have at least one image"
            );
        }

        boolean wasCoverImage =
                image.isCoverImage();

        propertyImageRepository.delete(image);

        // If the deleted image was the cover,
        // automatically make another image the cover.
        if (wasCoverImage) {

            List<PropertyImage> remainingImages =
                    propertyImageRepository
                            .findByPropertyId(propertyId);

            if (!remainingImages.isEmpty()) {

                PropertyImage newCover =
                        remainingImages.get(0);

                newCover.setCoverImage(true);

                propertyImageRepository.save(
                        newCover
                );
            }
        }
    }


    // SET PROPERTY COVER IMAGE
    public PropertyImage setCoverImage(
            Long propertyId,
            Long imageId,
            Authentication authentication
    ) {

        Property property = propertyRepository
                .findById(propertyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Property not found"
                        )
                );

        String email = authentication.getName();

        if (!property.getHost()
                .getEmail()
                .equals(email)) {

            throw new RuntimeException(
                    "You are not allowed to manage images for this property"
            );
        }

        PropertyImage selectedImage =
                propertyImageRepository
                        .findById(imageId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Property image not found"
                                )
                        );

        if (!selectedImage.getProperty()
                .getId()
                .equals(propertyId)) {

            throw new RuntimeException(
                    "This image does not belong to this property"
            );
        }

        List<PropertyImage> images =
                propertyImageRepository
                        .findByPropertyId(propertyId);

        images.forEach(image ->
                image.setCoverImage(
                        image.getId().equals(imageId)
                )
        );

        propertyImageRepository.saveAll(images);

        return selectedImage;
    }
    // MAP ENTITY TO RESPONSE
    private PropertyResponse mapToResponse(
            Property property
    ) {

        List<String> imageUrls =
                propertyImageRepository
                        .findByPropertyId(property.getId())
                        .stream()
                        .map(PropertyImage::getImageUrl)
                        .toList();

        return new PropertyResponse(
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
                property.getAmenities(),
                imageUrls,
                property.getHost().getId(),
                property.getHost().getFirstName()
                        + " "
                        + property.getHost().getLastName(),
                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }
}