package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.BookingStatus;
import com.elitebnb_backend.entity.PaymentProvider;
import com.elitebnb_backend.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminPaymentResponse {

    private Long id;
    private Long bookingId;

    private BigDecimal amount;
    private String currency;
    private String reference;

    private PaymentStatus status;
    private PaymentProvider provider;

    private Long guestId;
    private String guestName;
    private String guestEmail;

    private Long propertyId;
    private String propertyTitle;
    private String propertyLocation;

    private Long hostId;
    private String hostName;
    private String hostEmail;

    private LocalDate checkIn;
    private LocalDate checkOut;
    private BookingStatus bookingStatus;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
