package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BookingResponse {

    private Long id;

    private Long propertyId;
    private String propertyTitle;

    private Long guestId;
    private String guestName;

    private LocalDate checkIn;
    private LocalDate checkOut;

    private Integer guests;

    private Double totalAmount;

    private BookingStatus status;

    private LocalDateTime createdAt;
}