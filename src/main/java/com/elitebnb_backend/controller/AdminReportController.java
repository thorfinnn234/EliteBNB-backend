package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.ModerationActionRequest;
import com.elitebnb_backend.dto.ModerationReportResponse;
import com.elitebnb_backend.dto.UpdateReportStatusRequest;
import com.elitebnb_backend.entity.ReportReason;
import com.elitebnb_backend.entity.ReportStatus;
import com.elitebnb_backend.entity.ReportTargetType;
import com.elitebnb_backend.service.ModerationReportService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ModerationReportService moderationReportService;

    @GetMapping
    public ResponseEntity<List<ModerationReportResponse>> getReports(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            ReportStatus status,

            @RequestParam(required = false)
            ReportTargetType targetType,

            @RequestParam(required = false)
            ReportReason reason,

            @RequestParam(required = false)
            Long reporterId
    ) {

        return ResponseEntity.ok(
                moderationReportService.getReports(
                        search,
                        status,
                        targetType,
                        reason,
                        reporterId
                )
        );
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ModerationReportResponse> getReport(
            @PathVariable Long reportId
    ) {

        return ResponseEntity.ok(
                moderationReportService.getReport(
                        reportId
                )
        );
    }

    @PatchMapping("/{reportId}/status")
    public ResponseEntity<ModerationReportResponse> updateStatus(
            @PathVariable Long reportId,
            @RequestBody UpdateReportStatusRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                moderationReportService.updateStatus(
                        reportId,
                        request,
                        authentication.getName()
                )
        );
    }

    @PostMapping("/{reportId}/moderate")
    public ResponseEntity<ModerationReportResponse> moderate(
            @PathVariable Long reportId,
            @RequestBody ModerationActionRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                moderationReportService.moderate(
                        reportId,
                        request,
                        authentication.getName()
                )
        );
    }
}
