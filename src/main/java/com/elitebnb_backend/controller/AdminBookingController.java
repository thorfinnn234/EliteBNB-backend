package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.AdminBookingResponse;
import com.elitebnb_backend.dto.UpdateBookingStatusRequest;
import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.service.AdminBookingService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    @GetMapping
    public ResponseEntity<List<AdminBookingResponse>> getBookings(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            BookingStatus status,

            @RequestParam(required = false)
            Long propertyId,

            @RequestParam(required = false)
            Long guestId,

            @RequestParam(required = false)
            Long hostId,

            @RequestParam(required = false)
            LocalDate from,

            @RequestParam(required = false)
            LocalDate to
    ) {

        return ResponseEntity.ok(
                adminBookingService.getBookings(
                        search,
                        status,
                        propertyId,
                        guestId,
                        hostId,
                        from,
                        to
                )
        );
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<AdminBookingResponse> getBooking(
            @PathVariable Long bookingId
    ) {

        return ResponseEntity.ok(
                adminBookingService.getBooking(
                        bookingId
                )
        );
    }

    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<AdminBookingResponse> updateStatus(
            @PathVariable Long bookingId,
            @RequestBody UpdateBookingStatusRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                adminBookingService.updateStatus(
                        bookingId,
                        request.getStatus(),
                        authentication.getName()
                )
        );
    }
}
