package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String bio;
    private String location;
    private String profileImageUrl;
}