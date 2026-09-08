package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.HostVerificationRequest;
import com.elitebnb_backend.dto.HostVerificationResponse;
import com.elitebnb_backend.dto.UpdateHostVerificationStatusRequest;
import com.elitebnb_backend.entity.*;
import com.elitebnb_backend.repository.HostVerificationRepository;
import com.elitebnb_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HostVerificationService {

    private final HostVerificationRepository hostVerificationRepository;
    private final UserRepository userRepository;
    private final AdminNotificationService adminNotificationService;
    private final AuditLogService auditLogService;

    public HostVerificationResponse submitVerification(
            HostVerificationRequest request,
            String hostEmail
    ) {

        User host =
                getHost(hostEmail);

        validateRequest(request);

        HostVerification verification =
                hostVerificationRepository
                        .findByHost(host)
                        .orElseGet(() ->
                                HostVerification.builder()
                                        .host(host)
                                        .build()
                        );

        verification.setLegalName(request.getLegalName().trim());
        verification.setBusinessName(
                normalize(request.getBusinessName())
        );
        verification.setDocumentType(
                request.getDocumentType().trim()
        );
        verification.setDocumentUrl(
                normalize(request.getDocumentUrl())
        );
        verification.setStatus(
                HostVerificationStatus.PENDING
        );
        verification.setAdminNote(null);
        verification.setReviewedBy(null);
        verification.setReviewedAt(null);

        HostVerification saved =
                hostVerificationRepository.save(verification);

        adminNotificationService.notifyAdmins(
                "Host verification request",
                host.getFirstName()
                        + " "
                        + host.getLastName()
                        + " submitted verification details.",
                AdminNotificationType.HOST_VERIFICATION_REQUEST,
                saved.getId(),
                "HOST_VERIFICATION"
        );

        return map(saved);
    }

    public HostVerificationResponse getMyVerification(
            String hostEmail
    ) {

        User host =
                getHost(hostEmail);

        HostVerification verification =
                hostVerificationRepository
                        .findByHost(host)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Host verification not found"
                                )
                        );

        return map(verification);
    }

    public List<HostVerificationResponse> getVerifications(
            String search,
            HostVerificationStatus status,
            Long hostId
    ) {

        return hostVerificationRepository
                .searchAdminHostVerifications(
                        search,
                        status,
                        hostId
                )
                .stream()
                .map(this::map)
                .toList();
    }

    public List<HostVerificationResponse> getPendingVerifications() {

        return hostVerificationRepository
                .findByStatusOrderByCreatedAtDesc(
                        HostVerificationStatus.PENDING
                )
                .stream()
                .map(this::map)
                .toList();
    }

    public HostVerificationResponse getVerification(
            Long verificationId
    ) {

        return map(
                getVerificationEntity(verificationId)
        );
    }

    public HostVerificationResponse updateStatus(
            Long verificationId,
            UpdateHostVerificationStatusRequest request,
            String adminEmail
    ) {

        if (request.getStatus() == null) {
            throw new RuntimeException(
                    "Host verification status is required"
            );
        }

        HostVerification verification =
                getVerificationEntity(verificationId);

        User admin =
                getAdmin(adminEmail);

        verification.setStatus(request.getStatus());
        verification.setAdminNote(
                normalize(request.getAdminNote())
        );
        verification.setReviewedBy(admin);
        verification.setReviewedAt(LocalDateTime.now());

        HostVerification saved =
                hostVerificationRepository.save(verification);

        auditLogService.record(
                adminEmail,
                AuditActionType.HOST_VERIFICATION_UPDATED,
                AuditTargetType.HOST_VERIFICATION,
                saved.getId(),
                "Updated host verification for "
                        + saved.getHost().getEmail()
                        + " to "
                        + saved.getStatus().name()
        );

        return map(saved);
    }

    private void validateRequest(
            HostVerificationRequest request
    ) {

        if (request.getLegalName() == null
                || request.getLegalName().trim().isEmpty()) {
            throw new RuntimeException(
                    "Legal name is required"
            );
        }

        if (request.getDocumentType() == null
                || request.getDocumentType().trim().isEmpty()) {
            throw new RuntimeException(
                    "Document type is required"
            );
        }
    }

    private HostVerification getVerificationEntity(
            Long verificationId
    ) {

        return hostVerificationRepository
                .findById(verificationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Host verification not found"
                        )
                );
    }

    private User getHost(
            String email
    ) {

        User host =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Host not found"
                                )
                        );

        if (host.getRole() != Role.HOST) {
            throw new RuntimeException(
                    "Host account required"
            );
        }

        return host;
    }

    private User getAdmin(
            String email
    ) {

        User admin =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Admin account not found"
                                )
                        );

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException(
                    "Admin account required"
            );
        }

        return admin;
    }

    private String normalize(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private HostVerificationResponse map(
            HostVerification verification
    ) {

        String hostName =
                verification.getHost().getFirstName()
                        + " "
                        + verification.getHost().getLastName();

        User reviewedBy =
                verification.getReviewedBy();

        String reviewedByName =
                reviewedBy != null
                        ? reviewedBy.getFirstName()
                        + " "
                        + reviewedBy.getLastName()
                        : null;

        return new HostVerificationResponse(
                verification.getId(),

                verification.getHost().getId(),
                hostName,
                verification.getHost().getEmail(),

                verification.getLegalName(),
                verification.getBusinessName(),
                verification.getDocumentType(),
                verification.getDocumentUrl(),

                verification.getStatus(),
                verification.getAdminNote(),

                reviewedBy != null
                        ? reviewedBy.getId()
                        : null,
                reviewedByName,
                verification.getReviewedAt(),

                verification.getCreatedAt(),
                verification.getUpdatedAt()
        );
    }
}
