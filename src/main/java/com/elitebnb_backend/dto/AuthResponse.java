package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String message;
    private String token;
    private String email;
    private Role role;
}