package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.ReportReason;
import com.elitebnb_backend.entity.ReportStatus;
import com.elitebnb_backend.entity.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ModerationReportResponse {

    private Long id;

    private Long reporterId;
    private String reporterName;
    private String reporterEmail;

    private ReportTargetType targetType;
    private Long targetId;
    private String targetSummary;

    private Long reportedUserId;
    private String reportedUserName;
    private String reportedUserEmail;

    private Long reportedPropertyId;
    private String reportedPropertyTitle;

    private Long reportedReviewId;
    private String reportedReviewComment;

    private Long reportedBookingId;

    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private String adminNote;

    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
