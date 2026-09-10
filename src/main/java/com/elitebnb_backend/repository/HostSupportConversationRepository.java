package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.HostSupportConversation;
import com.elitebnb_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HostSupportConversationRepository
        extends JpaRepository<HostSupportConversation, Long> {

    /**
     * Implements the one-support-thread-per-Host rule used by both Host and
     * Admin support messaging flows.
     */
    Optional<HostSupportConversation> findByHost(
            User host
    );

    /**
     * Builds the Admin support inbox with the most recently active thread
     * first.
     */
    List<HostSupportConversation> findAllByOrderByUpdatedAtDesc();
}
