package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AdminPropertyResponse;
import com.elitebnb_backend.dto.UpdatePropertyApprovalRequest;
import com.elitebnb_backend.dto.UpdatePropertyStatusRequest;
import com.elitebnb_backend.entity.PropertyApprovalStatus;
import com.elitebnb_backend.entity.PropertyStatus;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.service.AdminPropertyService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/properties")
@RequiredArgsConstructor
public class AdminPropertyController {

    private final AdminPropertyService adminPropertyService;

    @GetMapping
    public ResponseEntity<List<AdminPropertyResponse>> getProperties(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            PropertyStatus status,

            @RequestParam(required = false)
            PropertyApprovalStatus approvalStatus,

            @RequestParam(required = false)
            PropertyType propertyType
    ) {

        return ResponseEntity.ok(
                adminPropertyService.getProperties(
                        search,
                        status,
                        approvalStatus,
                        propertyType
                )
        );
    }

    @GetMapping("/approvals")
    public ResponseEntity<List<AdminPropertyResponse>> getPendingApprovals() {

        return ResponseEntity.ok(
                adminPropertyService.getPendingApprovals()
        );
    }

    @GetMapping("/{propertyId}")
    public ResponseEntity<AdminPropertyResponse> getProperty(
            @PathVariable Long propertyId
    ) {

        return ResponseEntity.ok(
                adminPropertyService.getProperty(
                        propertyId
                )
        );
    }

    @PatchMapping("/{propertyId}/status")
    public ResponseEntity<AdminPropertyResponse> updateStatus(
            @PathVariable Long propertyId,
            @RequestBody UpdatePropertyStatusRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                adminPropertyService.updateStatus(
                        propertyId,
                        request.getStatus(),
                        authentication.getName()
                )
        );
    }

    @PatchMapping("/{propertyId}/approval")
    public ResponseEntity<AdminPropertyResponse> updateApproval(
            @PathVariable Long propertyId,
            @RequestBody UpdatePropertyApprovalRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                adminPropertyService.updateApproval(
                        propertyId,
                        request,
                        authentication.getName()
                )
        );
    }
}
