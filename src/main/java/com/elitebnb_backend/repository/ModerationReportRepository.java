package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.ModerationReport;
import com.elitebnb_backend.entity.ReportReason;
import com.elitebnb_backend.entity.ReportStatus;
import com.elitebnb_backend.entity.ReportTargetType;
import com.elitebnb_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ModerationReportRepository
        extends JpaRepository<ModerationReport, Long> {

    List<ModerationReport> findByReporterOrderByCreatedAtDesc(
            User reporter
    );

    long countByStatus(
            ReportStatus status
    );

    @Query("""
            SELECT r
            FROM ModerationReport r
            LEFT JOIN r.reportedUser reportedUser
            LEFT JOIN r.reportedProperty reportedProperty
            LEFT JOIN r.reportedReview reportedReview
            WHERE (
                :search IS NULL
                OR :search = ''
                OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.adminNote) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.reporter.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.reporter.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.reporter.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(reportedUser.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(reportedUser.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(reportedUser.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(reportedProperty.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(reportedProperty.location) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(reportedReview.comment) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:status IS NULL OR r.status = :status)
            AND (:targetType IS NULL OR r.targetType = :targetType)
            AND (:reason IS NULL OR r.reason = :reason)
            AND (:reporterId IS NULL OR r.reporter.id = :reporterId)
            ORDER BY r.id DESC
            """)
    List<ModerationReport> searchAdminReports(
            @Param("search") String search,
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTargetType targetType,
            @Param("reason") ReportReason reason,
            @Param("reporterId") Long reporterId
    );
}
