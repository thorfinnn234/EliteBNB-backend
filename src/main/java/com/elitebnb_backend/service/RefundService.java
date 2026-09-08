package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.CreateRefundRequest;
import com.elitebnb_backend.dto.RefundResponse;
import com.elitebnb_backend.dto.UpdateRefundStatusRequest;
import com.elitebnb_backend.entity.*;
import com.elitebnb_backend.repository.PaymentRepository;
import com.elitebnb_backend.repository.RefundRepository;
import com.elitebnb_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PlatformSettingService platformSettingService;
    private final AdminNotificationService adminNotificationService;
    private final AuditLogService auditLogService;

    public RefundResponse requestRefund(
            CreateRefundRequest request,
            String userEmail
    ) {

        if (!platformSettingService.isRefundsEnabled()) {
            throw new RuntimeException(
                    "Refund requests are currently disabled"
            );
        }

        User user =
                getUser(userEmail);

        validateCreateRequest(request);

        Payment payment =
                paymentRepository
                        .findById(request.getPaymentId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found"
                                )
                        );

        if (!payment.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You cannot request a refund for this payment"
            );
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new RuntimeException(
                    "Only successful payments can be refunded"
            );
        }

        if (request.getAmount()
                .compareTo(payment.getAmount()) > 0) {

            throw new RuntimeException(
                    "Refund amount cannot exceed payment amount"
            );
        }

        boolean activeRefundExists =
                refundRepository.existsByPaymentAndStatusIn(
                        payment,
                        List.of(
                                RefundStatus.REQUESTED,
                                RefundStatus.APPROVED,
                                RefundStatus.PROCESSING
                        )
                );

        if (activeRefundExists) {
            throw new RuntimeException(
                    "This payment already has an active refund request"
            );
        }

        Refund refund =
                Refund.builder()
                        .booking(payment.getBooking())
                        .payment(payment)
                        .requestedBy(user)
                        .amount(request.getAmount())
                        .reason(request.getReason().trim())
                        .status(RefundStatus.REQUESTED)
                        .provider(RefundProvider.PAYSTACK)
                        .build();

        Refund saved =
                refundRepository.save(refund);

        adminNotificationService.notifyAdmins(
                "Refund request",
                user.getFirstName()
                        + " "
                        + user.getLastName()
                        + " requested a refund for payment "
                        + payment.getReference()
                        + ".",
                AdminNotificationType.REFUND_REQUEST,
                saved.getId(),
                "REFUND"
        );

        return map(saved);
    }

    public List<RefundResponse> getMyRefunds(
            String userEmail
    ) {

        User user =
                getUser(userEmail);

        return refundRepository
                .findByRequestedByOrderByCreatedAtDesc(user)
                .stream()
                .map(this::map)
                .toList();
    }

    public List<RefundResponse> getRefunds(
            String search,
            RefundStatus status,
            Long bookingId,
            Long paymentId,
            Long requestedById
    ) {

        return refundRepository
                .searchAdminRefunds(
                        search,
                        status,
                        bookingId,
                        paymentId,
                        requestedById
                )
                .stream()
                .map(this::map)
                .toList();
    }

    public RefundResponse getRefund(
            Long refundId
    ) {

        return map(
                getRefundEntity(refundId)
        );
    }

    public RefundResponse updateStatus(
            Long refundId,
            UpdateRefundStatusRequest request,
            String adminEmail
    ) {

        if (request.getStatus() == null) {
            throw new RuntimeException(
                    "Refund status is required"
            );
        }

        Refund refund =
                getRefundEntity(refundId);

        User admin =
                getAdmin(adminEmail);

        validateStatusTransition(
                refund.getStatus(),
                request.getStatus()
        );

        refund.setStatus(request.getStatus());

        if (request.getProvider() != null) {
            refund.setProvider(request.getProvider());
        }

        refund.setProviderReference(
                normalize(request.getProviderReference())
        );

        refund.setAdminNote(
                normalize(request.getAdminNote())
        );

        if (request.getStatus() == RefundStatus.REFUNDED
                || request.getStatus() == RefundStatus.REJECTED) {

            refund.setProcessedBy(admin);
            refund.setProcessedAt(LocalDateTime.now());
        }

        Refund saved =
                refundRepository.save(refund);

        auditLogService.record(
                adminEmail,
                AuditActionType.REFUND_STATUS_UPDATED,
                AuditTargetType.REFUND,
                saved.getId(),
                "Updated refund #"
                        + saved.getId()
                        + " status to "
                        + saved.getStatus().name()
        );

        return map(saved);
    }

    private void validateCreateRequest(
            CreateRefundRequest request
    ) {

        if (request.getPaymentId() == null) {
            throw new RuntimeException(
                    "Payment ID is required"
            );
        }

        if (request.getAmount() == null
                || request.getAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Refund amount must be greater than zero"
            );
        }

        if (request.getReason() == null
                || request.getReason().trim().isEmpty()) {

            throw new RuntimeException(
                    "Refund reason is required"
            );
        }
    }

    private void validateStatusTransition(
            RefundStatus currentStatus,
            RefundStatus nextStatus
    ) {

        if (currentStatus == RefundStatus.REFUNDED
                || currentStatus == RefundStatus.REJECTED) {

            throw new RuntimeException(
                    "Finalized refunds cannot be changed"
            );
        }

        if (nextStatus == RefundStatus.REQUESTED
                && currentStatus != RefundStatus.REQUESTED) {

            throw new RuntimeException(
                    "Refund cannot be moved back to requested"
            );
        }
    }

    private Refund getRefundEntity(
            Long refundId
    ) {

        return refundRepository
                .findById(refundId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Refund not found"
                        )
                );
    }

    private User getUser(
            String email
    ) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }

    private User getAdmin(
            String email
    ) {

        User admin =
                getUser(email);

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

    private RefundResponse map(
            Refund refund
    ) {

        String requestedByName =
                refund.getRequestedBy().getFirstName()
                        + " "
                        + refund.getRequestedBy().getLastName();

        User processedBy =
                refund.getProcessedBy();

        String processedByName =
                processedBy != null
                        ? processedBy.getFirstName()
                        + " "
                        + processedBy.getLastName()
                        : null;

        return new RefundResponse(
                refund.getId(),

                refund.getBooking().getId(),
                refund.getPayment().getId(),
                refund.getPayment().getReference(),

                refund.getRequestedBy().getId(),
                requestedByName,
                refund.getRequestedBy().getEmail(),

                refund.getBooking().getProperty().getId(),
                refund.getBooking().getProperty().getTitle(),

                refund.getAmount(),
                refund.getReason(),
                refund.getStatus(),
                refund.getProvider(),
                refund.getProviderReference(),
                refund.getAdminNote(),

                processedBy != null
                        ? processedBy.getId()
                        : null,
                processedByName,
                refund.getProcessedAt(),

                refund.getCreatedAt(),
                refund.getUpdatedAt()
        );
    }
}
