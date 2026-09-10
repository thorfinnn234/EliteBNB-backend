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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostSupportConversationServiceTest {

    @Mock
    private HostSupportConversationRepository conversationRepository;

    @Mock
    private HostSupportMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HostVerificationRepository hostVerificationRepository;

    private HostSupportConversationService service;

    private User activeHost;
    private User verifiedHost;
    private User otherHost;
    private User guest;
    private User admin;

    @BeforeEach
    void setUp() {
        service =
                new HostSupportConversationService(
                        conversationRepository,
                        messageRepository,
                        userRepository,
                        hostVerificationRepository
                );

        activeHost =
                user(
                        10L,
                        "active-host@example.test",
                        "Active",
                        "Host",
                        Role.HOST,
                        AccountStatus.ACTIVE
                );
        verifiedHost =
                user(
                        11L,
                        "verified-host@example.test",
                        "Verified",
                        "Host",
                        Role.HOST,
                        AccountStatus.ACTIVE
                );
        otherHost =
                user(
                        12L,
                        "other-host@example.test",
                        "Other",
                        "Host",
                        Role.HOST,
                        AccountStatus.ACTIVE
                );
        guest =
                user(
                        20L,
                        "guest@example.test",
                        "Guest",
                        "User",
                        Role.USER,
                        AccountStatus.ACTIVE
                );
        admin =
                user(
                        99L,
                        "admin@example.test",
                        "Ada",
                        "Admin",
                        Role.ADMIN,
                        AccountStatus.ACTIVE
                );

        lenient().when(userRepository.findByEmail(activeHost.getEmail()))
                .thenReturn(Optional.of(activeHost));
        lenient().when(userRepository.findByEmail(verifiedHost.getEmail()))
                .thenReturn(Optional.of(verifiedHost));
        lenient().when(userRepository.findByEmail(otherHost.getEmail()))
                .thenReturn(Optional.of(otherHost));
        lenient().when(userRepository.findByEmail(guest.getEmail()))
                .thenReturn(Optional.of(guest));
        lenient().when(userRepository.findByEmail(admin.getEmail()))
                .thenReturn(Optional.of(admin));
        lenient().when(userRepository.findById(activeHost.getId()))
                .thenReturn(Optional.of(activeHost));
        lenient().when(userRepository.findById(verifiedHost.getId()))
                .thenReturn(Optional.of(verifiedHost));
        lenient().when(userRepository.findById(otherHost.getId()))
                .thenReturn(Optional.of(otherHost));
        lenient().when(userRepository.findById(guest.getId()))
                .thenReturn(Optional.of(guest));
        lenient().when(userRepository.findById(admin.getId()))
                .thenReturn(Optional.of(admin));
        lenient().when(hostVerificationRepository.findByHost(any(User.class)))
                .thenReturn(Optional.empty());

        lenient().when(conversationRepository.save(
                any(HostSupportConversation.class)
        )).thenAnswer(invocation -> {
            HostSupportConversation conversation =
                    invocation.getArgument(0);

            if (conversation.getId() == null) {
                conversation.setId(700L);
            }

            return conversation;
        });

        lenient().when(messageRepository.save(
                any(HostSupportMessage.class)
        )).thenAnswer(invocation -> {
            HostSupportMessage message =
                    invocation.getArgument(0);

            if (message.getId() == null) {
                message.setId(800L);
            }

            return message;
        });

        lenient().when(messageRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(messageRepository.findByConversationOrderByCreatedAtAsc(
                any(HostSupportConversation.class)
        )).thenReturn(List.of());
        lenient().when(messageRepository.findTopByConversationOrderByCreatedAtDesc(
                any(HostSupportConversation.class)
        )).thenReturn(Optional.empty());
        lenient().when(messageRepository.countByConversationAndSender_RoleAndReadFalse(
                any(HostSupportConversation.class),
                any(Role.class)
        )).thenReturn(0L);
    }

    /**
     * An ACTIVE Host with no verification record can open support messaging;
     * the thread is created lazily and does not require normal Host business
     * access.
     */
    @Test
    void activeUnverifiedHostCanCreateAndAccessSupportThread() {
        when(conversationRepository.findByHost(activeHost))
                .thenReturn(Optional.empty());
        when(hostVerificationRepository.findByHost(activeHost))
                .thenReturn(Optional.empty());

        HostSupportConversationDetailResponse response =
                service.getHostConversation(
                        authenticationFor(activeHost)
                );

        ArgumentCaptor<HostSupportConversation> conversationCaptor =
                ArgumentCaptor.forClass(
                        HostSupportConversation.class
                );

        verify(conversationRepository)
                .save(conversationCaptor.capture());

        assertThat(conversationCaptor.getValue().getHost())
                .isSameAs(activeHost);
        assertThat(response.getConversation().getHostId())
                .isEqualTo(activeHost.getId());
        assertThat(response.getConversation().getVerificationStatus())
                .isNull();
        assertThat(response.getMessages())
                .isEmpty();
    }

    /**
     * PENDING verification still allows support messaging, proving this service
     * is not reusing the verified-host business gate from HostAccessService.
     */
    @Test
    void pendingHostCanAccessSupportThreadWithoutBusinessVerification() {
        HostSupportConversation conversation =
                conversation(701L, activeHost);

        when(conversationRepository.findByHost(activeHost))
                .thenReturn(Optional.of(conversation));
        when(hostVerificationRepository.findByHost(activeHost))
                .thenReturn(Optional.of(
                        verification(
                                activeHost,
                                HostVerificationStatus.PENDING
                        )
                ));

        HostSupportConversationDetailResponse response =
                service.getHostConversation(
                        authenticationFor(activeHost)
                );

        assertThat(response.getConversation().getId())
                .isEqualTo(conversation.getId());
        assertThat(response.getConversation().getVerificationStatus())
                .isEqualTo(HostVerificationStatus.PENDING);
    }

    /**
     * VERIFIED Hosts use the same support thread contract; verification status
     * is informational here and does not change messaging behavior.
     */
    @Test
    void verifiedHostCanAccessSupportThread() {
        HostSupportConversation conversation =
                conversation(702L, verifiedHost);

        when(conversationRepository.findByHost(verifiedHost))
                .thenReturn(Optional.of(conversation));
        when(hostVerificationRepository.findByHost(verifiedHost))
                .thenReturn(Optional.of(
                        verification(
                                verifiedHost,
                                HostVerificationStatus.VERIFIED
                        )
                ));

        HostSupportConversationDetailResponse response =
                service.getHostConversation(
                        authenticationFor(verifiedHost)
                );

        assertThat(response.getConversation().getHostId())
                .isEqualTo(verifiedHost.getId());
        assertThat(response.getConversation().getVerificationStatus())
                .isEqualTo(HostVerificationStatus.VERIFIED);
    }

    /**
     * Admin can initiate the support thread for an unverified Host before that
     * Host sends the first message. The target Host is resolved by id and then
     * passed through the shared one-thread-per-Host creation path.
     */
    @Test
    void adminCanCreateSupportConversationForUnverifiedHost() {
        when(conversationRepository.findByHost(activeHost))
                .thenReturn(Optional.empty());

        HostSupportConversationDetailResponse response =
                service.createOrGetAdminConversationForHost(
                        activeHost.getId(),
                        authenticationFor(admin)
                );

        ArgumentCaptor<HostSupportConversation> conversationCaptor =
                ArgumentCaptor.forClass(
                        HostSupportConversation.class
                );

        verify(conversationRepository)
                .save(conversationCaptor.capture());

        assertThat(conversationCaptor.getValue().getHost())
                .isSameAs(activeHost);
        assertThat(response.getConversation().getHostId())
                .isEqualTo(activeHost.getId());
        assertThat(response.getMessages())
                .isEmpty();
    }

    /**
     * VERIFIED Hosts use the same Admin create-or-get endpoint; verification is
     * informational in the DTO and is not required for thread creation.
     */
    @Test
    void adminCanCreateOrGetSupportConversationForVerifiedHost() {
        when(conversationRepository.findByHost(verifiedHost))
                .thenReturn(Optional.empty());
        when(hostVerificationRepository.findByHost(verifiedHost))
                .thenReturn(Optional.of(
                        verification(
                                verifiedHost,
                                HostVerificationStatus.VERIFIED
                        )
                ));

        HostSupportConversationDetailResponse response =
                service.createOrGetAdminConversationForHost(
                        verifiedHost.getId(),
                        authenticationFor(admin)
                );

        assertThat(response.getConversation().getHostId())
                .isEqualTo(verifiedHost.getId());
        assertThat(response.getConversation().getVerificationStatus())
                .isEqualTo(HostVerificationStatus.VERIFIED);
    }

    /**
     * Repeated Admin initiation for the same Host reuses the repository result
     * instead of inserting another support conversation.
     */
    @Test
    void repeatedAdminCreateOrGetReturnsSameConversation() {
        HostSupportConversation existingConversation =
                conversation(708L, activeHost);

        when(conversationRepository.findByHost(activeHost))
                .thenReturn(Optional.of(existingConversation));

        HostSupportConversationDetailResponse firstResponse =
                service.createOrGetAdminConversationForHost(
                        activeHost.getId(),
                        authenticationFor(admin)
                );
        HostSupportConversationDetailResponse secondResponse =
                service.createOrGetAdminConversationForHost(
                        activeHost.getId(),
                        authenticationFor(admin)
                );

        assertThat(firstResponse.getConversation().getId())
                .isEqualTo(existingConversation.getId());
        assertThat(secondResponse.getConversation().getId())
                .isEqualTo(existingConversation.getId());
        verify(conversationRepository, never())
                .save(any(HostSupportConversation.class));
    }

    /**
     * Admin cannot create a Host support conversation for a guest account. This
     * keeps the support model strictly tied to Host verification work.
     */
    @Test
    void userIdIsRejectedForAdminCreateOrGet() {
        assertThatThrownBy(() ->
                service.createOrGetAdminConversationForHost(
                        guest.getId(),
                        authenticationFor(admin)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Host account required");

        verify(conversationRepository, never())
                .findByHost(guest);
    }

    /**
     * Admin cannot target another Admin account as a Host support participant.
     */
    @Test
    void adminIdIsRejectedForAdminCreateOrGet() {
        assertThatThrownBy(() ->
                service.createOrGetAdminConversationForHost(
                        admin.getId(),
                        authenticationFor(admin)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Host account required");

        verify(conversationRepository, never())
                .findByHost(admin);
    }

    /**
     * Missing target ids fail before any conversation lookup or insert, giving
     * the frontend a predictable validation error.
     */
    @Test
    void nonexistentHostIdIsRejectedForAdminCreateOrGet() {
        Long missingHostId = 404L;

        when(userRepository.findById(missingHostId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createOrGetAdminConversationForHost(
                        missingHostId,
                        authenticationFor(admin)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Host not found");

        verify(conversationRepository, never())
                .findByHost(any(User.class));
    }

    /**
     * A thread first created by the Host is still the same thread Admin receives
     * from the create-or-get endpoint.
     */
    @Test
    void existingHostCreatedConversationIsReusedByAdmin() {
        HostSupportConversation hostCreatedConversation =
                conversation(709L, activeHost);

        when(conversationRepository.findByHost(activeHost))
                .thenReturn(Optional.of(hostCreatedConversation));

        HostSupportConversationDetailResponse response =
                service.createOrGetAdminConversationForHost(
                        activeHost.getId(),
                        authenticationFor(admin)
                );

        assertThat(response.getConversation().getId())
                .isEqualTo(hostCreatedConversation.getId());
        assertThat(response.getConversation().getHostId())
                .isEqualTo(activeHost.getId());
        verify(conversationRepository, never())
                .save(any(HostSupportConversation.class));
    }

    /**
     * Host-facing support APIs never accept a raw conversation id. If a Host
     * tries to use the Admin id-based path, service authorization rejects it
     * before the conversation can be loaded.
     */
    @Test
    void hostCannotUseAdminLookupToAccessAnotherHostsConversation() {
        assertThatThrownBy(() ->
                service.getAdminConversation(
                        900L,
                        authenticationFor(activeHost)
                )
        ).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Admin account required");

        verify(conversationRepository, never())
                .findById(900L);
    }

    /**
     * Admin inbox rows include Host identity, verification status, preview data,
     * and unread Host-message count.
     */
    @Test
    void adminCanListSupportConversations() {
        HostSupportConversation conversation =
                conversation(703L, activeHost);
        HostSupportMessage lastMessage =
                message(
                        803L,
                        conversation,
                        activeHost,
                        "Please review my uploaded document."
                );

        when(conversationRepository.findAllByOrderByUpdatedAtDesc())
                .thenReturn(List.of(conversation));
        when(hostVerificationRepository.findByHost(activeHost))
                .thenReturn(Optional.of(
                        verification(
                                activeHost,
                                HostVerificationStatus.REJECTED
                        )
                ));
        when(messageRepository.findTopByConversationOrderByCreatedAtDesc(
                conversation
        )).thenReturn(Optional.of(lastMessage));
        when(messageRepository.countByConversationAndSender_RoleAndReadFalse(
                conversation,
                Role.HOST
        )).thenReturn(2L);

        List<HostSupportConversationResponse> responses =
                service.getAdminConversations(
                        authenticationFor(admin)
                );

        assertThat(responses)
                .hasSize(1);
        assertThat(responses.getFirst().getHostId())
                .isEqualTo(activeHost.getId());
        assertThat(responses.getFirst().getVerificationStatus())
                .isEqualTo(HostVerificationStatus.REJECTED);
        assertThat(responses.getFirst().getLastMessagePreview())
                .isEqualTo("Please review my uploaded document.");
        assertThat(responses.getFirst().getUnreadCount())
                .isEqualTo(2L);
    }

    /**
     * Admin detail view can open an existing support thread and receives
     * messages in repository-provided chronological order.
     */
    @Test
    void adminCanOpenHostSupportConversation() {
        HostSupportConversation conversation =
                conversation(704L, activeHost);
        HostSupportMessage firstMessage =
                message(804L, conversation, activeHost, "Hello admin");
        HostSupportMessage secondMessage =
                message(805L, conversation, admin, "Hello host");

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationOrderByCreatedAtAsc(
                conversation
        )).thenReturn(List.of(firstMessage, secondMessage));

        HostSupportConversationDetailResponse response =
                service.getAdminConversation(
                        conversation.getId(),
                        authenticationFor(admin)
                );

        assertThat(response.getConversation().getId())
                .isEqualTo(conversation.getId());
        assertThat(response.getMessages())
                .extracting(HostSupportMessageResponse::getId)
                .containsExactly(
                        firstMessage.getId(),
                        secondMessage.getId()
                );
    }

    /**
     * Host message sending derives the sender from authentication, trims the
     * body, and writes into that Host's own support conversation.
     */
    @Test
    void hostCanSendMessage() {
        when(conversationRepository.findByHost(activeHost))
                .thenReturn(Optional.empty());

        HostSupportMessageResponse response =
                service.sendHostMessage(
                        sendMessageRequest("  Please help  "),
                        authenticationFor(activeHost)
                );

        ArgumentCaptor<HostSupportMessage> messageCaptor =
                ArgumentCaptor.forClass(
                        HostSupportMessage.class
                );

        verify(messageRepository)
                .save(messageCaptor.capture());

        HostSupportMessage savedMessage =
                messageCaptor.getValue();

        assertThat(savedMessage.getSender())
                .isSameAs(activeHost);
        assertThat(savedMessage.getBody())
                .isEqualTo("Please help");
        assertThat(savedMessage.getConversation().getHost())
                .isSameAs(activeHost);
        assertThat(response.getSenderRole())
                .isEqualTo(Role.HOST);
    }

    /**
     * Admin message sending derives the Admin sender from authentication and
     * writes only into an existing support conversation id.
     */
    @Test
    void adminCanSendMessage() {
        HostSupportConversation conversation =
                conversation(705L, activeHost);

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));

        HostSupportMessageResponse response =
                service.sendAdminMessage(
                        conversation.getId(),
                        sendMessageRequest("  Please upload a clearer ID  "),
                        authenticationFor(admin)
                );

        ArgumentCaptor<HostSupportMessage> messageCaptor =
                ArgumentCaptor.forClass(
                        HostSupportMessage.class
                );

        verify(messageRepository)
                .save(messageCaptor.capture());

        assertThat(messageCaptor.getValue().getSender())
                .isSameAs(admin);
        assertThat(messageCaptor.getValue().getBody())
                .isEqualTo("Please upload a clearer ID");
        assertThat(response.getSenderRole())
                .isEqualTo(Role.ADMIN);
    }

    /**
     * Blank support messages are rejected before a conversation or message is
     * created, preventing empty support threads from validation failures.
     */
    @Test
    void blankMessageIsRejected() {
        assertThatThrownBy(() ->
                service.sendHostMessage(
                        sendMessageRequest("   "),
                        authenticationFor(activeHost)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Message body cannot be blank");

        verifyNoInteractions(conversationRepository);
        verifyNoInteractions(messageRepository);
    }

    /**
     * Host mark-read only affects Admin-authored unread messages. The Host's
     * own outgoing messages remain untouched.
     */
    @Test
    void hostMarkReadOnlyMarksAdminMessages() {
        HostSupportConversation conversation =
                conversation(706L, activeHost);
        HostSupportMessage adminMessage =
                message(806L, conversation, admin, "Admin note");
        HostSupportMessage hostMessage =
                message(807L, conversation, activeHost, "Host reply");

        when(conversationRepository.findByHost(activeHost))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationAndSender_RoleAndReadFalse(
                conversation,
                Role.ADMIN
        )).thenReturn(List.of(adminMessage));

        service.markHostConversationRead(
                authenticationFor(activeHost)
        );

        assertThat(adminMessage.isRead())
                .isTrue();
        assertThat(adminMessage.getReadAt())
                .isNotNull();
        assertThat(hostMessage.isRead())
                .isFalse();
        verify(messageRepository)
                .saveAll(List.of(adminMessage));
    }

    /**
     * Admin mark-read only affects Host-authored unread messages. Admin replies
     * remain unread for the Host until the Host reads them.
     */
    @Test
    void adminMarkReadOnlyMarksHostMessages() {
        HostSupportConversation conversation =
                conversation(707L, activeHost);
        HostSupportMessage hostMessage =
                message(808L, conversation, activeHost, "Host question");
        HostSupportMessage adminMessage =
                message(809L, conversation, admin, "Admin answer");

        when(conversationRepository.findById(conversation.getId()))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationAndSender_RoleAndReadFalse(
                conversation,
                Role.HOST
        )).thenReturn(List.of(hostMessage));

        service.markAdminConversationRead(
                conversation.getId(),
                authenticationFor(admin)
        );

        assertThat(hostMessage.isRead())
                .isTrue();
        assertThat(hostMessage.getReadAt())
                .isNotNull();
        assertThat(adminMessage.isRead())
                .isFalse();
        verify(messageRepository)
                .saveAll(List.of(hostMessage));
    }

    private SendMessageRequest sendMessageRequest(
            String body
    ) {
        SendMessageRequest request =
                new SendMessageRequest();

        request.setBody(body);

        return request;
    }

    private HostSupportConversation conversation(
            Long id,
            User host
    ) {
        return HostSupportConversation
                .builder()
                .id(id)
                .host(host)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private HostSupportMessage message(
            Long id,
            HostSupportConversation conversation,
            User sender,
            String body
    ) {
        return HostSupportMessage
                .builder()
                .id(id)
                .conversation(conversation)
                .sender(sender)
                .body(body)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private HostVerification verification(
            User host,
            HostVerificationStatus status
    ) {
        return HostVerification
                .builder()
                .id(300L)
                .host(host)
                .status(status)
                .build();
    }

    private User user(
            Long id,
            String email,
            String firstName,
            String lastName,
            Role role,
            AccountStatus accountStatus
    ) {
        return User
                .builder()
                .id(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password("encoded-password")
                .role(role)
                .accountStatus(accountStatus)
                .profileImageUrl("https://example.test/profile.jpg")
                .build();
    }

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
}
