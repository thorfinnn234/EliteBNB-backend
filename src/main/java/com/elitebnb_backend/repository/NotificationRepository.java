package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Notification;
import com.elitebnb_backend.entity.NotificationType;
import com.elitebnb_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification>
    findByRecipientOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndReadFalse(User recipient);

    List<Notification>
    findByRecipientAndTypeAndConversationIdAndReadFalse(
            User recipient,
            NotificationType type,
            Long conversationId
    );

    List<Notification>
    findByRecipientAndTypeAndHostSupportConversationIdAndReadFalse(
            User recipient,
            NotificationType type,
            Long hostSupportConversationId
    );
}
