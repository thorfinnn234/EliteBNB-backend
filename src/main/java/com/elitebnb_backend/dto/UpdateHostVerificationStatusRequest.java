package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.HostVerificationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateHostVerificationStatusRequest {

    private HostVerificationStatus status;
    private String adminNote;
}
