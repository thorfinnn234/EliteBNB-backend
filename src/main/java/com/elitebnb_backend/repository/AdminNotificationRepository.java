package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.AdminNotification;
import com.elitebnb_backend.entity.AdminNotificationType;
import com.elitebnb_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminNotificationRepository
        extends JpaRepository<AdminNotification, Long> {

    List<AdminNotification> findByAdminOrderByCreatedAtDesc(
            User admin
    );

    List<AdminNotification> findByAdminAndReadOrderByCreatedAtDesc(
            User admin,
            boolean read
    );

    List<AdminNotification> findByAdminAndTypeOrderByCreatedAtDesc(
            User admin,
            AdminNotificationType type
    );

    long countByAdminAndReadFalse(
            User admin
    );
}
