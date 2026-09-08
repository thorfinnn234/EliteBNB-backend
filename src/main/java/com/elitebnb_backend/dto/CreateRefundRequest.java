package com.elitebnb_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateRefundRequest {

    private Long paymentId;
    private BigDecimal amount;
    private String reason;
}
