package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.HostProfileResponse;
import com.elitebnb_backend.dto.UpdateHostProfileRequest;
import com.elitebnb_backend.service.HostProfileService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/host/profile")
public class HostProfileController {

    private final HostProfileService hostProfileService;

    public HostProfileController(
            HostProfileService hostProfileService
    ) {
        this.hostProfileService =
                hostProfileService;
    }

    @GetMapping
    public ResponseEntity<HostProfileResponse>
    getProfile(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                hostProfileService.getProfile(
                        authentication
                )
        );
    }

    @PutMapping
    public ResponseEntity<HostProfileResponse>
    updateProfile(
            @RequestBody UpdateHostProfileRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                hostProfileService.updateProfile(
                        request,
                        authentication
                )
        );
    }

    @PostMapping(
            value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<HostProfileResponse>
    uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                hostProfileService.uploadProfileImage(
                        file,
                        authentication
                )
        );
    }
}