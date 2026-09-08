package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.AuditActionType;
import com.elitebnb_backend.entity.AuditLog;
import com.elitebnb_backend.entity.AuditTargetType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE (
                :search IS NULL
                OR :search = ''
                OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.admin.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.admin.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.admin.email) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:action IS NULL OR a.action = :action)
            AND (:targetType IS NULL OR a.targetType = :targetType)
            AND (:targetId IS NULL OR a.targetId = :targetId)
            AND (:adminId IS NULL OR a.admin.id = :adminId)
            ORDER BY a.id DESC
            """)
    List<AuditLog> searchAdminAuditLogs(
            @Param("search") String search,
            @Param("action") AuditActionType action,
            @Param("targetType") AuditTargetType targetType,
            @Param("targetId") Long targetId,
            @Param("adminId") Long adminId
    );
}