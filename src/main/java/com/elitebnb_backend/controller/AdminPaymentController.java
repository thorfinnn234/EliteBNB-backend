package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AdminPaymentResponse;
import com.elitebnb_backend.dto.UpdatePaymentStatusRequest;
import com.elitebnb_backend.entity.PaymentProvider;
import com.elitebnb_backend.entity.PaymentStatus;
import com.elitebnb_backend.service.AdminPaymentService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @GetMapping
    public ResponseEntity<List<AdminPaymentResponse>> getPayments(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            PaymentStatus status,

            @RequestParam(required = false)
            PaymentProvider provider,

            @RequestParam(required = false)
            Long bookingId,

            @RequestParam(required = false)
            Long guestId,

            @RequestParam(required = false)
            Long propertyId,

            @RequestParam(required = false)
            Long hostId,

            @RequestParam(required = false)
            LocalDate from,

            @RequestParam(required = false)
            LocalDate to
    ) {

        return ResponseEntity.ok(
                adminPaymentService.getPayments(
                        search,
                        status,
                        provider,
                        bookingId,
                        guestId,
                        propertyId,
                        hostId,
                        from,
                        to
                )
        );
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<AdminPaymentResponse> getPayment(
            @PathVariable Long paymentId
    ) {

        return ResponseEntity.ok(
                adminPaymentService.getPayment(
                        paymentId
                )
        );
    }

    @PatchMapping("/{paymentId}/status")
    public ResponseEntity<AdminPaymentResponse> updateStatus(
            @PathVariable Long paymentId,
            @RequestBody UpdatePaymentStatusRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                adminPaymentService.updateStatus(
                        paymentId,
                        request.getStatus(),
                        authentication.getName()
                )
        );
    }
}
