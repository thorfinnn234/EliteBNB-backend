package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.BookingStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBookingStatusRequest {

    private BookingStatus status;
}
