package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostOnboardingResponse {

    private Long id;

    private String firstName;
    private String lastName;
    private String email;

    private String phoneNumber;
    private String bio;
    private String profileImageUrl;

    private String address;
    private String city;
    private String state;
    private String country;

    private boolean hostOnboardingCompleted;

    private Integer hostOnboardingStep;
}