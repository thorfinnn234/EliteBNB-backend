package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.HostVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class HostVerificationResponse {

    private Long id;

    private Long hostId;
    private String hostName;
    private String hostEmail;

    private String legalName;
    private String businessName;
    private String documentType;
    private String documentUrl;

    private HostVerificationStatus status;
    private String adminNote;

    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
