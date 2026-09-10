package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.HostSupportConversationDetailResponse;
import com.elitebnb_backend.dto.HostSupportConversationResponse;
import com.elitebnb_backend.dto.HostSupportMessageResponse;
import com.elitebnb_backend.dto.SendMessageRequest;
import com.elitebnb_backend.entity.AccountStatus;
import com.elitebnb_backend.entity.HostSupportConversation;
import com.elitebnb_backend.entity.HostSupportMessage;
import com.elitebnb_backend.entity.HostVerification;
import com.elitebnb_backend.entity.HostVerificationStatus;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.HostSupportConversationRepository;
import com.elitebnb_backend.repository.HostSupportMessageRepository;
import com.elitebnb_backend.repository.HostVerificationRepository;
import com.elitebnb_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HostSupportConversationService {

    private static final int MAX_MESSAGE_BODY_LENGTH = 2000;
    private static final int MESSAGE_PREVIEW_LENGTH = 80;

    private final HostSupportConversationRepository conversationRepository;
    private final HostSupportMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final HostVerificationRepository hostVerificationRepository;

    /**
     * Returns the authenticated Host's verification support thread, creating it
     * on first access. This endpoint deliberately checks only active Host
     * identity, not VERIFIED business access, because it is part of the
     * verification journey itself.
     */
    @Transactional
    public HostSupportConversationDetailResponse getHostConversation(
            Authentication authentication
    ) {
        User host = getActiveHost(authentication);
        HostSupportConversation conversation =
                findOrCreateConversation(host);

        return mapToDetailResponse(
                conversation,
                Role.HOST
        );
    }

    /**
     * Sends a support message as the authenticated Host. Sender identity comes
     * from the JWT principal and the Host's own one-thread conversation is
     * resolved server-side, so the request never needs a senderId or thread id.
     */
    @Transactional
    public HostSupportMessageResponse sendHostMessage(
            SendMessageRequest request,
            Authentication authentication
    ) {
        User host = getActiveHost(authentication);
        String body = validateAndNormalizeBody(request);
        HostSupportConversation conversation =
                findOrCreateConversation(host);

        return sendMessage(
                conversation,
                host,
                body
        );
    }

    /**
     * Marks unread Admin-authored messages as read for the Host. Host-authored
     * messages are excluded so a sender never marks their own outgoing messages
     * as received.
     */
    @Transactional
    public HostSupportConversationResponse markHostConversationRead(
            Authentication authentication
    ) {
        User host = getActiveHost(authentication);
        HostSupportConversation conversation =
                findOrCreateConversation(host);

        markUnreadMessagesFromRole(
                conversation,
                Role.ADMIN
        );

        return mapToConversationResponse(
                conversation,
                Role.HOST
        );
    }

    /**
     * Returns every Host support conversation for the Admin inbox, sorted by
     * latest support activity.
     */
    @Transactional(readOnly = true)
    public List<HostSupportConversationResponse> getAdminConversations(
            Authentication authentication
    ) {
        getActiveAdmin(authentication);

        return conversationRepository
                .findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(conversation ->
                        mapToConversationResponse(
                                conversation,
                                Role.ADMIN
                        )
                )
                .toList();
    }

    /**
     * Creates or returns the single verification-support thread for a specific
     * Host. Admin supplies the Host id only; the service validates that the
     * target account is truly a Host and reuses the one-thread-per-Host rule.
     */
    @Transactional
    public HostSupportConversationDetailResponse createOrGetAdminConversationForHost(
            Long hostId,
            Authentication authentication
    ) {
        getActiveAdmin(authentication);
        User host = getHostForAdmin(hostId);
        HostSupportConversation conversation =
                findOrCreateConversation(host);

        return mapToDetailResponse(
                conversation,
                Role.ADMIN
        );
    }

    /**
     * Opens a single support conversation for Admin review. The conversation id
     * is authorized by the Admin role, never by Host ownership.
     */
    @Transactional(readOnly = true)
    public HostSupportConversationDetailResponse getAdminConversation(
            Long conversationId,
            Authentication authentication
    ) {
        getActiveAdmin(authentication);
        HostSupportConversation conversation =
                getConversation(conversationId);

        return mapToDetailResponse(
                conversation,
                Role.ADMIN
        );
    }

    /**
     * Sends a support response as the authenticated Admin. The Admin sender is
     * resolved from authentication so the browser cannot spoof sender identity.
     */
    @Transactional
    public HostSupportMessageResponse sendAdminMessage(
            Long conversationId,
            SendMessageRequest request,
            Authentication authentication
    ) {
        User admin = getActiveAdmin(authentication);
        String body = validateAndNormalizeBody(request);
        HostSupportConversation conversation =
                getConversation(conversationId);

        return sendMessage(
                conversation,
                admin,
                body
        );
    }

    /**
     * Marks unread Host-authored messages as read for the shared Admin side of
     * the thread. Admin-authored messages are left untouched.
     */
    @Transactional
    public HostSupportConversationResponse markAdminConversationRead(
            Long conversationId,
            Authentication authentication
    ) {
        getActiveAdmin(authentication);
        HostSupportConversation conversation =
                getConversation(conversationId);

        markUnreadMessagesFromRole(
                conversation,
                Role.HOST
        );

        return mapToConversationResponse(
                conversation,
                Role.ADMIN
        );
    }

    /**
     * Lazily creates the single support conversation for a Host. The database
     * unique constraint on host_id backs up this service rule so duplicate rows
     * cannot be created accidentally.
     */
    private HostSupportConversation findOrCreateConversation(
            User host
    ) {
        return conversationRepository
                .findByHost(host)
                .orElseGet(() ->
                        conversationRepository.save(
                                HostSupportConversation
                                        .builder()
                                        .host(host)
                                        .build()
                        )
                );
    }

    /**
     * Loads a support conversation for Admin operations and gives a clear
     * business error when the id does not exist.
     */
    private HostSupportConversation getConversation(
            Long conversationId
    ) {
        if (conversationId == null) {
            throw new RuntimeException(
                    "Support conversation ID is required"
            );
        }

        return conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Support conversation not found"
                        )
                );
    }

    /**
     * Resolves the Admin-supplied target id to a real Host account. Verification
     * status is intentionally not checked because support messaging exists for
     * Hosts before, during, and after verification.
     */
    private User getHostForAdmin(
            Long hostId
    ) {
        if (hostId == null) {
            throw new RuntimeException(
                    "Host ID is required"
            );
        }

        User host =
                userRepository
                        .findById(hostId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Host not found"
                                )
                        );

        if (host.getRole() != Role.HOST) {
            throw new RuntimeException(
                    "Host account required"
            );
        }

        return host;
    }

    /**
     * Persists a normalized support message and touches the conversation so the
     * inbox order reflects the latest support activity.
     */
    private HostSupportMessageResponse sendMessage(
            HostSupportConversation conversation,
            User sender,
            String body
    ) {
        HostSupportMessage message =
                HostSupportMessage
                        .builder()
                        .conversation(conversation)
                        .sender(sender)
                        .body(body)
                        .read(false)
                        .build();

        HostSupportMessage savedMessage =
                messageRepository.save(message);

        conversation.touch();
        conversationRepository.save(conversation);

        return mapToMessageResponse(savedMessage);
    }

    /**
     * Applies read state only to messages authored by the opposite side. This
     * keeps Host unread counts and Admin unread counts independent enough for
     * the current one-Host-to-Admin-side support model.
     */
    private void markUnreadMessagesFromRole(
            HostSupportConversation conversation,
            Role unreadSenderRole
    ) {
        LocalDateTime readTime = LocalDateTime.now();

        List<HostSupportMessage> unreadMessages =
                messageRepository
                        .findByConversationAndSender_RoleAndReadFalse(
                                conversation,
                                unreadSenderRole
                        );

        unreadMessages.forEach(message ->
                message.markRead(readTime)
        );

        messageRepository.saveAll(unreadMessages);
    }

    /**
     * Resolves and authorizes an active Host account for support messaging.
     * Verification is intentionally not checked here because unverified Hosts
     * need this channel to complete verification support conversations.
     */
    private User getActiveHost(
            Authentication authentication
    ) {
        User host = getAuthenticatedUser(authentication);

        if (host.getRole() != Role.HOST
                || host.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Active Host account required"
            );
        }

        return host;
    }

    /**
     * Resolves and authorizes an active Admin account for Admin support inbox
     * operations.
     */
    private User getActiveAdmin(
            Authentication authentication
    ) {
        User admin = getAuthenticatedUser(authentication);

        if (admin.getRole() != Role.ADMIN
                || admin.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Admin account required"
            );
        }

        return admin;
    }

    /**
     * Loads the current database user from Spring Security's authenticated
     * principal so role, account status, and profile data are never taken from
     * request bodies.
     */
    private User getAuthenticatedUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new RuntimeException(
                    "Authentication required"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }

    /**
     * Normalizes support message text before persistence and rejects empty or
     * oversized messages with the same business-error style used elsewhere in
     * the messaging services.
     */
    private String validateAndNormalizeBody(
            SendMessageRequest request
    ) {
        if (request == null || request.getBody() == null) {
            throw new RuntimeException(
                    "Message body is required"
            );
        }

        String body = request.getBody().trim();

        if (body.isBlank()) {
            throw new RuntimeException(
                    "Message body cannot be blank"
            );
        }

        if (body.length() > MAX_MESSAGE_BODY_LENGTH) {
            throw new RuntimeException(
                    "Message body cannot exceed "
                            + MAX_MESSAGE_BODY_LENGTH
                            + " characters"
            );
        }

        return body;
    }

    /**
     * Builds the detail response used by Host and Admin conversation views.
     * Messages are mapped through DTOs so lazy JPA relationships are never
     * serialized directly.
     */
    private HostSupportConversationDetailResponse mapToDetailResponse(
            HostSupportConversation conversation,
            Role viewerRole
    ) {
        List<HostSupportMessageResponse> messages =
                messageRepository
                        .findByConversationOrderByCreatedAtAsc(
                                conversation
                        )
                        .stream()
                        .map(this::mapToMessageResponse)
                        .toList();

        return new HostSupportConversationDetailResponse(
                mapToConversationResponse(
                        conversation,
                        viewerRole
                ),
                messages
        );
    }

    /**
     * Maps a support conversation summary for the current viewer side. The same
     * DTO powers the Host support screen and the Admin support inbox; only the
     * unread count changes based on which side is viewing.
     */
    private HostSupportConversationResponse mapToConversationResponse(
            HostSupportConversation conversation,
            Role viewerRole
    ) {
        User host = conversation.getHost();
        Role unreadSenderRole =
                viewerRole == Role.ADMIN
                        ? Role.HOST
                        : Role.ADMIN;

        HostSupportMessage lastMessage =
                messageRepository
                        .findTopByConversationOrderByCreatedAtDesc(
                                conversation
                        )
                        .orElse(null);

        return new HostSupportConversationResponse(
                conversation.getId(),
                host.getId(),
                buildDisplayName(host),
                host.getEmail(),
                host.getProfileImageUrl(),
                getVerificationStatus(host),
                lastMessage != null
                        ? buildPreview(lastMessage.getBody())
                        : null,
                lastMessage != null
                        ? lastMessage.getCreatedAt()
                        : null,
                messageRepository
                        .countByConversationAndSender_RoleAndReadFalse(
                                conversation,
                                unreadSenderRole
                        ),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    /**
     * Maps one support message without exposing the sender entity or any
     * private profile fields beyond the display-safe name and role.
     */
    private HostSupportMessageResponse mapToMessageResponse(
            HostSupportMessage message
    ) {
        User sender = message.getSender();

        return new HostSupportMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                sender.getId(),
                buildDisplayName(sender),
                sender.getRole(),
                message.getBody(),
                message.isRead(),
                message.getReadAt(),
                message.getCreatedAt()
        );
    }

    /**
     * Includes the Host verification status in Admin support inbox rows when a
     * HostVerification record exists, while keeping messaging independent from
     * verification state transitions.
     */
    private HostVerificationStatus getVerificationStatus(
            User host
    ) {
        return hostVerificationRepository
                .findByHost(host)
                .map(HostVerification::getStatus)
                .orElse(null);
    }

    /**
     * Builds a short inbox preview without changing the stored support message.
     */
    private String buildPreview(
            String body
    ) {
        if (body.length() <= MESSAGE_PREVIEW_LENGTH) {
            return body;
        }

        return body.substring(
                0,
                MESSAGE_PREVIEW_LENGTH - 3
        ) + "...";
    }

    /**
     * Uses public profile name fields for message display and falls back to a
     * role-based label when an account does not yet have a usable name.
     */
    private String buildDisplayName(
            User user
    ) {
        String firstName = user.getFirstName() == null
                ? ""
                : user.getFirstName().trim();

        String lastName = user.getLastName() == null
                ? ""
                : user.getLastName().trim();

        String fullName = (firstName + " " + lastName)
                .trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        if (user.getRole() == Role.ADMIN) {
            return "Admin " + user.getId();
        }

        if (user.getRole() == Role.HOST) {
            return "Host " + user.getId();
        }

        return "User " + user.getId();
    }
}
