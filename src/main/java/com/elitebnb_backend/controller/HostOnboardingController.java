package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.HostOnboardingRequest;
import com.elitebnb_backend.dto.HostOnboardingResponse;
import com.elitebnb_backend.service.HostOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host/onboarding")
@RequiredArgsConstructor
public class HostOnboardingController {

    private final HostOnboardingService hostOnboardingService;

    @GetMapping
    public ResponseEntity<HostOnboardingResponse> getOnboarding(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostOnboardingService.getOnboarding(
                        authentication.getName()
                )
        );
    }

    @PutMapping
    public ResponseEntity<HostOnboardingResponse> saveOnboarding(
            @RequestBody HostOnboardingRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostOnboardingService.saveOnboarding(
                        authentication.getName(),
                        request
                )
        );
    }

    @PostMapping("/complete")
    public ResponseEntity<HostOnboardingResponse> completeOnboarding(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostOnboardingService.completeOnboarding(
                        authentication.getName()
                )
        );
    }
}