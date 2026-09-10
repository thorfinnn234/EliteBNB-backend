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

    /**
     * Creates a linked notification for the existing USER <-> HOST conversation
     * system. Booking and property context stay available, while conversationId
     * and messageId give the frontend an exact chat target.
     */
    public void createConversationMessageNotification(
            User recipient,
            String title,
            String message,
            Long conversationId,
            Long messageId,
            Booking booking,
            Property property
    ) {

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(NotificationType.MESSAGE)
                .booking(booking)
                .property(property)
                .conversationId(conversationId)
                .messageId(messageId)
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Creates a linked notification for Admin-to-Host verification support
     * messages. Support conversation ids are separate from normal conversation
     * ids so the frontend can route to the correct support screen.
     */
    public void createHostSupportMessageNotification(
            User recipient,
            String title,
            String message,
            Long hostSupportConversationId,
            Long hostSupportMessageId
    ) {

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(NotificationType.HOST_SUPPORT_MESSAGE)
                .hostSupportConversationId(hostSupportConversationId)
                .hostSupportMessageId(hostSupportMessageId)
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Synchronizes normal conversation read state with notification read state.
     * Only unread MESSAGE notifications for this exact recipient and
     * conversation are marked; booking, review, system, and other conversation
     * notifications are not loaded by the repository query.
     */
    public void markConversationMessageNotificationsRead(
            User recipient,
            Long conversationId
    ) {

        List<Notification> notifications =
                notificationRepository
                        .findByRecipientAndTypeAndConversationIdAndReadFalse(
                                recipient,
                                NotificationType.MESSAGE,
                                conversationId
                        );

        notifications.forEach(notification ->
                notification.setRead(true)
        );

        notificationRepository.saveAll(notifications);
    }

    /**
     * Synchronizes Host-side support message reads with their linked
     * HOST_SUPPORT_MESSAGE notifications. This intentionally targets the Host's
     * normal notification inbox, not AdminNotification.
     */
    public void markHostSupportMessageNotificationsRead(
            User recipient,
            Long hostSupportConversationId
    ) {

        List<Notification> notifications =
                notificationRepository
                        .findByRecipientAndTypeAndHostSupportConversationIdAndReadFalse(
                                recipient,
                                NotificationType.HOST_SUPPORT_MESSAGE,
                                hostSupportConversationId
                        );

        notifications.forEach(notification ->
                notification.setRead(true)
        );

        notificationRepository.saveAll(notifications);
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

                notification.getConversationId(),
                notification.getMessageId(),
                notification.getHostSupportConversationId(),
                notification.getHostSupportMessageId(),

                notification.getCreatedAt()
        );
    }
}
