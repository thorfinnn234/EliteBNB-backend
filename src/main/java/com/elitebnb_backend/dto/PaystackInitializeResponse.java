package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaystackInitializeResponse {

    private String authorizationUrl;
    private String accessCode;
    private String reference;
}