package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.HostOnboardingRequest;
import com.elitebnb_backend.dto.HostOnboardingResponse;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HostOnboardingService {

    private final UserRepository userRepository;

    // =========================
    // GET ONBOARDING PROGRESS
    // =========================

    public HostOnboardingResponse getOnboarding(String email) {
        return mapToResponse(getHost(email));
    }

    // =========================
    // SAVE ONBOARDING PROGRESS
    // =========================

    public HostOnboardingResponse saveOnboarding(
            String email,
            HostOnboardingRequest request
    ) {
        User host = getHost(email);

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

        if (request.getAddress() != null) {
            host.setAddress(
                    request.getAddress().trim()
            );
        }

        if (request.getCity() != null) {
            host.setCity(
                    request.getCity().trim()
            );
        }

        if (request.getState() != null) {
            host.setState(
                    request.getState().trim()
            );
        }

        if (request.getCountry() != null) {
            host.setCountry(
                    request.getCountry().trim()
            );
        }

        /*
         * Remember where the host stopped.
         *
         * Valid onboarding steps are currently 1 - 11.
         */
        if (request.getCurrentStep() != null) {

            int step = Math.max(
                    1,
                    Math.min(
                            request.getCurrentStep(),
                            11
                    )
            );

            host.setHostOnboardingStep(step);
        }

        User savedHost =
                userRepository.save(host);

        return mapToResponse(savedHost);
    }

    // =========================
    // COMPLETE ONBOARDING
    // =========================

    public HostOnboardingResponse completeOnboarding(
            String email
    ) {
        User host = getHost(email);

        validateHostDetails(host);

        /*
         * Later we will also verify that the host
         * has successfully created/published
         * their first property before allowing
         * onboarding to complete.
         */

        host.setHostOnboardingCompleted(true);

        // Final onboarding step
        host.setHostOnboardingStep(11);

        User savedHost =
                userRepository.save(host);

        return mapToResponse(savedHost);
    }

    // =========================
    // VALIDATION
    // =========================

    private void validateHostDetails(User host) {

        if (isBlank(host.getPhoneNumber())) {
            throw new RuntimeException(
                    "Phone number is required"
            );
        }

        if (isBlank(host.getAddress())) {
            throw new RuntimeException(
                    "Address is required"
            );
        }

        if (isBlank(host.getCity())) {
            throw new RuntimeException(
                    "City is required"
            );
        }

        if (isBlank(host.getState())) {
            throw new RuntimeException(
                    "State is required"
            );
        }

        if (isBlank(host.getCountry())) {
            throw new RuntimeException(
                    "Country is required"
            );
        }
    }

    // =========================
    // GET CURRENT HOST
    // =========================

    private User getHost(String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );

        if (user.getRole() != Role.HOST) {
            throw new RuntimeException(
                    "Host account required"
            );
        }

        return user;
    }

    private boolean isBlank(String value) {
        return value == null ||
                value.trim().isEmpty();
    }

    // =========================
    // RESPONSE MAPPER
    // =========================

    private HostOnboardingResponse mapToResponse(
            User host
    ) {
        return new HostOnboardingResponse(
                host.getId(),
                host.getFirstName(),
                host.getLastName(),
                host.getEmail(),
                host.getPhoneNumber(),
                host.getBio(),
                host.getProfileImageUrl(),
                host.getAddress(),
                host.getCity(),
                host.getState(),
                host.getCountry(),
                host.isHostOnboardingCompleted(),
                host.getHostOnboardingStep()
        );
    }
}