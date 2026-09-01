package com.elitebnb_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phoneNumber;

    @Column(length = 1000)
    private String bio;

    private String location;

    @Column(length = 1000)
    private String profileImageUrl;

    // =========================
    // HOST ONBOARDING
    // =========================

    @Column(nullable = false)
    @Builder.Default
    private boolean hostOnboardingCompleted = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer hostOnboardingStep = 1;

    private String address;

    private String city;

    private String state;

    private String country;



    // =========================
    // EMAIL VERIFICATION
    // =========================

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    private String verificationCode;

    private LocalDateTime verificationCodeExpiry;

    // =========================
    // ROLE
    // =========================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}