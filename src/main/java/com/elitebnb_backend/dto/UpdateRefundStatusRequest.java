package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.RefundProvider;
import com.elitebnb_backend.entity.RefundStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRefundStatusRequest {

    private RefundStatus status;
    private RefundProvider provider;
    private String providerReference;
    private String adminNote;
}
