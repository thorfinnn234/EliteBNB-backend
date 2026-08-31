package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.NotificationResponse;
import com.elitebnb_backend.entity.*;
import com.elitebnb_backend.repository.NotificationRepository;
import com.elitebnb_backend.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // CREATE INTERNAL NOTIFICATION
    public void createNotification(
            User recipient,
            String title,
            String message,
            NotificationType type,
            Booking booking,
            Property property
    ) {

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .booking(booking)
                .property(property)
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    // GET LOGGED-IN USER NOTIFICATIONS
    public List<NotificationResponse> getMyNotifications(
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);

        return notificationRepository
                .findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // UNREAD COUNT
    public long getUnreadCount(
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);

        return notificationRepository
                .countByRecipientAndReadFalse(user);
    }

    // MARK ONE AS READ
    public NotificationResponse markAsRead(
            Long notificationId,
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                )
                        );

        verifyOwnership(notification, user);

        notification.setRead(true);

        Notification updated =
                notificationRepository.save(notification);

        return mapToResponse(updated);
    }

    // MARK ALL AS READ
    public void markAllAsRead(
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);

        List<Notification> notifications =
                notificationRepository
                        .findByRecipientOrderByCreatedAtDesc(user);

        notifications.forEach(notification ->
                notification.setRead(true)
        );

        notificationRepository.saveAll(notifications);
    }

    // DELETE NOTIFICATION
    public void deleteNotification(
            Long notificationId,
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                )
                        );

        verifyOwnership(notification, user);

        notificationRepository.delete(notification);
    }

    private User getAuthenticatedUser(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }

    private void verifyOwnership(
            Notification notification,
            User user
    ) {

        if (!notification
                .getRecipient()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not allowed to manage this notification"
            );
        }
    }

    private NotificationResponse mapToResponse(
            Notification notification
    ) {

        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),

                notification.getBooking() != null
                        ? notification.getBooking().getId()
                        : null,

                notification.getProperty() != null
                        ? notification.getProperty().getId()
                        : null,

                notification.getCreatedAt()
        );
    }
}