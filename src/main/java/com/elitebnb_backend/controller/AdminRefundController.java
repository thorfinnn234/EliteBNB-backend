package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.RefundResponse;
import com.elitebnb_backend.dto.UpdateRefundStatusRequest;
import com.elitebnb_backend.entity.RefundStatus;
import com.elitebnb_backend.service.RefundService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundService refundService;

    @GetMapping
    public ResponseEntity<List<RefundResponse>> getRefunds(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            RefundStatus status,

            @RequestParam(required = false)
            Long bookingId,

            @RequestParam(required = false)
            Long paymentId,

            @RequestParam(required = false)
            Long requestedById
    ) {

        return ResponseEntity.ok(
                refundService.getRefunds(
                        search,
                        status,
                        bookingId,
                        paymentId,
                        requestedById
                )
        );
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponse> getRefund(
            @PathVariable Long refundId
    ) {

        return ResponseEntity.ok(
                refundService.getRefund(refundId)
        );
    }

    @PatchMapping("/{refundId}/status")
    public ResponseEntity<RefundResponse> updateStatus(
            @PathVariable Long refundId,
            @RequestBody UpdateRefundStatusRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                refundService.updateStatus(
                        refundId,
                        request,
                        authentication.getName()
                )
        );
    }
}
