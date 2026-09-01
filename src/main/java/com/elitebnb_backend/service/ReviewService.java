package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.CreateReviewRequest;
import com.elitebnb_backend.dto.HostReviewResponseRequest;
import com.elitebnb_backend.dto.ReviewResponse;
import com.elitebnb_backend.entity.*;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.ReviewRepository;
import com.elitebnb_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public ReviewResponse createReview(
            CreateReviewRequest request,
            String userEmail
    ) {
        User user = getUser(userEmail);

        if (request.getBookingId() == null) {
            throw new RuntimeException("Booking ID is required");
        }

        if (request.getRating() == null ||
                request.getRating() < 1 ||
                request.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        if (request.getComment() == null ||
                request.getComment().trim().isEmpty()) {
            throw new RuntimeException("Review comment is required");
        }

        Booking booking = bookingRepository
                .findById(request.getBookingId())
                .orElseThrow(() ->
                        new RuntimeException("Booking not found")
                );

        if (!booking.getGuest().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You can only review your own bookings"
            );
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException(
                    "You can only review a completed stay"
            );
        }

        if (reviewRepository.existsByBooking(booking)) {
            throw new RuntimeException(
                    "This booking has already been reviewed"
            );
        }

        Review review = Review.builder()
                .booking(booking)
                .property(booking.getProperty())
                .guest(user)
                .rating(request.getRating())
                .comment(request.getComment().trim())
                .build();

        return mapToResponse(
                reviewRepository.save(review)
        );
    }

    public List<ReviewResponse> getMyReviews(
            String userEmail
    ) {
        User user = getUser(userEmail);

        return reviewRepository
                .findByGuestOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReviewResponse> getPropertyReviews(
            Long propertyId
    ) {
        return reviewRepository.findAll()
                .stream()
                .filter(review ->
                        review.getProperty()
                                .getId()
                                .equals(propertyId)
                )
                .sorted(
                        (a, b) ->
                                b.getCreatedAt()
                                        .compareTo(
                                                a.getCreatedAt()
                                        )
                )
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReviewResponse> getHostReviews(
            String userEmail
    ) {
        User host = getUser(userEmail);

        return reviewRepository
                .findByPropertyHostOrderByCreatedAtDesc(host)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ReviewResponse respondToReview(
            Long reviewId,
            HostReviewResponseRequest request,
            String userEmail
    ) {
        User host = getUser(userEmail);

        Review review = reviewRepository
                .findById(reviewId)
                .orElseThrow(() ->
                        new RuntimeException("Review not found")
                );

        if (!review.getProperty()
                .getHost()
                .getId()
                .equals(host.getId())) {
            throw new RuntimeException(
                    "You can only respond to reviews on your own properties"
            );
        }

        if (request.getResponse() == null ||
                request.getResponse().trim().isEmpty()) {
            throw new RuntimeException(
                    "Response cannot be empty"
            );
        }

        review.setHostResponse(
                request.getResponse().trim()
        );

        review.setRespondedAt(
                LocalDateTime.now()
        );

        return mapToResponse(
                reviewRepository.save(review)
        );
    }

    private User getUser(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );
    }

    private ReviewResponse mapToResponse(
            Review review
    ) {
        User guest = review.getGuest();
        Property property = review.getProperty();

        return new ReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                property.getId(),
                property.getTitle(),
                guest.getId(),
                guest.getFirstName() + " " + guest.getLastName(),
                guest.getProfileImageUrl(),
                review.getRating(),
                review.getComment(),
                review.getHostResponse(),
                review.getCreatedAt(),
                review.getRespondedAt()
        );
    }
}