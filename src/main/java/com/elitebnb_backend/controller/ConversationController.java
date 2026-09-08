package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.ConversationDetailResponse;
import com.elitebnb_backend.dto.ConversationResponse;
import com.elitebnb_backend.dto.CreateConversationRequest;
import com.elitebnb_backend.dto.MessageResponse;
import com.elitebnb_backend.dto.SendMessageRequest;
import com.elitebnb_backend.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * Creates or reuses a conversation for the authenticated guest and the
     * actual host who owns the supplied property.
     */
    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @RequestBody CreateConversationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                conversationService.createConversation(
                        request,
                        authentication
                )
        );
    }

    /**
     * Lists the authenticated user's guest or host inbox, sorted by latest
     * conversation activity.
     */
    @GetMapping
    public ResponseEntity<List<ConversationResponse>>
    getMyConversations(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                conversationService.getMyConversations(
                        authentication
                )
        );
    }

    /**
     * Returns a participant-authorized conversation with its message history.
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDetailResponse> getConversation(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                conversationService.getConversation(
                        conversationId,
                        authentication
                )
        );
    }

    /**
     * Sends a message as the authenticated participant; sender identity is not
     * accepted from the request body.
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                conversationService.sendMessage(
                        conversationId,
                        request,
                        authentication
                )
        );
    }

    /**
     * Marks all unread messages from the other participant as read for the
     * authenticated viewer.
     */
    @PatchMapping("/{conversationId}/read")
    public ResponseEntity<ConversationResponse> markConversationRead(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                conversationService.markConversationRead(
                        conversationId,
                        authentication
                )
        );
    }
}
