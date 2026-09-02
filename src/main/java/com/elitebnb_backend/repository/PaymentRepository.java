package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.Payment;
import com.elitebnb_backend.entity.PaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
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
}