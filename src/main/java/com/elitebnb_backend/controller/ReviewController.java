package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.CreateReviewRequest;
import com.elitebnb_backend.dto.HostReviewResponseRequest;
import com.elitebnb_backend.dto.ReviewResponse;
import com.elitebnb_backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @RequestBody CreateReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                reviewService.createReview(
                        request,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/api/reviews/my")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                reviewService.getMyReviews(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/api/properties/{propertyId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getPropertyReviews(
            @PathVariable Long propertyId
    ) {
        return ResponseEntity.ok(
                reviewService.getPropertyReviews(propertyId)
        );
    }

    @GetMapping("/api/host/reviews")
    public ResponseEntity<List<ReviewResponse>> getHostReviews(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                reviewService.getHostReviews(
                        authentication.getName()
                )
        );
    }

    @PutMapping("/api/reviews/{reviewId}/response")
    public ResponseEntity<ReviewResponse> respondToReview(
            @PathVariable Long reviewId,
            @RequestBody HostReviewResponseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                reviewService.respondToReview(
                        reviewId,
                        request,
                        authentication.getName()
                )
        );
    }
}