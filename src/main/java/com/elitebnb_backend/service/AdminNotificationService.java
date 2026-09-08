package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AdminNotificationResponse;
import com.elitebnb_backend.entity.AdminNotification;
import com.elitebnb_backend.entity.AdminNotificationType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.AdminNotificationRepository;
import com.elitebnb_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;
    private final UserRepository userRepository;

    public void notifyAdmins(
            String title,
            String message,
            AdminNotificationType type,
            Long targetId,
            String targetType
    ) {

        List<User> admins =
                userRepository.findByRole(Role.ADMIN);

        List<AdminNotification> notifications =
                admins.stream()
                        .map(admin ->
                                AdminNotification.builder()
                                        .admin(admin)
                                        .title(title)
                                        .message(message)
                                        .type(type)
                                        .targetId(targetId)
                                        .targetType(targetType)
                                        .read(false)
                                        .build()
                        )
                        .toList();

        adminNotificationRepository.saveAll(notifications);
    }

    public List<AdminNotificationResponse> getNotifications(
            String adminEmail,
            Boolean read,
            AdminNotificationType type
    ) {

        User admin =
                getAdmin(adminEmail);

        List<AdminNotification> notifications;

        if (read != null) {
            notifications =
                    adminNotificationRepository
                            .findByAdminAndReadOrderByCreatedAtDesc(
                                    admin,
                                    read
                            );
        } else if (type != null) {
            notifications =
                    adminNotificationRepository
                            .findByAdminAndTypeOrderByCreatedAtDesc(
                                    admin,
                                    type
                            );
        } else {
            notifications =
                    adminNotificationRepository
                            .findByAdminOrderByCreatedAtDesc(admin);
        }

        return notifications
                .stream()
                .filter(notification ->
                        type == null
                                || notification.getType() == type
                )
                .map(this::map)
                .toList();
    }

    public long getUnreadCount(
            String adminEmail
    ) {

        User admin =
                getAdmin(adminEmail);

        return adminNotificationRepository
                .countByAdminAndReadFalse(admin);
    }

    public AdminNotificationResponse markAsRead(
            Long notificationId,
            String adminEmail
    ) {

        User admin =
                getAdmin(adminEmail);

        AdminNotification notification =
                getNotificationForAdmin(
                        notificationId,
                        admin
                );

        notification.setRead(true);

        return map(
                adminNotificationRepository.save(notification)
        );
    }

    public void markAllAsRead(
            String adminEmail
    ) {

        User admin =
                getAdmin(adminEmail);

        List<AdminNotification> notifications =
                adminNotificationRepository
                        .findByAdminAndReadOrderByCreatedAtDesc(
                                admin,
                                false
                        );

        notifications.forEach(notification ->
                notification.setRead(true)
        );

        adminNotificationRepository.saveAll(notifications);
    }

    private AdminNotification getNotificationForAdmin(
            Long notificationId,
            User admin
    ) {

        AdminNotification notification =
                adminNotificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Admin notification not found"
                                )
                        );

        if (!notification.getAdmin()
                .getId()
                .equals(admin.getId())) {

            throw new RuntimeException(
                    "You are not allowed to manage this notification"
            );
        }

        return notification;
    }

    private User getAdmin(
            String adminEmail
    ) {

        User admin =
                userRepository
                        .findByEmail(adminEmail)
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

        return admin;
    }

    private AdminNotificationResponse map(
            AdminNotification notification
    ) {

        return new AdminNotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getTargetId(),
                notification.getTargetType(),
                notification.getCreatedAt()
        );
    }
}
