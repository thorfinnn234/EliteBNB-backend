package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private String location;

    private String profileImageUrl;

    private Role role;

    private AccountStatus accountStatus;

    private boolean emailVerified;

    private boolean hostOnboardingCompleted;
}