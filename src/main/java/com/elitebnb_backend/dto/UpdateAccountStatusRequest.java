package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.AccountStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountStatusRequest {

    private AccountStatus status;
}