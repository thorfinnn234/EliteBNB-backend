package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.ConversationDetailResponse;
import com.elitebnb_backend.dto.ConversationResponse;
import com.elitebnb_backend.dto.CreateConversationRequest;
import com.elitebnb_backend.dto.MessageResponse;
import com.elitebnb_backend.dto.SendMessageRequest;
import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.Conversation;
import com.elitebnb_backend.entity.Message;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.PropertyType;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.BookingRepository;
import com.elitebnb_backend.repository.ConversationRepository;
import com.elitebnb_backend.repository.MessageRepository;
import com.elitebnb_backend.repository.PropertyImageRepository;
import com.elitebnb_backend.repository.PropertyRepository;
import com.elitebnb_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PropertyImageRepository propertyImageRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private HostAccessService hostAccessService;

    private ConversationService conversationService;
    private User guest;
    private User otherGuest;
    private User host;
    private User otherHost;
    private Property property;
    private Booking booking;

    /**
     * Builds a service with mocked repositories so these tests can verify
     * messaging rules without starting the full Spring context or external
     * services such as Cloudinary, Brevo, Paystack, or PostgreSQL.
     */
    @BeforeEach
    void setUp() {
        conversationService =
                new ConversationService(
                        conversationRepository,
                        messageRepository,
                        userRepository,
                        propertyRepository,
                        bookingRepository,
                        propertyImageRepository,
                        notificationService,
                        hostAccessService
                );

        guest = user(1L, "guest@example.com", "Jane", "Guest", Role.USER);
        otherGuest = user(2L, "other@example.com", "Other", "Guest", Role.USER);
        host = user(10L, "host@example.com", "Hakeem", "Host", Role.HOST);
        otherHost = user(11L, "other-host@example.com", "Other", "Host", Role.HOST);
        property = property(100L, host);
        booking = booking(200L, guest, property);

        lenient().when(userRepository.findByEmail(guest.getEmail()))
                .thenReturn(Optional.of(guest));
        lenient().when(userRepository.findByEmail(otherGuest.getEmail()))
                .thenReturn(Optional.of(otherGuest));
        lenient().when(userRepository.findByEmail(host.getEmail()))
                .thenReturn(Optional.of(host));
        lenient().when(userRepository.findByEmail(otherHost.getEmail()))
                .thenReturn(Optional.of(otherHost));

        lenient().when(propertyImageRepository.findByPropertyId(property.getId()))
                .thenReturn(List.of());
        lenient().when(messageRepository.findTopByConversationOrderByCreatedAtDesc(
                any(Conversation.class)
        )).thenReturn(Optional.empty());
        lenient().when(messageRepository.countByConversationAndSenderNotAndReadFalse(
                any(Conversation.class),
                any(User.class)
        )).thenReturn(0L);

        lenient().when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> {
                    Conversation conversation = invocation.getArgument(0);

                    if (conversation.getId() == null) {
                        conversation.setId(500L);
                    }

                    return conversation;
                });

        lenient().when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> {
                    Message message = invocation.getArgument(0);

                    if (message.getId() == null) {
                        message.setId(600L);
                    }

                    return message;
                });

        lenient().when(messageRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Verifies that property-origin conversations derive the host from property
     * ownership and the guest from authentication.
     */
    @Test
    void createConversationUsesPropertyHostAndAuthenticatedGuest() {
        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));
        when(conversationRepository.findByGuestAndHostAndProperty(
                guest,
                host,
                property
        )).thenReturn(Optional.empty());

        ConversationResponse response =
                conversationService.createConversation(
                        createConversationRequest(
                                property.getId(),
                                null
                        ),
                        authenticationFor(guest)
                );

        ArgumentCaptor<Conversation> conversationCaptor =
                ArgumentCaptor.forClass(Conversation.class);

        verify(conversationRepository).save(
                conversationCaptor.capture()
        );

        Conversation savedConversation =
                conversationCaptor.getValue();

        assertThat(savedConversation.getGuest()).isSameAs(guest);
        assertThat(savedConversation.getHost()).isSameAs(host);
        assertThat(savedConversation.getProperty()).isSameAs(property);
        assertThat(response.getGuestId()).isEqualTo(guest.getId());
        assertThat(response.getHostId()).isEqualTo(host.getId());
    }

    /**
     * Normal guest-to-Host messaging is a Host business surface. Guests can
     * only start that conversation after the property's real Host has been
     * verified, and the backend checks the derived Host before any thread is
     * reused or created.
     */
    @Test
    void userCannotStartConversationWithUnverifiedHost() {
        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));
        doThrow(businessAccessDenied())
                .when(hostAccessService)
                .requireVerifiedBusinessAccess(host);

        assertThatThrownBy(() ->
                conversationService.createConversation(
                        createConversationRequest(
                                property.getId(),
                                null
                        ),
                        authenticationFor(guest)
                )
        ).isInstanceOf(AccessDeniedException.class)
                .hasMessage(
                        "Host verification is required before using Host business features"
                );

        verify(conversationRepository, never())
                .findByGuestAndHostAndProperty(
                        any(User.class),
                        any(User.class),
                        any(Property.class)
                );
        verify(conversationRepository, never())
                .save(any(Conversation.class));
    }

    /**
     * Verifies the deterministic reuse rule so repeated "Message Host" clicks
     * do not create duplicate threads.
     */
    @Test
    void createConversationReusesExistingConversationForSameGuestHostProperty() {
        Conversation existingConversation =
                conversation(700L, guest, host, property, null);

        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));
        when(conversationRepository.findByGuestAndHostAndProperty(
                guest,
                host,
                property
        )).thenReturn(Optional.of(existingConversation));

        ConversationResponse response =
                conversationService.createConversation(
                        createConversationRequest(
                                property.getId(),
                                null
                        ),
                        authenticationFor(guest)
                );

        assertThat(response.getId())
                .isEqualTo(existingConversation.getId());

        verify(conversationRepository, never())
                .save(any(Conversation.class));
        verifyNoInteractions(bookingRepository);
    }

    /**
     * Verifies that valid reservation context is attached to the existing
     * property conversation instead of creating a parallel booking-specific
     * conversation.
     */
    @Test
    void createConversationAttachesVerifiedBookingToExistingConversation() {
        Conversation existingConversation =
                conversation(700L, guest, host, property, null);

        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));
        when(bookingRepository.findById(booking.getId()))
                .thenReturn(Optional.of(booking));
        when(conversationRepository.findByGuestAndHostAndProperty(
                guest,
                host,
                property
        )).thenReturn(Optional.of(existingConversation));

        ConversationResponse response =
                conversationService.createConversation(
                        createConversationRequest(
                                property.getId(),
                                booking.getId()
                        ),
                        authenticationFor(guest)
                );

        assertThat(response.getId())
                .isEqualTo(existingConversation.getId());
        assertThat(response.getBookingId())
                .isEqualTo(booking.getId());

        verify(conversationRepository).save(existingConversation);
    }

    /**
     * Verifies that a guest cannot use another guest's reservation as
     * conversation context.
     */
    @Test
    void createConversationRejectsBookingOwnedByAnotherGuest() {
        Booking otherGuestBooking =
                booking(201L, otherGuest, property);

        when(propertyRepository.findById(property.getId()))
                .thenReturn(Optional.of(property));
        when(bookingRepository.findById(otherGuestBooking.getId()))
                .thenReturn(Optional.of(otherGuestBooking));

        assertThatThrownBy(() ->
                conversationService.createConversation(
                        createConversationRequest(
                                property.getId(),
                                otherGuestBooking.getId()
                        ),
                        authenticationFor(guest)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "You can only start booking conversations for your own booking"
                );

        verify(conversationRepository, never())
                .findByGuestAndHostAndProperty(
                        any(User.class),
                        any(User.class),
                        any(Property.class)
                );
    }

    /**
     * Verifies that one guest cannot access a conversation belonging to another
     * guest.
     */
    @Test
    void userCannotAccessAnotherUsersConversation() {
        Conversation conversation =
                conversation(700L, otherGuest, host, property, null);

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        assertThatThrownBy(() ->
                conversationService.getConversation(
                        conversation.getId(),
                        authenticationFor(guest)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "You are not allowed to access this conversation"
                );
    }

    /**
     * Verifies that one host cannot access a conversation belonging to another
     * host.
     */
    @Test
    void hostCannotAccessAnotherHostsConversation() {
        Conversation conversation =
                conversation(700L, guest, otherHost, property, null);

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        assertThatThrownBy(() ->
                conversationService.getConversation(
                        conversation.getId(),
                        authenticationFor(host)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "You are not allowed to access this conversation"
                );
    }

    /**
     * Once the central Host access guard allows the Host, the normal inbox
     * query still works. This protects the existing USER<->HOST messaging
     * contract for verified Hosts.
     */
    @Test
    void verifiedHostPreservesConversationAccess() {
        Conversation conversation =
                conversation(700L, guest, host, property, null);

        when(conversationRepository.findByHostOrderByUpdatedAtDesc(host))
                .thenReturn(List.of(conversation));

        List<ConversationResponse> responses =
                conversationService.getMyConversations(
                        authenticationFor(host)
                );

        assertThat(responses)
                .extracting(ConversationResponse::getId)
                .containsExactly(conversation.getId());
        verify(hostAccessService)
                .requireVerifiedBusinessAccess(host);
    }

    /**
     * Unverified Hosts can continue the verification journey elsewhere, but
     * they cannot use the normal guest-to-Host inbox until Admin verification
     * succeeds.
     */
    @Test
    void unverifiedHostCannotListBusinessConversations() {
        doThrow(businessAccessDenied())
                .when(hostAccessService)
                .requireVerifiedBusinessAccess(host);

        assertThatThrownBy(() ->
                conversationService.getMyConversations(
                        authenticationFor(host)
                )
        ).isInstanceOf(AccessDeniedException.class)
                .hasMessage(
                        "Host verification is required before using Host business features"
                );

        verify(conversationRepository, never())
                .findByHostOrderByUpdatedAtDesc(any(User.class));
    }

    /**
     * Sender identity still comes from authentication, and an unverified Host
     * is denied before the service even loads a conversation by id.
     */
    @Test
    void unverifiedHostCannotSendBusinessConversationMessage() {
        doThrow(businessAccessDenied())
                .when(hostAccessService)
                .requireVerifiedBusinessAccess(host);

        assertThatThrownBy(() ->
                conversationService.sendMessage(
                        700L,
                        sendMessageRequest("Hello guest"),
                        authenticationFor(host)
                )
        ).isInstanceOf(AccessDeniedException.class)
                .hasMessage(
                        "Host verification is required before using Host business features"
                );

        verify(conversationRepository, never())
                .findById(any(Long.class));
        verify(messageRepository, never())
                .save(any(Message.class));
    }

    /**
     * Verifies that a nonparticipant cannot send messages even when they know a
     * real conversation id.
     */
    @Test
    void nonParticipantCannotSendMessage() {
        Conversation conversation =
                conversation(700L, otherGuest, host, property, null);

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        assertThatThrownBy(() ->
                conversationService.sendMessage(
                        conversation.getId(),
                        sendMessageRequest("Hello"),
                        authenticationFor(guest)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "You are not allowed to access this conversation"
                );

        verify(messageRepository, never()).save(any(Message.class));
    }

    /**
     * Verifies that the sender comes from authentication and message bodies are
     * normalized before storage.
     */
    @Test
    void sendMessageDerivesSenderFromAuthenticationAndTrimsBody() {
        Conversation conversation =
                conversation(700L, guest, host, property, null);

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        MessageResponse response =
                conversationService.sendMessage(
                        conversation.getId(),
                        sendMessageRequest("  Hello host  "),
                        authenticationFor(guest)
                );

        ArgumentCaptor<Message> messageCaptor =
                ArgumentCaptor.forClass(Message.class);

        verify(messageRepository).save(messageCaptor.capture());

        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getSender()).isSameAs(guest);
        assertThat(savedMessage.getBody()).isEqualTo("Hello host");
        assertThat(response.getSenderId()).isEqualTo(guest.getId());

        verify(notificationService).createConversationMessageNotification(
                eq(host),
                eq("New message"),
                eq("Jane Guest sent you a message about Ocean View Apartment."),
                eq(conversation.getId()),
                eq(response.getId()),
                isNull(),
                eq(property)
        );
        verify(notificationService, never())
                .createConversationMessageNotification(
                        eq(guest),
                        any(String.class),
                        any(String.class),
                        any(Long.class),
                        any(Long.class),
                        any(),
                        any()
                );
    }

    /**
     * Host-to-guest replies create one linked MESSAGE notification for the
     * guest. The sender still receives no notification.
     */
    @Test
    void hostMessageCreatesLinkedNotificationForGuest() {
        Conversation conversation =
                conversation(701L, guest, host, property, null);

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        MessageResponse response =
                conversationService.sendMessage(
                        conversation.getId(),
                        sendMessageRequest("Hello guest"),
                        authenticationFor(host)
                );

        verify(notificationService).createConversationMessageNotification(
                eq(guest),
                eq("New message"),
                eq("Hakeem Host sent you a message about Ocean View Apartment."),
                eq(conversation.getId()),
                eq(response.getId()),
                isNull(),
                eq(property)
        );
        verify(notificationService, never())
                .createConversationMessageNotification(
                        eq(host),
                        any(String.class),
                        any(String.class),
                        any(Long.class),
                        any(Long.class),
                        any(),
                        any()
                );
    }

    /**
     * Verifies that empty or whitespace-only messages are rejected before any
     * database write.
     */
    @Test
    void blankMessageIsRejected() {
        Conversation conversation =
                conversation(700L, guest, host, property, null);

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        assertThatThrownBy(() ->
                conversationService.sendMessage(
                        conversation.getId(),
                        sendMessageRequest("   "),
                        authenticationFor(guest)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Message body cannot be blank");

        verify(messageRepository, never()).save(any(Message.class));
    }

    /**
     * Verifies that conversation details preserve the repository's
     * oldest-to-newest message ordering.
     */
    @Test
    void getConversationReturnsMessagesOldestToNewest() {
        Conversation conversation =
                conversation(700L, guest, host, property, null);

        Message olderMessage =
                message(1L, conversation, guest, "Older", false);
        Message newerMessage =
                message(2L, conversation, host, "Newer", false);

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationOrderByCreatedAtAsc(
                conversation
        )).thenReturn(List.of(olderMessage, newerMessage));

        ConversationDetailResponse response =
                conversationService.getConversation(
                        conversation.getId(),
                        authenticationFor(guest)
                );

        assertThat(response.getMessages())
                .extracting(MessageResponse::getId)
                .containsExactly(1L, 2L);
    }

    /**
     * Verifies that inbox responses expose unread received messages for the
     * current participant.
     */
    @Test
    void conversationListExposesUnreadCount() {
        Conversation conversation =
                conversation(700L, guest, host, property, null);

        when(conversationRepository.findByHostOrderByUpdatedAtDesc(host))
                .thenReturn(List.of(conversation));
        when(messageRepository.countByConversationAndSenderNotAndReadFalse(
                conversation,
                host
        )).thenReturn(2L);

        List<ConversationResponse> responses =
                conversationService.getMyConversations(
                        authenticationFor(host)
                );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUnreadCount())
                .isEqualTo(2L);
    }

    /**
     * Verifies that mark-read updates only messages sent by the other
     * participant, never the viewer's own outgoing messages.
     */
    @Test
    void markConversationReadOnlyReadsOtherParticipantMessages() {
        Conversation conversation =
                conversation(700L, guest, host, property, null);

        Message unreadHostMessage =
                message(1L, conversation, host, "Please confirm", false);
        Message ownGuestMessage =
                message(2L, conversation, guest, "Thanks", false);

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationAndSenderNotAndReadFalse(
                conversation,
                guest
        )).thenReturn(List.of(unreadHostMessage));

        conversationService.markConversationRead(
                conversation.getId(),
                authenticationFor(guest)
        );

        assertThat(unreadHostMessage.isRead()).isTrue();
        assertThat(unreadHostMessage.getReadAt()).isNotNull();
        assertThat(ownGuestMessage.isRead()).isFalse();

        verify(messageRepository).saveAll(
                List.of(unreadHostMessage)
        );
        verify(notificationService)
                .markConversationMessageNotificationsRead(
                        guest,
                        conversation.getId()
                );
    }

    /**
     * Creates a request matching the public conversation creation contract.
     */
    private CreateConversationRequest createConversationRequest(
            Long propertyId,
            Long bookingId
    ) {
        CreateConversationRequest request =
                new CreateConversationRequest();

        request.setPropertyId(propertyId);
        request.setBookingId(bookingId);

        return request;
    }

    /**
     * Creates a request matching the public send-message contract.
     */
    private SendMessageRequest sendMessageRequest(
            String body
    ) {
        SendMessageRequest request =
                new SendMessageRequest();

        request.setBody(body);

        return request;
    }

    /**
     * Builds an authenticated Spring Security principal for the supplied user.
     */
    private Authentication authenticationFor(
            User user
    ) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                )
        );
    }

    /**
     * Builds a user with public profile fields used by messaging responses.
     */
    private User user(
            Long id,
            String email,
            String firstName,
            String lastName,
            Role role
    ) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded-password")
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .emailVerified(true)
                .build();
    }

    /**
     * Builds a property whose host relationship can be trusted by the service.
     */
    private Property property(
            Long id,
            User host
    ) {
        return Property.builder()
                .id(id)
                .title("Ocean View Apartment")
                .description("A waterfront stay.")
                .location("Lekki")
                .pricePerNight(100000.0)
                .bedrooms(2)
                .bathrooms(2)
                .maxGuests(4)
                .propertyType(PropertyType.APARTMENT)
                .host(host)
                .build();
    }

    /**
     * Builds a booking that ties a guest to a property for reservation-origin
     * conversation checks.
     */
    private Booking booking(
            Long id,
            User guest,
            Property property
    ) {
        return Booking.builder()
                .id(id)
                .guest(guest)
                .property(property)
                .checkIn(LocalDate.now().plusDays(1))
                .checkOut(LocalDate.now().plusDays(3))
                .guests(2)
                .totalAmount(200000.0)
                .build();
    }

    /**
     * Builds an existing conversation with stable timestamps for inbox mapping.
     */
    private Conversation conversation(
            Long id,
            User guest,
            User host,
            Property property,
            Booking booking
    ) {
        return Conversation.builder()
                .id(id)
                .guest(guest)
                .host(host)
                .property(property)
                .booking(booking)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Builds messages for ordering and read/unread behavior tests.
     */
    private Message message(
            Long id,
            Conversation conversation,
            User sender,
            String body,
            boolean read
    ) {
        return Message.builder()
                .id(id)
                .conversation(conversation)
                .sender(sender)
                .body(body)
                .read(read)
                .createdAt(LocalDateTime.now().plusSeconds(id))
                .build();
    }

    /**
     * Reuses the same 403-style exception that HostAccessService raises for
     * authenticated Hosts who are not yet verified for business operations.
     */
    private AccessDeniedException businessAccessDenied() {
        return new AccessDeniedException(
                "Host verification is required before using Host business features"
        );
    }
}
