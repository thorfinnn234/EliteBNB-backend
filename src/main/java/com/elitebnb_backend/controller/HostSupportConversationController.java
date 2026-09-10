package com.elitebnb_backend.controller;

import com.elitebnb_backend.dto.HostSupportConversationDetailResponse;
import com.elitebnb_backend.dto.HostSupportConversationResponse;
import com.elitebnb_backend.dto.HostSupportMessageResponse;
import com.elitebnb_backend.dto.SendMessageRequest;
import com.elitebnb_backend.service.HostSupportConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host/support-conversation")
@RequiredArgsConstructor
public class HostSupportConversationController {

    private final HostSupportConversationService hostSupportConversationService;

    /**
     * Returns or creates the authenticated Host's single Admin support thread.
     */
    @GetMapping
    public ResponseEntity<HostSupportConversationDetailResponse>
    getSupportConversation(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostSupportConversationService
                        .getHostConversation(authentication)
        );
    }

    /**
     * Sends a support message as the authenticated Host. The service derives
     * sender and conversation ownership from authentication.
     */
    @PostMapping("/messages")
    public ResponseEntity<HostSupportMessageResponse> sendMessage(
            @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostSupportConversationService
                        .sendHostMessage(
                                request,
                                authentication
                        )
        );
    }

    /**
     * Marks unread Admin messages in this Host's support thread as read.
     */
    @PatchMapping("/read")
    public ResponseEntity<HostSupportConversationResponse>
    markConversationRead(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostSupportConversationService
                        .markHostConversationRead(authentication)
        );
    }
}
