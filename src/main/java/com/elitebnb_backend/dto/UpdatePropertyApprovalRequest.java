package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.PropertyApprovalStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePropertyApprovalRequest {

    private PropertyApprovalStatus approvalStatus;
    private String approvalNote;
}
