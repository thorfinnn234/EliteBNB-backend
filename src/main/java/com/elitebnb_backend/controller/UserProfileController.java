package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.UpdateUserProfileRequest;
import com.elitebnb_backend.dto.UserProfileResponse;
import com.elitebnb_backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                userProfileService.getProfile(
                        authentication.getName()
                )
        );
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestBody UpdateUserProfileRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                userProfileService.updateProfile(
                        authentication.getName(),
                        request
                )
        );
    }

    @PostMapping("/image")
    public ResponseEntity<UserProfileResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                userProfileService.uploadProfileImage(
                        authentication.getName(),
                        file
                )
        );
    }
}