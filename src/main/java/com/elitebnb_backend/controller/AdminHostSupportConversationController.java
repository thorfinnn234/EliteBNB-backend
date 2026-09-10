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

import java.util.List;

@RestController
@RequestMapping("/api/admin/host-support-conversations")
@RequiredArgsConstructor
public class AdminHostSupportConversationController {

    private final HostSupportConversationService hostSupportConversationService;

    /**
     * Lists Host support conversations for the Admin inbox.
     */
    @GetMapping
    public ResponseEntity<List<HostSupportConversationResponse>>
    getSupportConversations(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostSupportConversationService
                        .getAdminConversations(authentication)
        );
    }

    /**
     * Creates or returns the single support thread for a specific Host so Admin
     * can initiate verification-support communication before the Host writes
     * first.
     */
    @PostMapping("/host/{hostId}")
    public ResponseEntity<HostSupportConversationDetailResponse>
    createOrGetSupportConversationForHost(
            @PathVariable Long hostId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostSupportConversationService
                        .createOrGetAdminConversationForHost(
                                hostId,
                                authentication
                        )
        );
    }

    /**
     * Opens one Host support thread for Admin review.
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<HostSupportConversationDetailResponse>
    getSupportConversation(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostSupportConversationService
                        .getAdminConversation(
                                conversationId,
                                authentication
                        )
        );
    }

    /**
     * Sends a support reply as the authenticated Admin.
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<HostSupportMessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostSupportConversationService
                        .sendAdminMessage(
                                conversationId,
                                request,
                                authentication
                        )
        );
    }

    /**
     * Marks unread Host messages in this support thread as read for Admin.
     */
    @PatchMapping("/{conversationId}/read")
    public ResponseEntity<HostSupportConversationResponse>
    markConversationRead(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                hostSupportConversationService
                        .markAdminConversationRead(
                                conversationId,
                                authentication
                        )
        );
    }
}
