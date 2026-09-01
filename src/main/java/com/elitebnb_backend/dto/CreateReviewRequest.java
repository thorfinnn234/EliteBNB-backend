package com.elitebnb_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewRequest {

    private Long bookingId;

    private Integer rating;

    private String comment;
}