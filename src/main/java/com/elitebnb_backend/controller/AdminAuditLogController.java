package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AuditLogResponse;
import com.elitebnb_backend.entity.AuditActionType;
import com.elitebnb_backend.entity.AuditTargetType;
import com.elitebnb_backend.service.AuditLogService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            AuditActionType action,

            @RequestParam(required = false)
            AuditTargetType targetType,

            @RequestParam(required = false)
            Long targetId,

            @RequestParam(required = false)
            Long adminId
    ) {

        return ResponseEntity.ok(
                auditLogService.getAuditLogs(
                        search,
                        action,
                        targetType,
                        targetId,
                        adminId
                )
        );
    }

    @GetMapping("/{auditLogId}")
    public ResponseEntity<AuditLogResponse> getAuditLog(
            @PathVariable Long auditLogId
    ) {

        return ResponseEntity.ok(
                auditLogService.getAuditLog(
                        auditLogId
                )
        );
    }
}
