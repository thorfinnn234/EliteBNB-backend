package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.UpdateUserProfileRequest;
import com.elitebnb_backend.dto.UserProfileResponse;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public UserProfileResponse getProfile(String email) {
        User user = getUser(email);

        return mapToResponse(user);
    }

    public UserProfileResponse updateProfile(
            String email,
            UpdateUserProfileRequest request
    ) {
        User user = getUser(email);

        if (request.getFirstName() != null &&
                !request.getFirstName().trim().isEmpty()) {
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null &&
                !request.getLastName().trim().isEmpty()) {
            user.setLastName(request.getLastName().trim());
        }

        user.setPhoneNumber(request.getPhoneNumber());
        user.setBio(request.getBio());
        user.setLocation(request.getLocation());

        return mapToResponse(
                userRepository.save(user)
        );
    }

    public UserProfileResponse uploadProfileImage(
            String email,
            MultipartFile file
    ) {
        User user = getUser(email);

        String imageUrl =
                cloudinaryService.uploadImage(file);

        user.setProfileImageUrl(imageUrl);

        return mapToResponse(
                userRepository.save(user)
        );
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );
    }

    private UserProfileResponse mapToResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getBio(),
                user.getLocation(),
                user.getProfileImageUrl()
        );
    }
}