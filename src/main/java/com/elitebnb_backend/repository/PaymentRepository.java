package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.Payment;
import com.elitebnb_backend.entity.PaymentProvider;
import com.elitebnb_backend.entity.PaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReference(
            String reference
    );

    Optional<Payment> findByBooking(
            Booking booking
    );

    boolean existsByBooking(
            Booking booking
    );

    long countByStatus(
            PaymentStatus status
    );

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = com.elitebnb_backend.entity.PaymentStatus.SUCCESS
            """)
    BigDecimal sumSuccessfulPayments();

    @Query("""
            SELECT p
            FROM Payment p
            WHERE (
                :search IS NULL
                OR :search = ''
                OR LOWER(p.reference) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.currency) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.user.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.user.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(CONCAT(CONCAT(p.user.firstName, ' '), p.user.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.user.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.booking.property.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.booking.property.location) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.booking.property.host.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.booking.property.host.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(CONCAT(CONCAT(p.booking.property.host.firstName, ' '), p.booking.property.host.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.booking.property.host.email) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:status IS NULL OR p.status = :status)
            AND (:provider IS NULL OR p.provider = :provider)
            AND (:bookingId IS NULL OR p.booking.id = :bookingId)
            AND (:guestId IS NULL OR p.user.id = :guestId)
            AND (:propertyId IS NULL OR p.booking.property.id = :propertyId)
            AND (:hostId IS NULL OR p.booking.property.host.id = :hostId)
            ORDER BY p.id DESC
            """)
    List<Payment> searchAdminPayments(
            @Param("search") String search,
            @Param("status") PaymentStatus status,
            @Param("provider") PaymentProvider provider,
            @Param("bookingId") Long bookingId,
            @Param("guestId") Long guestId,
            @Param("propertyId") Long propertyId,
            @Param("hostId") Long hostId
    );

    @Query("""
            SELECT FUNCTION('to_char', p.createdAt, 'YYYY-MM'), COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = com.elitebnb_backend.entity.PaymentStatus.SUCCESS
            AND p.createdAt >= :start
            GROUP BY FUNCTION('to_char', p.createdAt, 'YYYY-MM')
            ORDER BY FUNCTION('to_char', p.createdAt, 'YYYY-MM')
            """)
    List<Object[]> sumRevenueByMonth(
            @Param("start") LocalDateTime start
    );
}
