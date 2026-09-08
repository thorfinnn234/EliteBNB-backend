package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AnalyticsDataPointResponse {

    private String label;
    private BigDecimal value;
}
