package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.RefundProvider;
import com.elitebnb_backend.entity.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RefundResponse {

    private Long id;

    private Long bookingId;
    private Long paymentId;
    private String paymentReference;

    private Long requestedById;
    private String requestedByName;
    private String requestedByEmail;

    private Long propertyId;
    private String propertyTitle;

    private BigDecimal amount;
    private String reason;
    private RefundStatus status;
    private RefundProvider provider;
    private String providerReference;
    private String adminNote;

    private Long processedById;
    private String processedByName;
    private LocalDateTime processedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
