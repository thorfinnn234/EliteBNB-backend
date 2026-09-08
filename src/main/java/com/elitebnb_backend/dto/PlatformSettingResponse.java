package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.PlatformSettingKey;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PlatformSettingResponse {

    private Long id;
    private PlatformSettingKey key;
    private String value;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
