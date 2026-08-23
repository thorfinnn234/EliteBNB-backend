package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.AuthResponse;
import com.elitebnb_backend.dto.LoginRequest;
import com.elitebnb_backend.dto.RegisterRequest;
import com.elitebnb_backend.dto.VerifyEmailRequest;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.UserRepository;
import com.elitebnb_backend.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    // REGISTER
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        String verificationCode =
                String.format(
                        "%06d",
                        new Random().nextInt(1_000_000)
                );

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .phoneNumber(request.getPhoneNumber())
                .role(
                        request.getRole() != null
                                ? request.getRole()
                                : Role.USER
                )
                .emailVerified(false)
                .verificationCode(verificationCode)
                .verificationCodeExpiry(
                        LocalDateTime.now().plusMinutes(10)
                )
                .build();

        userRepository.save(user);

        emailService.sendVerificationCode(
                user.getEmail(),
                verificationCode
        );
    }

    // VERIFY EMAIL
    public void verifyEmail(
            VerifyEmailRequest request
    ) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );

        if (user.isEmailVerified()) {
            throw new RuntimeException(
                    "Email already verified"
            );
        }

        if (user.getVerificationCode() == null
                || !user.getVerificationCode()
                .equals(request.getCode())) {

            throw new RuntimeException(
                    "Invalid verification code"
            );
        }

        if (user.getVerificationCodeExpiry() == null
                || user.getVerificationCodeExpiry()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Verification code has expired"
            );
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);

        userRepository.save(user);
    }

    // LOGIN
    public AuthResponse login(
            LoginRequest request
    ) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid email or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException(
                    "Please verify your email before logging in"
            );
        }

        String token =
                jwtService.generateToken(user);

        return new AuthResponse(
                "Login successful",
                token,
                user.getEmail(),
                user.getRole()
        );
    }
}