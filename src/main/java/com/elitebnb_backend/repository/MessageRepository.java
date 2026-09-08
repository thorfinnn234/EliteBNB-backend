package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Conversation;
import com.elitebnb_backend.entity.Message;
import com.elitebnb_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository
        extends JpaRepository<Message, Long> {

    /**
     * Returns the full message history in chat order for a conversation detail
     * screen.
     */
    List<Message> findByConversationOrderByCreatedAtAsc(
            Conversation conversation
    );

    /**
     * Finds the latest message so inbox rows can show a preview without loading
     * the whole conversation history.
     */
    Optional<Message> findTopByConversationOrderByCreatedAtDesc(
            Conversation conversation
    );

    /**
     * Counts unread received messages for the current participant. Messages sent
     * by the viewer are excluded so a sender never sees their own message as
     * unread.
     */
    long countByConversationAndSenderNotAndReadFalse(
            Conversation conversation,
            User sender
    );

    /**
     * Loads only unread messages from the other participant so mark-read cannot
     * alter the viewer's own outgoing messages.
     */
    List<Message> findByConversationAndSenderNotAndReadFalse(
            Conversation conversation,
            User sender
    );
}
