package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePaymentStatusRequest {

    private PaymentStatus status;
}
