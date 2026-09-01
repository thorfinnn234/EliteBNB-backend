package com.elitebnb_backend.repository;

import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.Review;
import com.elitebnb_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBooking(Booking booking);

    Optional<Review> findByBooking(Booking booking);

    List<Review> findByPropertyOrderByCreatedAtDesc(Property property);

    List<Review> findByGuestOrderByCreatedAtDesc(User guest);

    List<Review> findByPropertyHostOrderByCreatedAtDesc(User host);
}