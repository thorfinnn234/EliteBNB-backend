package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.PropertyStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePropertyStatusRequest {

    private PropertyStatus status;
}