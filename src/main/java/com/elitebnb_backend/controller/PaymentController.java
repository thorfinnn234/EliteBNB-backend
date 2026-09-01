package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.PaystackInitializeResponse;
import com.elitebnb_backend.dto.PaymentResponse;
import com.elitebnb_backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize/{bookingId}")
    public ResponseEntity<PaystackInitializeResponse>
    initializePayment(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                paymentService.initializePayment(
                        bookingId,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/verify/{reference}")
    public ResponseEntity<PaymentResponse>
    verifyPayment(
            @PathVariable String reference,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                paymentService.verifyPayment(
                        reference,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse>
    getBookingPayment(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                paymentService.getBookingPayment(
                        bookingId,
                        authentication.getName()
                )
        );
    }
}