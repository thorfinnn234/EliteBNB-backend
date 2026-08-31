package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.HostEarningsResponse;
import com.elitebnb_backend.service.EarningsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host")
public class EarningsController {

    private final EarningsService earningsService;

    public EarningsController(
            EarningsService earningsService
    ) {
        this.earningsService = earningsService;
    }

    @GetMapping("/earnings")
    public ResponseEntity<HostEarningsResponse> getEarnings(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                earningsService.getHostEarnings(authentication)
        );
    }
}