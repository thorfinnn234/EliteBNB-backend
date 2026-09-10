package com.elitebnb_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "host_support_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostSupportMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private HostSupportConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_status", nullable = false)
    @Builder.Default
    private boolean read = false;

    private LocalDateTime readAt;

    /**
     * Ensures messages created directly through JPA still receive a creation
     * timestamp before they are stored.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Marks the message as read by the receiving side. Services only call this
     * for messages authored by the opposite side, so senders never read their
     * own outgoing messages.
     */
    public void markRead(
            LocalDateTime readTime
    ) {
        if (!read) {
            read = true;
            readAt = readTime;
        }
    }
}
