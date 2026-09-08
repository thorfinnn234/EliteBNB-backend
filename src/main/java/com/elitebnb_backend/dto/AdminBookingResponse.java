package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminBookingResponse {

    private Long id;

    private Long propertyId;
    private String propertyTitle;
    private String propertyLocation;

    private Long hostId;
    private String hostName;
    private String hostEmail;

    private Long guestId;
    private String guestName;
    private String guestEmail;

    private LocalDate checkIn;
    private LocalDate checkOut;

    private Integer guests;
    private Double totalAmount;

    private BookingStatus status;

    private Long paymentId;
    private PaymentStatus paymentStatus;
    private BigDecimal paymentAmount;
    private String paymentCurrency;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
