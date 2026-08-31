package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.HostProfileResponse;
import com.elitebnb_backend.dto.UpdateHostProfileRequest;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HostProfileService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public HostProfileService(
            UserRepository userRepository,
            CloudinaryService cloudinaryService
    ) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public HostProfileResponse getProfile(
            Authentication authentication
    ) {

        User host = getAuthenticatedHost(authentication);

        return mapToResponse(host);
    }

    public HostProfileResponse updateProfile(
            UpdateHostProfileRequest request,
            Authentication authentication
    ) {

        User host = getAuthenticatedHost(authentication);

        if (
                request.getFirstName() != null &&
                        !request.getFirstName().isBlank()
        ) {
            host.setFirstName(
                    request.getFirstName().trim()
            );
        }

        if (
                request.getLastName() != null &&
                        !request.getLastName().isBlank()
        ) {
            host.setLastName(
                    request.getLastName().trim()
            );
        }

        if (request.getPhoneNumber() != null) {
            host.setPhoneNumber(
                    request.getPhoneNumber().trim()
            );
        }

        if (request.getBio() != null) {
            host.setBio(
                    request.getBio().trim()
            );
        }

        if (request.getLocation() != null) {
            host.setLocation(
                    request.getLocation().trim()
            );
        }

        User updatedHost =
                userRepository.save(host);

        return mapToResponse(updatedHost);
    }

    public HostProfileResponse uploadProfileImage(
            MultipartFile file,
            Authentication authentication
    ) {

        User host = getAuthenticatedHost(authentication);

        if (file == null || file.isEmpty()) {
            throw new RuntimeException(
                    "Profile image is required"
            );
        }

        String imageUrl =
                cloudinaryService.uploadImage(file);

        host.setProfileImageUrl(imageUrl);

        User updatedHost =
                userRepository.save(host);

        return mapToResponse(updatedHost);
    }

    private User getAuthenticatedHost(
            Authentication authentication
    ) {

        if (
                authentication == null ||
                        !authentication.isAuthenticated()
        ) {
            throw new RuntimeException(
                    "Authentication required"
            );
        }

        String email = authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Host not found"
                        )
                );
    }

    private HostProfileResponse mapToResponse(
            User host
    ) {

        return new HostProfileResponse(
                host.getId(),
                host.getFirstName(),
                host.getLastName(),
                host.getEmail(),
                host.getPhoneNumber(),
                host.getBio(),
                host.getLocation(),
                host.getProfileImageUrl()
        );
    }
}