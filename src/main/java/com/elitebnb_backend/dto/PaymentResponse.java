package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long bookingId;

    private BigDecimal amount;
    private String currency;

    private String reference;

    private PaymentStatus status;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}