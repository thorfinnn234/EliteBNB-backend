package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.BlockAvailabilityRequest;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyAvailability;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.PropertyAvailabilityRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AvailabilityService {

    private final PropertyAvailabilityRepository availabilityRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public AvailabilityService(
            PropertyAvailabilityRepository availabilityRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository
    ) {
        this.availabilityRepository = availabilityRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    public PropertyAvailability blockDates(
            Long propertyId,
            BlockAvailabilityRequest request,
            Authentication authentication
    ) {

        User host = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Host not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Only the owner can block dates
        if (!property.getHost().getId().equals(host.getId())) {
            throw new RuntimeException(
                    "You cannot manage availability for this property"
            );
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new RuntimeException("Start date and end date are required");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Start date cannot be in the past");
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }

        boolean overlappingBlock =
                availabilityRepository.existsOverlappingBlock(
                        propertyId,
                        request.getStartDate(),
                        request.getEndDate()
                );

        if (overlappingBlock) {
            throw new RuntimeException(
                    "These dates are already blocked"
            );
        }

        PropertyAvailability availability =
                PropertyAvailability.builder()
                        .property(property)
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .reason(request.getReason())
                        .build();

        return availabilityRepository.save(availability);
    }

    public List<PropertyAvailability> getBlockedDates(Long propertyId) {

        if (!propertyRepository.existsById(propertyId)) {
            throw new RuntimeException("Property not found");
        }

        return availabilityRepository.findByPropertyId(propertyId);
    }

    public void unblockDates(
            Long propertyId,
            Long blockId,
            Authentication authentication
    ) {

        User host = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Host not found"));

        PropertyAvailability block =
                availabilityRepository.findById(blockId)
                        .orElseThrow(() ->
                                new RuntimeException("Availability block not found")
                        );

        if (!block.getProperty().getId().equals(propertyId)) {
            throw new RuntimeException(
                    "Availability block does not belong to this property"
            );
        }

        if (!block.getProperty()
                .getHost()
                .getId()
                .equals(host.getId())) {

            throw new RuntimeException(
                    "You cannot manage availability for this property"
            );
        }

        availabilityRepository.delete(block);
    }
}