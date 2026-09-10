package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.ConversationDetailResponse;
import com.elitebnb_backend.dto.ConversationResponse;
import com.elitebnb_backend.dto.CreateConversationRequest;
import com.elitebnb_backend.dto.MessageResponse;
import com.elitebnb_backend.dto.SendMessageRequest;
import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.Conversation;
import com.elitebnb_backend.entity.Message;
import com.elitebnb_backend.entity.NotificationType;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyImage;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.ConversationRepository;
import com.elitebnb_backend.repository.MessageRepository;
import com.elitebnb_backend.repository.PropertyImageRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int MAX_MESSAGE_BODY_LENGTH = 2000;
    private static final int MESSAGE_PREVIEW_LENGTH = 80;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final NotificationService notificationService;
    private final HostAccessService hostAccessService;

    /**
     * Starts or reuses the single conversation for an authenticated guest and a
     * property's real host. The frontend supplies context only; the sender,
     * guest, and host identities are resolved from authentication and database
     * ownership so the browser cannot invent participants.
     */
    @Transactional
    public ConversationResponse createConversation(
            CreateConversationRequest request,
            Authentication authentication
    ) {
        User guest = getAuthenticatedUser(authentication);

        if (guest.getRole() != Role.USER) {
            throw new RuntimeException(
                    "Only guests can start conversations"
            );
        }

        if (request == null || request.getPropertyId() == null) {
            throw new RuntimeException("Property ID is required");
        }

        Property property = propertyRepository
                .findById(request.getPropertyId())
                .orElseThrow(() ->
                        new RuntimeException("Property not found")
                );

        // The host is derived from the property owner. We never accept a hostId
        // from the frontend because that would let the browser choose who is in
        // the conversation.
        User host = property.getHost();

        if (host == null) {
            throw new RuntimeException("Property host not found");
        }

        if (sameUser(guest, host)) {
            throw new RuntimeException(
                    "You cannot start a conversation with yourself"
            );
        }

        hostAccessService.requireVerifiedBusinessAccess(host);

        Booking booking = resolveBookingContext(
                request,
                guest,
                property,
                host
        );

        Conversation conversation = findOrCreateConversation(
                guest,
                host,
                property,
                booking
        );

        return mapToConversationResponse(
                conversation,
                guest
        );
    }

    /**
     * Returns the authenticated participant's inbox. Guests see conversations
     * where they are the guest; hosts see conversations where they are the host.
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations(
            Authentication authentication
    ) {
        User participant = getMessagingParticipant(authentication);

        List<Conversation> conversations =
                participant.getRole() == Role.USER
                        ? conversationRepository
                                .findByGuestOrderByUpdatedAtDesc(participant)
                        : conversationRepository
                                .findByHostOrderByUpdatedAtDesc(participant);

        return conversations
                .stream()
                .map(conversation ->
                        mapToConversationResponse(
                                conversation,
                                participant
                        )
                )
                .toList();
    }

    /**
     * Returns one authorized conversation and its messages in oldest-to-newest
     * order. Marking read is kept as an explicit PATCH so this read endpoint
     * remains predictable for clients and tests.
     */
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversation(
            Long conversationId,
            Authentication authentication
    ) {
        User participant = getMessagingParticipant(authentication);

        Conversation conversation =
                getAuthorizedConversation(
                        conversationId,
                        participant
                );

        List<MessageResponse> messages =
                messageRepository
                        .findByConversationOrderByCreatedAtAsc(
                                conversation
                        )
                        .stream()
                        .map(this::mapToMessageResponse)
                        .toList();

        return new ConversationDetailResponse(
                mapToConversationResponse(
                        conversation,
                        participant
                ),
                messages
        );
    }

    /**
     * Sends a message as the authenticated participant. There is no senderId in
     * the request because sender identity must come from the JWT principal.
     */
    @Transactional
    public MessageResponse sendMessage(
            Long conversationId,
            SendMessageRequest request,
            Authentication authentication
    ) {
        User sender = getMessagingParticipant(authentication);

        Conversation conversation =
                getAuthorizedConversation(
                        conversationId,
                        sender
                );

        String body = validateAndNormalizeBody(request);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .body(body)
                .read(false)
                .build();

        Message savedMessage = messageRepository.save(message);

        // A new message is the only event that intentionally updates inbox
        // activity ordering.
        conversation.touch();
        conversationRepository.save(conversation);

        createNewMessageNotification(
                conversation,
                savedMessage,
                resolveRecipient(conversation, sender)
        );

        return mapToMessageResponse(savedMessage);
    }

    /**
     * Marks only messages from the other participant as read. This keeps a
     * sender's own messages out of unread state and gives each participant a
     * correct inbox count.
     */
    @Transactional
    public ConversationResponse markConversationRead(
            Long conversationId,
            Authentication authentication
    ) {
        User participant = getMessagingParticipant(authentication);

        Conversation conversation =
                getAuthorizedConversation(
                        conversationId,
                        participant
                );

        LocalDateTime readTime = LocalDateTime.now();

        List<Message> unreadReceivedMessages =
                messageRepository
                        .findByConversationAndSenderNotAndReadFalse(
                                conversation,
                                participant
                        );

        unreadReceivedMessages.forEach(message ->
                message.markRead(readTime)
        );

        messageRepository.saveAll(unreadReceivedMessages);

        return mapToConversationResponse(
                conversation,
                participant
        );
    }

    /**
     * Resolves the authenticated user from Spring Security, then loads the
     * database user so role and profile data come from the current backend
     * state instead of from request data.
     */
    private User getAuthenticatedUser(
            Authentication authentication
    ) {
        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException("Authentication required");
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );
    }

    /**
     * Restricts general messaging access to real guest and verified host
     * accounts. Admin routes are separate in this backend and do not grant
     * conversation access.
     */
    private User getMessagingParticipant(
            Authentication authentication
    ) {
        User participant = getAuthenticatedUser(authentication);

        if (participant.getRole() != Role.USER &&
                participant.getRole() != Role.HOST) {

            throw new RuntimeException(
                    "Only guests and hosts can use messaging"
            );
        }

        if (participant.getRole() == Role.HOST) {
            hostAccessService.requireVerifiedBusinessAccess(
                    participant
            );
        }

        return participant;
    }

    /**
     * Validates optional booking context for reservation-origin conversations.
     * The booking must belong to the authenticated guest, the supplied property,
     * and that property's resolved host.
     */
    private Booking resolveBookingContext(
            CreateConversationRequest request,
            User guest,
            Property property,
            User host
    ) {
        if (request.getBookingId() == null) {
            return null;
        }

        Booking booking = bookingRepository
                .findById(request.getBookingId())
                .orElseThrow(() ->
                        new RuntimeException("Booking not found")
                );

        if (!sameUser(booking.getGuest(), guest)) {
            throw new RuntimeException(
                    "You can only start booking conversations for your own booking"
            );
        }

        if (!sameId(
                booking.getProperty().getId(),
                property.getId()
        )) {
            throw new RuntimeException(
                    "Booking does not belong to this property"
            );
        }

        if (!sameUser(booking.getProperty().getHost(), host)) {
            throw new RuntimeException(
                    "Booking property does not belong to this host"
            );
        }

        return booking;
    }

    /**
     * Enforces deterministic duplicate prevention. One conversation exists per
     * guest, host, and property; booking context is attached to that same thread
     * when it is supplied and has passed ownership checks.
     */
    private Conversation findOrCreateConversation(
            User guest,
            User host,
            Property property,
            Booking booking
    ) {
        return conversationRepository
                .findByGuestAndHostAndProperty(
                        guest,
                        host,
                        property
                )
                .map(existingConversation ->
                        attachBookingWhenNeeded(
                                existingConversation,
                                booking
                        )
                )
                .orElseGet(() -> {
                    Conversation conversation =
                            Conversation.builder()
                                    .guest(guest)
                                    .host(host)
                                    .property(property)
                                    .booking(booking)
                                    .build();

                    return conversationRepository.save(
                            conversation
                    );
                });
    }

    /**
     * Keeps booking context useful without creating parallel conversations. If a
     * guest later starts from a verified reservation for the same property
     * relationship, the single conversation points at that reservation.
     */
    private Conversation attachBookingWhenNeeded(
            Conversation conversation,
            Booking booking
    ) {
        if (booking == null) {
            return conversation;
        }

        if (conversation.getBooking() != null &&
                sameId(
                        conversation.getBooking().getId(),
                        booking.getId()
                )) {

            return conversation;
        }

        conversation.setBooking(booking);

        return conversationRepository.save(conversation);
    }

    /**
     * Loads a conversation by id, then verifies participant ownership. A valid
     * conversationId alone never grants access.
     */
    private Conversation getAuthorizedConversation(
            Long conversationId,
            User participant
    ) {
        if (conversationId == null) {
            throw new RuntimeException("Conversation ID is required");
        }

        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Conversation not found"
                                )
                        );

        if (!isParticipant(conversation, participant)) {
            throw new RuntimeException(
                    "You are not allowed to access this conversation"
            );
        }

        return conversation;
    }

    /**
     * Checks whether the authenticated account is either the guest or host on
     * the conversation.
     */
    private boolean isParticipant(
            Conversation conversation,
            User participant
    ) {
        return sameUser(conversation.getGuest(), participant) ||
                sameUser(conversation.getHost(), participant);
    }

    /**
     * Normalizes message text before storage so blank or whitespace-only
     * messages cannot be sent.
     */
    private String validateAndNormalizeBody(
            SendMessageRequest request
    ) {
        if (request == null || request.getBody() == null) {
            throw new RuntimeException("Message body is required");
        }

        String body = request.getBody().trim();

        if (body.isBlank()) {
            throw new RuntimeException("Message body cannot be blank");
        }

        if (body.length() > MAX_MESSAGE_BODY_LENGTH) {
            throw new RuntimeException(
                    "Message body cannot exceed " +
                            MAX_MESSAGE_BODY_LENGTH +
                            " characters"
            );
        }

        return body;
    }

    /**
     * Finds the participant who should receive a new-message notification.
     */
    private User resolveRecipient(
            Conversation conversation,
            User sender
    ) {
        if (sameUser(sender, conversation.getGuest())) {
            return conversation.getHost();
        }

        return conversation.getGuest();
    }

    /**
     * Reuses the existing in-app notification system for lightweight message
     * alerts. No email, SMS, WebSocket, or separate notification mechanism is
     * introduced.
     */
    private void createNewMessageNotification(
            Conversation conversation,
            Message message,
            User recipient
    ) {
        notificationService.createNotification(
                recipient,
                "New message",
                buildFullName(message.getSender())
                        + " sent you a message about "
                        + conversation.getProperty().getTitle()
                        + ".",
                NotificationType.SYSTEM,
                conversation.getBooking(),
                conversation.getProperty()
        );
    }

    /**
     * Maps a conversation to a frontend-safe summary. Contact details and raw
     * entities are intentionally omitted; only display names, profile images,
     * property context, preview data, and unread counts are exposed.
     */
    private ConversationResponse mapToConversationResponse(
            Conversation conversation,
            User viewer
    ) {
        User guest = conversation.getGuest();
        User host = conversation.getHost();
        Property property = conversation.getProperty();
        User otherParticipant =
                sameUser(viewer, guest) ? host : guest;

        Message lastMessage =
                messageRepository
                        .findTopByConversationOrderByCreatedAtDesc(
                                conversation
                        )
                        .orElse(null);

        return new ConversationResponse(
                conversation.getId(),
                property.getId(),
                property.getTitle(),
                resolvePropertyImageUrl(property),
                conversation.getBooking() != null
                        ? conversation.getBooking().getId()
                        : null,
                guest.getId(),
                buildFullName(guest),
                guest.getProfileImageUrl(),
                host.getId(),
                buildFullName(host),
                host.getProfileImageUrl(),
                buildFullName(otherParticipant),
                otherParticipant.getProfileImageUrl(),
                lastMessage != null
                        ? buildPreview(lastMessage.getBody())
                        : null,
                lastMessage != null
                        ? lastMessage.getCreatedAt()
                        : null,
                messageRepository
                        .countByConversationAndSenderNotAndReadFalse(
                                conversation,
                                viewer
                        ),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    /**
     * Maps a message without serializing its conversation or sender entity.
     */
    private MessageResponse mapToMessageResponse(
            Message message
    ) {
        User sender = message.getSender();

        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                sender.getId(),
                buildFullName(sender),
                sender.getRole(),
                message.getBody(),
                message.isRead(),
                message.getReadAt(),
                message.getCreatedAt()
        );
    }

    /**
     * Selects a cover image when one exists, otherwise falls back to the first
     * stored property image. The query keeps DTO mapping away from lazy entity
     * serialization.
     */
    private String resolvePropertyImageUrl(
            Property property
    ) {
        List<PropertyImage> images =
                propertyImageRepository
                        .findByPropertyId(property.getId());

        return images
                .stream()
                .filter(PropertyImage::isCoverImage)
                .map(PropertyImage::getImageUrl)
                .findFirst()
                .orElseGet(() ->
                        images.stream()
                                .map(PropertyImage::getImageUrl)
                                .findFirst()
                                .orElse(null)
                );
    }

    /**
     * Builds a compact inbox preview without changing the stored message body.
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
     * Builds display names from public profile fields only; private contact data
     * such as email and phone number are not used for messaging responses.
     */
    private String buildFullName(
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

        return user.getRole() == Role.HOST
                ? "Host " + user.getId()
                : "Guest " + user.getId();
    }

    /**
     * Compares nullable entity ids safely.
     */
    private boolean sameId(
            Long firstId,
            Long secondId
    ) {
        return firstId != null && firstId.equals(secondId);
    }

    /**
     * Compares two users by id so authorization still works with lazy proxies.
     */
    private boolean sameUser(
            User firstUser,
            User secondUser
    ) {
        return firstUser != null &&
                secondUser != null &&
                sameId(
                        firstUser.getId(),
                        secondUser.getId()
                );
    }
}
