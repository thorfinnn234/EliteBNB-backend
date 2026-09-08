package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.AuditActionType;
import com.elitebnb_backend.entity.AuditTargetType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;

    private Long adminId;
    private String adminName;
    private String adminEmail;

    private AuditActionType action;
    private AuditTargetType targetType;
    private Long targetId;

    private String description;
    private LocalDateTime createdAt;
}
