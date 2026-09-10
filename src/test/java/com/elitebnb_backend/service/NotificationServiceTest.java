package com.elitebnb_backend.service;

import com.elitebnb_backend.dto.NotificationResponse;
import com.elitebnb_backend.entity.Booking;
import com.elitebnb_backend.entity.Notification;
import com.elitebnb_backend.entity.NotificationType;
import com.elitebnb_backend.entity.Property;
import com.elitebnb_backend.entity.Role;
import com.elitebnb_backend.entity.User;
import com.elitebnb_backend.repository.NotificationRepository;
import com.elitebnb_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    private User guest;
    private User host;
    private Property property;
    private Booking booking;

    @BeforeEach
    void setUp() {
        notificationService =
                new NotificationService(
                        notificationRepository,
                        userRepository
                );

        guest =
                user(
                        1L,
                        "guest@example.test",
                        "Guest",
                        "User",
                        Role.USER
                );
        host =
                user(
                        2L,
                        "host@example.test",
                        "Host",
                        "User",
                        Role.HOST
                );
        property =
                Property
                        .builder()
                        .id(10L)
                        .title("Test Property")
                        .host(host)
                        .build();
        booking =
                Booking
                        .builder()
                        .id(20L)
                        .guest(guest)
                        .property(property)
                        .build();
    }

    /**
     * Normal conversation notifications are persisted with MESSAGE type and
     * exact conversation/message ids so the frontend can deep-link to the chat.
     */
    @Test
    void createsLinkedConversationMessageNotification() {
        notificationService.createConversationMessageNotification(
                host,
                "New message",
                "Guest User sent you a message.",
                100L,
                200L,
                booking,
                property
        );

        ArgumentCaptor<Notification> notificationCaptor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository)
                .save(notificationCaptor.capture());

        Notification notification =
                notificationCaptor.getValue();

        assertThat(notification.getRecipient())
                .isSameAs(host);
        assertThat(notification.getType())
                .isEqualTo(NotificationType.MESSAGE);
        assertThat(notification.getConversationId())
                .isEqualTo(100L);
        assertThat(notification.getMessageId())
                .isEqualTo(200L);
        assertThat(notification.getBooking())
                .isSameAs(booking);
        assertThat(notification.getProperty())
                .isSameAs(property);
        assertThat(notification.isRead())
                .isFalse();
    }

    /**
     * Support message notifications use separate support ids instead of normal
     * conversation ids, keeping C1's separate support model intact.
     */
    @Test
    void createsLinkedHostSupportMessageNotification() {
        notificationService.createHostSupportMessageNotification(
                host,
                "New support message",
                "Admin sent you a verification support message.",
                300L,
                400L
        );

        ArgumentCaptor<Notification> notificationCaptor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository)
                .save(notificationCaptor.capture());

        Notification notification =
                notificationCaptor.getValue();

        assertThat(notification.getRecipient())
                .isSameAs(host);
        assertThat(notification.getType())
                .isEqualTo(NotificationType.HOST_SUPPORT_MESSAGE);
        assertThat(notification.getHostSupportConversationId())
                .isEqualTo(300L);
        assertThat(notification.getHostSupportMessageId())
                .isEqualTo(400L);
        assertThat(notification.getConversationId())
                .isNull();
        assertThat(notification.getMessageId())
                .isNull();
    }

    /**
     * Notification API responses expose both normal and support message
     * linkage while keeping older booking/property fields intact.
     */
    @Test
    void getMyNotificationsMapsMessageLinkageFields() {
        Notification notification =
                notification(
                        1L,
                        guest,
                        NotificationType.MESSAGE,
                        100L,
                        200L,
                        null,
                        null
                );
        notification.setBooking(booking);
        notification.setProperty(property);

        when(userRepository.findByEmail(guest.getEmail()))
                .thenReturn(Optional.of(guest));
        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(guest))
                .thenReturn(List.of(notification));

        List<NotificationResponse> responses =
                notificationService.getMyNotifications(
                        authenticationFor(guest)
                );

        assertThat(responses)
                .hasSize(1);
        assertThat(responses.getFirst().getBookingId())
                .isEqualTo(booking.getId());
        assertThat(responses.getFirst().getPropertyId())
                .isEqualTo(property.getId());
        assertThat(responses.getFirst().getConversationId())
                .isEqualTo(100L);
        assertThat(responses.getFirst().getMessageId())
                .isEqualTo(200L);
        assertThat(responses.getFirst().getHostSupportConversationId())
                .isNull();
        assertThat(responses.getFirst().getHostSupportMessageId())
                .isNull();
    }

    /**
     * Conversation read-sync marks only unread MESSAGE notifications for the
     * exact recipient and conversation returned by the repository query.
     */
    @Test
    void conversationReadSyncMarksOnlyMatchingMessageNotifications() {
        Notification matchingNotification =
                notification(
                        1L,
                        guest,
                        NotificationType.MESSAGE,
                        100L,
                        200L,
                        null,
                        null
                );
        Notification otherConversationNotification =
                notification(
                        2L,
                        guest,
                        NotificationType.MESSAGE,
                        101L,
                        201L,
                        null,
                        null
                );
        Notification systemNotification =
                notification(
                        3L,
                        guest,
                        NotificationType.SYSTEM,
                        null,
                        null,
                        null,
                        null
                );

        when(notificationRepository
                .findByRecipientAndTypeAndConversationIdAndReadFalse(
                        guest,
                        NotificationType.MESSAGE,
                        100L
                ))
                .thenReturn(List.of(matchingNotification));

        notificationService.markConversationMessageNotificationsRead(
                guest,
                100L
        );

        assertThat(matchingNotification.isRead())
                .isTrue();
        assertThat(otherConversationNotification.isRead())
                .isFalse();
        assertThat(systemNotification.isRead())
                .isFalse();
        verify(notificationRepository)
                .saveAll(List.of(matchingNotification));
    }

    /**
     * Re-running read-sync after everything is already read is safe because the
     * repository returns no unread matching notifications.
     */
    @Test
    void conversationReadSyncIsIdempotent() {
        when(notificationRepository
                .findByRecipientAndTypeAndConversationIdAndReadFalse(
                        guest,
                        NotificationType.MESSAGE,
                        100L
                ))
                .thenReturn(List.of());

        notificationService.markConversationMessageNotificationsRead(
                guest,
                100L
        );

        verify(notificationRepository)
                .saveAll(List.of());
    }

    /**
     * Host support read-sync targets only HOST_SUPPORT_MESSAGE rows for the
     * Host's exact support conversation and leaves normal MESSAGE rows alone.
     */
    @Test
    void supportReadSyncMarksOnlyMatchingSupportNotifications() {
        Notification matchingSupportNotification =
                notification(
                        1L,
                        host,
                        NotificationType.HOST_SUPPORT_MESSAGE,
                        null,
                        null,
                        300L,
                        400L
                );
        Notification normalMessageNotification =
                notification(
                        2L,
                        host,
                        NotificationType.MESSAGE,
                        100L,
                        200L,
                        null,
                        null
                );

        when(notificationRepository
                .findByRecipientAndTypeAndHostSupportConversationIdAndReadFalse(
                        host,
                        NotificationType.HOST_SUPPORT_MESSAGE,
                        300L
                ))
                .thenReturn(List.of(matchingSupportNotification));

        notificationService.markHostSupportMessageNotificationsRead(
                host,
                300L
        );

        assertThat(matchingSupportNotification.isRead())
                .isTrue();
        assertThat(normalMessageNotification.isRead())
                .isFalse();
        verify(notificationRepository)
                .saveAll(List.of(matchingSupportNotification));
    }

    private Notification notification(
            Long id,
            User recipient,
            NotificationType type,
            Long conversationId,
            Long messageId,
            Long hostSupportConversationId,
            Long hostSupportMessageId
    ) {
        return Notification
                .builder()
                .id(id)
                .recipient(recipient)
                .title("Notification")
                .message("Notification message")
                .type(type)
                .conversationId(conversationId)
                .messageId(messageId)
                .hostSupportConversationId(hostSupportConversationId)
                .hostSupportMessageId(hostSupportMessageId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private User user(
            Long id,
            String email,
            String firstName,
            String lastName,
            Role role
    ) {
        return User
                .builder()
                .id(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password("encoded-password")
                .role(role)
                .build();
    }

    private Authentication authenticationFor(
            User user
    ) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of()
        );
    }
}
