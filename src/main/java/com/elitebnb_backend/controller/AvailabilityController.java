package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.BlockAvailabilityRequest;
import com.elitebnb_backend.entity.PropertyAvailability;
import com.elitebnb_backend.service.AvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(
            AvailabilityService availabilityService
    ) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/block")
    public ResponseEntity<PropertyAvailability> blockDates(
            @PathVariable Long propertyId,
            @RequestBody BlockAvailabilityRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                availabilityService.blockDates(
                        propertyId,
                        request,
                        authentication
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<PropertyAvailability>> getBlockedDates(
            @PathVariable Long propertyId
    ) {

        return ResponseEntity.ok(
                availabilityService.getBlockedDates(propertyId)
        );
    }

    @DeleteMapping("/{blockId}")
    public ResponseEntity<Void> unblockDates(
            @PathVariable Long propertyId,
            @PathVariable Long blockId,
            Authentication authentication
    ) {

        availabilityService.unblockDates(
                propertyId,
                blockId,
                authentication
        );

        return ResponseEntity.noContent().build();
    }
}