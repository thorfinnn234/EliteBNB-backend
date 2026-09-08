package com.elitebnb_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePlatformSettingRequest {

    private String value;
    private String description;
}
