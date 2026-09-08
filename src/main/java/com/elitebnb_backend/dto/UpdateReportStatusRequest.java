package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.ReportStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReportStatusRequest {

    private ReportStatus status;
    private String adminNote;
}
