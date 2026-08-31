package com.elitebnb_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateBookingRequest {

    private Long propertyId;

    private LocalDate checkIn;

    private LocalDate checkOut;

    private Integer guests;
}