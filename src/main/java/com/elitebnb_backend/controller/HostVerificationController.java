package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.HostVerificationRequest;
import com.elitebnb_backend.dto.HostVerificationResponse;
import com.elitebnb_backend.service.HostVerificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host/verification")
@RequiredArgsConstructor
public class HostVerificationController {

    private final HostVerificationService hostVerificationService;

    @PostMapping
    public ResponseEntity<HostVerificationResponse> submitVerification(
            @RequestBody HostVerificationRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                hostVerificationService.submitVerification(
                        request,
                        authentication.getName()
                )
        );
    }

    @GetMapping
    public ResponseEntity<HostVerificationResponse> getMyVerification(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                hostVerificationService.getMyVerification(
                        authentication.getName()
                )
        );
    }
}
