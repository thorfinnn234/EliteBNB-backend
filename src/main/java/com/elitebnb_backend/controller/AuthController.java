package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AuthResponse;
import com.elitebnb_backend.dto.LoginRequest;
import com.elitebnb_backend.dto.RegisterRequest;
import com.elitebnb_backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.elitebnb_backend.dto.VerifyEmailRequest;
import com.elitebnb_backend.dto.ResendVerificationRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody RegisterRequest request
    ) {
        authService.register(request);

        return ResponseEntity.ok(
                "User registered successfully"
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                authService.login(request)
        );
    }
    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @RequestBody VerifyEmailRequest request
    ) {

        authService.verifyEmail(request);

        return ResponseEntity.ok(
                "Email verified successfully"
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(
            @RequestBody ResendVerificationRequest request
    ) {

        authService.resendVerificationCode(request);

        return ResponseEntity.ok(
                "Verification code resent successfully"
        );
    }
}