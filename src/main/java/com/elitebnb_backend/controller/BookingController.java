package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.BookingResponse;
import com.elitebnb_backend.dto.CreateBookingRequest;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.service.BookingService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(
            BookingService bookingService
    ) {
        this.bookingService = bookingService;
    }

    // USER: CREATE A BOOKING
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody CreateBookingRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                bookingService.createBooking(
                        request,
                        authentication
                )
        );
    }

    // USER: GET MY BOOKINGS
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                bookingService.getMyBookings(
                        authentication
                )
        );
    }

    // HOST: GET BOOKINGS FOR MY PROPERTIES
    @GetMapping("/host")
    public ResponseEntity<List<BookingResponse>> getHostBookings(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                bookingService.getHostBookings(
                        authentication
                )
        );
    }

    // HOST: UPDATE BOOKING STATUS
    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestParam BookingStatus status,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                bookingService.updateBookingStatus(
                        bookingId,
                        status,
                        authentication
                )
        );
    }
}