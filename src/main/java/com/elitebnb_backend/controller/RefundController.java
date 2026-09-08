package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.CreateRefundRequest;
import com.elitebnb_backend.dto.RefundResponse;
import com.elitebnb_backend.service.RefundService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<RefundResponse> requestRefund(
            @RequestBody CreateRefundRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                refundService.requestRefund(
                        request,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/my")
    public ResponseEntity<List<RefundResponse>> getMyRefunds(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                refundService.getMyRefunds(
                        authentication.getName()
                )
        );
    }
}
