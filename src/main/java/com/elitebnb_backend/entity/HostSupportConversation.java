package com.elitebnb_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "host_support_conversations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_host_support_conversation_host",
                        columnNames = "host_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostSupportConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false, unique = true)
    private User host;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Applies timestamp defaults when Hibernate creates a row without Lombok
     * builder defaults, such as in tests or future persistence utilities.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * Keeps the support inbox ordered by the latest meaningful thread update.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Moves this Host support thread to the top of the Admin and Host support
     * inboxes after a new support message is sent.
     */
    public void touch() {
        updatedAt = LocalDateTime.now();
    }
}
