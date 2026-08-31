package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AddPropertyImageRequest;
import com.elitebnb_backend.dto.CreatePropertyRequest;
import com.elitebnb_backend.dto.PropertyResponse;
import com.elitebnb_backend.dto.UpdatePropertyRequest;

import com.elitebnb_backend.entity.PropertyImage;
import com.elitebnb_backend.entity.PropertyImageType;
import com.elitebnb_backend.entity.PropertyType;

import com.elitebnb_backend.service.PropertyService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(
            PropertyService propertyService
    ) {
        this.propertyService = propertyService;
    }

    // CREATE PROPERTY
    @PostMapping
    public ResponseEntity<PropertyResponse> createProperty(
            @RequestBody CreatePropertyRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                propertyService.createProperty(
                        request,
                        authentication
                )
        );
    }

    // GET ALL PROPERTIES
    @GetMapping
    public ResponseEntity<List<PropertyResponse>> getAllProperties() {

        return ResponseEntity.ok(
                propertyService.getAllProperties()
        );
    }

    // GET MY PROPERTIES
    @GetMapping("/my")
    public ResponseEntity<List<PropertyResponse>> getMyProperties(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                propertyService.getMyProperties(
                        authentication
                )
        );
    }

    // GET ONE PROPERTY
    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponse> getPropertyById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                propertyService.getPropertyById(id)
        );
    }

    // UPDATE PROPERTY
    @PutMapping("/{id}")
    public ResponseEntity<PropertyResponse> updateProperty(
            @PathVariable Long id,
            @RequestBody UpdatePropertyRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                propertyService.updateProperty(
                        id,
                        request,
                        authentication
                )
        );
    }

    // DELETE PROPERTY
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProperty(
            @PathVariable Long id,
            Authentication authentication
    ) {

        propertyService.deleteProperty(
                id,
                authentication
        );

        return ResponseEntity.ok(
                "Property deleted successfully"
        );
    }

    // OLD IMAGE URL ENDPOINT
    @PostMapping("/{id}/images")
    public ResponseEntity<String> addPropertyImage(
            @PathVariable Long id,
            @RequestBody AddPropertyImageRequest request,
            Authentication authentication
    ) {

        propertyService.addPropertyImage(
                id,
                request,
                authentication
        );

        return ResponseEntity.ok(
                "Property image added successfully"
        );
    }

    // REAL IMAGE FILE UPLOAD
    @PostMapping(
            value = "/{propertyId}/images/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PropertyImage> uploadPropertyImage(
            @PathVariable Long propertyId,

            @RequestParam("file")
            MultipartFile file,

            @RequestParam("imageType")
            PropertyImageType imageType,

            @RequestParam(
                    value = "coverImage",
                    defaultValue = "false"
            )
            boolean coverImage,

            Authentication authentication
    ) {

        return ResponseEntity.ok(
                propertyService.uploadPropertyImage(
                        propertyId,
                        file,
                        imageType,
                        coverImage,
                        authentication
                )
        );
    }


    // DELETE PROPERTY IMAGE
    @DeleteMapping(
            "/{propertyId}/images/{imageId}"
    )
    public ResponseEntity<String> deletePropertyImage(
            @PathVariable Long propertyId,
            @PathVariable Long imageId,
            Authentication authentication
    ) {

        propertyService.deletePropertyImage(
                propertyId,
                imageId,
                authentication
        );

        return ResponseEntity.ok(
                "Property image deleted successfully"
        );
    }


    // SET PROPERTY COVER IMAGE
    @PatchMapping(
            "/{propertyId}/images/{imageId}/cover"
    )
    public ResponseEntity<PropertyImage> setCoverImage(
            @PathVariable Long propertyId,
            @PathVariable Long imageId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                propertyService.setCoverImage(
                        propertyId,
                        imageId,
                        authentication
                )
        );
    }
    // GET PROPERTY IMAGES
    @GetMapping("/{id}/images")
    public ResponseEntity<List<PropertyImage>> getPropertyImages(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                propertyService.getPropertyImages(id)
        );
    }

    // SEARCH / FILTER PROPERTIES
    @GetMapping("/search")
    public ResponseEntity<List<PropertyResponse>> searchProperties(

            @RequestParam(required = false)
            String location,

            @RequestParam(required = false)
            PropertyType propertyType,

            @RequestParam(required = false)
            Double minPrice,

            @RequestParam(required = false)
            Double maxPrice,

            @RequestParam(required = false)
            Integer bedrooms,

            @RequestParam(required = false)
            Integer guests
    ) {

        return ResponseEntity.ok(
                propertyService.searchProperties(
                        location,
                        propertyType,
                        minPrice,
                        maxPrice,
                        bedrooms,
                        guests
                )
        );
    }
}