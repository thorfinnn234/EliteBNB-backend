package com.elitebnb_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostOnboardingRequest {

    private String phoneNumber;
    private String bio;

    private String address;
    private String city;
    private String state;
    private String country;

    private Integer currentStep;
}