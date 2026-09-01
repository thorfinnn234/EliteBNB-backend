package com.elitebnb_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_booking",
                        columnNames = "booking_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 1500)
    private String comment;

    @Column(length = 1500)
    private String hostResponse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private User guest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private LocalDateTime respondedAt;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}