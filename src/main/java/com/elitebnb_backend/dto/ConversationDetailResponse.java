package com.elitebnb_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ConversationDetailResponse {

    private ConversationResponse conversation;

    private List<MessageResponse> messages;
}
