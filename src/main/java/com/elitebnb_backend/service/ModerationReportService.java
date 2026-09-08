package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.CreateReportRequest;
import com.elitebnb_backend.dto.ModerationActionRequest;
import com.elitebnb_backend.dto.ModerationReportResponse;
import com.elitebnb_backend.dto.UpdateReportStatusRequest;
import com.elitebnb_backend.entity.*;
import com.elitebnb_backend.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModerationReportService {

    private final ModerationReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;
    private final AdminNotificationService adminNotificationService;

    public ModerationReportResponse createReport(
            CreateReportRequest request,
            String reporterEmail
    ) {

        User reporter =
                getUserByEmail(reporterEmail);

        validateCreateRequest(request);

        ModerationReport report =
                ModerationReport.builder()
                        .reporter(reporter)
                        .targetType(request.getTargetType())
                        .targetId(request.getTargetId())
                        .reason(request.getReason())
                        .description(request.getDescription().trim())
                        .status(ReportStatus.OPEN)
                        .build();

        attachTarget(report);
        validateReporterAccess(report, reporter);

        ModerationReport saved =
                reportRepository.save(report);

        adminNotificationService.notifyAdmins(
                "New moderation report",
                reporter.getFirstName()
                        + " "
                        + reporter.getLastName()
                        + " submitted a "
                        + saved.getReason().name()
                        + " report.",
                AdminNotificationType.NEW_REPORT,
                saved.getId(),
                "REPORT"
        );

        return map(saved);
    }

    public List<ModerationReportResponse> getMyReports(
            String reporterEmail
    ) {

        User reporter =
                getUserByEmail(reporterEmail);

        return reportRepository
                .findByReporterOrderByCreatedAtDesc(reporter)
                .stream()
                .map(this::map)
                .toList();
    }

    public List<ModerationReportResponse> getReports(
            String search,
            ReportStatus status,
            ReportTargetType targetType,
            ReportReason reason,
            Long reporterId
    ) {

        return reportRepository
                .searchAdminReports(
                        search,
                        status,
                        targetType,
                        reason,
                        reporterId
                )
                .stream()
                .map(this::map)
                .toList();
    }

    public ModerationReportResponse getReport(
            Long reportId
    ) {

        return map(
                getReportEntity(reportId)
        );
    }

    public ModerationReportResponse updateStatus(
            Long reportId,
            UpdateReportStatusRequest request,
            String adminEmail
    ) {

        if (request.getStatus() == null) {
            throw new RuntimeException(
                    "Report status is required"
            );
        }

        ModerationReport report =
                getReportEntity(reportId);

        report.setStatus(
                request.getStatus()
        );

        report.setAdminNote(
                normalizeNote(request.getAdminNote())
        );

        updateResolvedAt(report);

        ModerationReport saved =
                reportRepository.save(report);

        auditLogService.record(
                adminEmail,
                AuditActionType.REPORT_STATUS_UPDATED,
                AuditTargetType.REPORT,
                saved.getId(),
                "Updated report #"
                        + saved.getId()
                        + " status to "
                        + saved.getStatus().name()
        );

        return map(saved);
    }

    public ModerationReportResponse moderate(
            Long reportId,
            ModerationActionRequest request,
            String adminEmail
    ) {

        if (request.getAction() == null) {
            throw new RuntimeException(
                    "Moderation action is required"
            );
        }

        ModerationReport report =
                getReportEntity(reportId);

        switch (request.getAction()) {

            case SUSPEND_USER ->
                    moderateUser(
                            report,
                            AccountStatus.SUSPENDED
                    );

            case REACTIVATE_USER ->
                    moderateUser(
                            report,
                            AccountStatus.ACTIVE
                    );

            case SUSPEND_PROPERTY ->
                    moderateProperty(
                            report,
                            PropertyStatus.SUSPENDED
                    );

            case REACTIVATE_PROPERTY ->
                    moderateProperty(
                            report,
                            PropertyStatus.ACTIVE
                    );

            case HIDE_REVIEW ->
                    moderateReview(
                            report,
                            ReviewStatus.HIDDEN
                    );

            case RESTORE_REVIEW ->
                    moderateReview(
                            report,
                            ReviewStatus.VISIBLE
                    );

            case DISMISS_REPORT ->
                    report.setStatus(ReportStatus.DISMISSED);
        }

        report.setAdminNote(
                normalizeNote(request.getAdminNote())
        );

        if (report.getStatus() != ReportStatus.DISMISSED) {
            report.setStatus(ReportStatus.RESOLVED);
        }

        updateResolvedAt(report);

        ModerationReport saved =
                reportRepository.save(report);

        auditLogService.record(
                adminEmail,
                AuditActionType.REPORT_MODERATION_ACTION,
                AuditTargetType.REPORT,
                saved.getId(),
                "Applied moderation action "
                        + request.getAction().name()
                        + " to report #"
                        + saved.getId()
        );

        return map(saved);
    }

    private void validateCreateRequest(
            CreateReportRequest request
    ) {

        if (request.getTargetType() == null) {
            throw new RuntimeException(
                    "Report target type is required"
            );
        }

        if (request.getTargetId() == null) {
            throw new RuntimeException(
                    "Report target ID is required"
            );
        }

        if (request.getReason() == null) {
            throw new RuntimeException(
                    "Report reason is required"
            );
        }

        if (request.getDescription() == null
                || request.getDescription().trim().isEmpty()) {
            throw new RuntimeException(
                    "Report description is required"
            );
        }
    }

    private void attachTarget(
            ModerationReport report
    ) {

        switch (report.getTargetType()) {

            case USER ->
                    report.setReportedUser(
                            userRepository
                                    .findById(report.getTargetId())
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "Reported user not found"
                                            )
                                    )
                    );

            case PROPERTY ->
                    report.setReportedProperty(
                            propertyRepository
                                    .findById(report.getTargetId())
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "Reported property not found"
                                            )
                                    )
                    );

            case REVIEW ->
                    report.setReportedReview(
                            reviewRepository
                                    .findById(report.getTargetId())
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "Reported review not found"
                                            )
                                    )
                    );

            case BOOKING ->
                    report.setReportedBooking(
                            bookingRepository
                                    .findById(report.getTargetId())
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "Reported booking not found"
                                            )
                                    )
                    );
        }
    }

    private void validateReporterAccess(
            ModerationReport report,
            User reporter
    ) {

        if (report.getReportedUser() != null
                && report.getReportedUser()
                .getId()
                .equals(reporter.getId())) {

            throw new RuntimeException(
                    "You cannot report your own account"
            );
        }

        if (report.getReportedBooking() == null) {
            return;
        }

        Booking booking =
                report.getReportedBooking();

        boolean reporterIsGuest =
                booking.getGuest()
                        .getId()
                        .equals(reporter.getId());

        boolean reporterIsHost =
                booking.getProperty()
                        .getHost()
                        .getId()
                        .equals(reporter.getId());

        if (!reporterIsGuest
                && !reporterIsHost) {

            throw new RuntimeException(
                    "You can only report bookings you are part of"
            );
        }
    }

    private void moderateUser(
            ModerationReport report,
            AccountStatus status
    ) {

        User user =
                resolveModerationUser(report);

        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException(
                    "Admin accounts cannot be moderated here"
            );
        }

        user.setAccountStatus(status);

        userRepository.save(user);
    }

    private void moderateProperty(
            ModerationReport report,
            PropertyStatus status
    ) {

        Property property =
                resolveModerationProperty(report);

        property.setStatus(status);

        propertyRepository.save(property);
    }

    private void moderateReview(
            ModerationReport report,
            ReviewStatus status
    ) {

        Review review =
                resolveModerationReview(report);

        review.setStatus(status);

        reviewRepository.save(review);
    }

    private User resolveModerationUser(
            ModerationReport report
    ) {

        if (report.getReportedUser() != null) {
            return report.getReportedUser();
        }

        if (report.getReportedProperty() != null) {
            return report.getReportedProperty().getHost();
        }

        if (report.getReportedReview() != null) {
            return report.getReportedReview().getGuest();
        }

        if (report.getReportedBooking() != null) {
            return report.getReportedBooking().getGuest();
        }

        throw new RuntimeException(
                "No user is attached to this report"
        );
    }

    private Property resolveModerationProperty(
            ModerationReport report
    ) {

        if (report.getReportedProperty() != null) {
            return report.getReportedProperty();
        }

        if (report.getReportedReview() != null) {
            return report.getReportedReview().getProperty();
        }

        if (report.getReportedBooking() != null) {
            return report.getReportedBooking().getProperty();
        }

        throw new RuntimeException(
                "No property is attached to this report"
        );
    }

    private Review resolveModerationReview(
            ModerationReport report
    ) {

        if (report.getReportedReview() != null) {
            return report.getReportedReview();
        }

        throw new RuntimeException(
                "No review is attached to this report"
        );
    }

    private ModerationReport getReportEntity(
            Long reportId
    ) {

        return reportRepository
                .findById(reportId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Report not found"
                        )
                );
    }

    private User getUserByEmail(
            String email
    ) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }

    private String normalizeNote(
            String note
    ) {

        if (note == null
                || note.trim().isEmpty()) {
            return null;
        }

        return note.trim();
    }

    private void updateResolvedAt(
            ModerationReport report
    ) {

        if (report.getStatus() == ReportStatus.RESOLVED
                || report.getStatus() == ReportStatus.DISMISSED) {

            report.setResolvedAt(
                    LocalDateTime.now()
            );

            return;
        }

        report.setResolvedAt(null);
    }

    private ModerationReportResponse map(
            ModerationReport report
    ) {

        String reporterName =
                report.getReporter().getFirstName()
                        + " "
                        + report.getReporter().getLastName();

        User reportedUser =
                report.getReportedUser();

        Property reportedProperty =
                report.getReportedProperty();

        Review reportedReview =
                report.getReportedReview();

        Booking reportedBooking =
                report.getReportedBooking();

        String reportedUserName =
                reportedUser != null
                        ? reportedUser.getFirstName()
                        + " "
                        + reportedUser.getLastName()
                        : null;

        return new ModerationReportResponse(
                report.getId(),

                report.getReporter().getId(),
                reporterName,
                report.getReporter().getEmail(),

                report.getTargetType(),
                report.getTargetId(),
                targetSummary(report),

                reportedUser != null
                        ? reportedUser.getId()
                        : null,
                reportedUserName,
                reportedUser != null
                        ? reportedUser.getEmail()
                        : null,

                reportedProperty != null
                        ? reportedProperty.getId()
                        : null,
                reportedProperty != null
                        ? reportedProperty.getTitle()
                        : null,

                reportedReview != null
                        ? reportedReview.getId()
                        : null,
                reportedReview != null
                        ? reportedReview.getComment()
                        : null,

                reportedBooking != null
                        ? reportedBooking.getId()
                        : null,

                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getAdminNote(),

                report.getResolvedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    private String targetSummary(
            ModerationReport report
    ) {

        if (report.getReportedUser() != null) {
            return report.getReportedUser().getFirstName()
                    + " "
                    + report.getReportedUser().getLastName();
        }

        if (report.getReportedProperty() != null) {
            return report.getReportedProperty().getTitle();
        }

        if (report.getReportedReview() != null) {
            return report.getReportedReview().getComment();
        }

        if (report.getReportedBooking() != null) {
            return "Booking #"
                    + report.getReportedBooking().getId();
        }

        return null;
    }
}
