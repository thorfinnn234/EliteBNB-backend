package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.HostVerification;
import com.elitebnb_backend.entity.HostVerificationStatus;
import com.elitebnb_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HostVerificationRepository
        extends JpaRepository<HostVerification, Long> {

    Optional<HostVerification> findByHost(User host);

    boolean existsByHost(User host);

    long countByStatus(
            HostVerificationStatus status
    );

    List<HostVerification> findByStatusOrderByCreatedAtDesc(
            HostVerificationStatus status
    );

    @Query("""
            SELECT v
            FROM HostVerification v
            WHERE (
                :search IS NULL
                OR :search = ''
                OR LOWER(v.legalName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(v.businessName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(v.documentType) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(v.host.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(v.host.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(v.host.email) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:status IS NULL OR v.status = :status)
            AND (:hostId IS NULL OR v.host.id = :hostId)
            ORDER BY v.id DESC
            """)
    List<HostVerification> searchAdminHostVerifications(
            @Param("search") String search,
            @Param("status") HostVerificationStatus status,
            @Param("hostId") Long hostId
    );
}
