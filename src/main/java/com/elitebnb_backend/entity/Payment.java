package com.elitebnb_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(
            name = "booking_id",
            nullable = false,
            unique = true
    )
    private Booking booking;

    @ManyToOne
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "NGN";

    @Column(
            nullable = false,
            unique = true
    )
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status =
            PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentProvider provider =
            PaymentProvider.PAYSTACK;

    private LocalDateTime paidAt;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt =
            LocalDateTime.now();
}