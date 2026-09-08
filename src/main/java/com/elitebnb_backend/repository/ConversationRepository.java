package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Conversation;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    /**
     * Supports the one-thread-per-guest-host-property rule used when a guest
     * clicks "Message Host" repeatedly from the same property.
     */
    Optional<Conversation> findByGuestAndHostAndProperty(
            User guest,
            User host,
            Property property
    );

    /**
     * Loads a guest inbox with the most recently active conversation first.
     */
    List<Conversation> findByGuestOrderByUpdatedAtDesc(
            User guest
    );

    /**
     * Loads a host inbox with the most recently active conversation first.
     */
    List<Conversation> findByHostOrderByUpdatedAtDesc(
            User host
    );
}
