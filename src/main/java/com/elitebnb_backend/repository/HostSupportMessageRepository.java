package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.HostSupportConversation;
import com.elitebnb_backend.entity.HostSupportMessage;
import com.elitebnb_backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HostSupportMessageRepository
        extends JpaRepository<HostSupportMessage, Long> {

    /**
     * Returns the support message history in chronological chat order.
     */
    List<HostSupportMessage> findByConversationOrderByCreatedAtAsc(
            HostSupportConversation conversation
    );

    /**
     * Finds the latest support message for Admin inbox previews.
     */
    Optional<HostSupportMessage> findTopByConversationOrderByCreatedAtDesc(
            HostSupportConversation conversation
    );

    /**
     * Counts unread messages sent by the opposite side. Role-based counting is
     * sufficient for this v1 because the Admin side is shared, while each Host
     * has exactly one support conversation.
     */
    long countByConversationAndSender_RoleAndReadFalse(
            HostSupportConversation conversation,
            Role senderRole
    );

    /**
     * Loads unread messages from the opposite side so mark-read operations do
     * not mutate the viewer's own outgoing support messages.
     */
    List<HostSupportMessage> findByConversationAndSender_RoleAndReadFalse(
            HostSupportConversation conversation,
            Role senderRole
    );
}
