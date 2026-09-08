package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Payment;
import com.elitebnb_backend.entity.Refund;
import com.elitebnb_backend.entity.RefundStatus;
import com.elitebnb_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RefundRepository
        extends JpaRepository<Refund, Long> {

    boolean existsByPaymentAndStatusIn(
            Payment payment,
            List<RefundStatus> statuses
    );

    List<Refund> findByRequestedByOrderByCreatedAtDesc(
            User requestedBy
    );

    long countByStatus(
            RefundStatus status
    );

    @Query("""
            SELECT r
            FROM Refund r
            WHERE (
                :search IS NULL
                OR :search = ''
                OR LOWER(r.reason) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.adminNote) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.payment.reference) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.requestedBy.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.requestedBy.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.requestedBy.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.booking.property.title) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:status IS NULL OR r.status = :status)
            AND (:bookingId IS NULL OR r.booking.id = :bookingId)
            AND (:paymentId IS NULL OR r.payment.id = :paymentId)
            AND (:requestedById IS NULL OR r.requestedBy.id = :requestedById)
            ORDER BY r.id DESC
            """)
    List<Refund> searchAdminRefunds(
            @Param("search") String search,
            @Param("status") RefundStatus status,
            @Param("bookingId") Long bookingId,
            @Param("paymentId") Long paymentId,
            @Param("requestedById") Long requestedById
    );
}
