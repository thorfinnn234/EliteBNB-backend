package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.CreateReportRequest;
import com.elitebnb_backend.dto.ModerationReportResponse;
import com.elitebnb_backend.service.ModerationReportService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ModerationReportService moderationReportService;

    @PostMapping
    public ResponseEntity<ModerationReportResponse> createReport(
            @RequestBody CreateReportRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                moderationReportService.createReport(
                        request,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/my")
    public ResponseEntity<List<ModerationReportResponse>> getMyReports(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                moderationReportService.getMyReports(
                        authentication.getName()
                )
        );
    }
}
