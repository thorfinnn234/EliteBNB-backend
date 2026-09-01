package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

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
}