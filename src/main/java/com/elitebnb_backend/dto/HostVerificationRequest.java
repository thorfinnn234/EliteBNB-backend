package com.elitebnb_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostVerificationRequest {

    private String legalName;
    private String businessName;
    private String documentType;
    private String documentUrl;
}
