package com.elitebnb_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateHostProfileRequest {

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String bio;
    private String location;
}