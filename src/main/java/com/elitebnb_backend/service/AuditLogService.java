package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AuditLogResponse;
import com.elitebnb_backend.entity.AuditActionType;
import com.elitebnb_backend.entity.AuditLog;
import com.elitebnb_backend.entity.AuditTargetType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.AuditLogRepository;
import com.elitebnb_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void record(
            String adminEmail,
            AuditActionType action,
            AuditTargetType targetType,
            Long targetId,
            String description
    ) {

        User admin =
                userRepository.findByEmail(adminEmail)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Admin account not found"
                                )
                        );

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException(
                    "Admin account required"
            );
        }

        AuditLog auditLog =
                AuditLog.builder()
                        .admin(admin)
                        .action(action)
                        .targetType(targetType)
                        .targetId(targetId)
                        .description(description)
                        .build();

        auditLogRepository.save(auditLog);
    }

    public List<AuditLogResponse> getAuditLogs(
            String search,
            AuditActionType action,
            AuditTargetType targetType,
            Long targetId,
            Long adminId
    ) {

        return auditLogRepository
                .searchAdminAuditLogs(
                        search,
                        action,
                        targetType,
                        targetId,
                        adminId
                )
                .stream()
                .map(this::map)
                .toList();
    }

    public AuditLogResponse getAuditLog(
            Long auditLogId
    ) {

        AuditLog auditLog =
                auditLogRepository
                        .findById(auditLogId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Audit log not found"
                                )
                        );

        return map(auditLog);
    }

    private AuditLogResponse map(
            AuditLog auditLog
    ) {

        String adminName =
                auditLog.getAdmin().getFirstName()
                        + " "
                        + auditLog.getAdmin().getLastName();

        return new AuditLogResponse(
                auditLog.getId(),

                auditLog.getAdmin().getId(),
                adminName,
                auditLog.getAdmin().getEmail(),

                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),

                auditLog.getDescription(),
                auditLog.getCreatedAt()
        );
    }
}