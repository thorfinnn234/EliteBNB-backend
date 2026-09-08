package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.HostVerificationResponse;
import com.elitebnb_backend.dto.UpdateHostVerificationStatusRequest;
import com.elitebnb_backend.entity.HostVerificationStatus;
import com.elitebnb_backend.service.HostVerificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/host-verifications")
@RequiredArgsConstructor
public class AdminHostVerificationController {

    private final HostVerificationService hostVerificationService;

    @GetMapping
    public ResponseEntity<List<HostVerificationResponse>> getVerifications(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            HostVerificationStatus status,

            @RequestParam(required = false)
            Long hostId
    ) {

        return ResponseEntity.ok(
                hostVerificationService.getVerifications(
                        search,
                        status,
                        hostId
                )
        );
    }

    @GetMapping("/pending")
    public ResponseEntity<List<HostVerificationResponse>> getPending() {

        return ResponseEntity.ok(
                hostVerificationService.getPendingVerifications()
        );
    }

    @GetMapping("/{verificationId}")
    public ResponseEntity<HostVerificationResponse> getVerification(
            @PathVariable Long verificationId
    ) {

        return ResponseEntity.ok(
                hostVerificationService.getVerification(
                        verificationId
                )
        );
    }

    @PatchMapping("/{verificationId}/status")
    public ResponseEntity<HostVerificationResponse> updateStatus(
            @PathVariable Long verificationId,
            @RequestBody UpdateHostVerificationStatusRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                hostVerificationService.updateStatus(
                        verificationId,
                        request,
                        authentication.getName()
                )
        );
    }
}
