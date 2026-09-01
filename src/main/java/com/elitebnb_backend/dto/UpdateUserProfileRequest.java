package com.elitebnb_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserProfileRequest {

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String bio;
    private String location;
}