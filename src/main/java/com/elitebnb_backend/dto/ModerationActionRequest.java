package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.ModerationActionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerationActionRequest {

    private ModerationActionType action;
    private String adminNote;
}
