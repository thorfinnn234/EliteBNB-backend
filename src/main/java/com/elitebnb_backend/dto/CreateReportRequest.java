package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.ReportReason;
import com.elitebnb_backend.entity.ReportTargetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportRequest {

    private ReportTargetType targetType;
    private Long targetId;
    private ReportReason reason;
    private String description;
}
