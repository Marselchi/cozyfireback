package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.notifications.dto.NotificationMapper;
import com.cozyfireplace.server.notifications.dto.NotificationResponse;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationInboxService}.
 * <p>
 * The repository and {@link NotificationMapper} are mocked. Covers the inbox listing projection,
 * the unread counter, the mark-as-read flow (which stamps {@code readAt} before projecting),
 * bulk read, and single/bulk deletion — including the ownership-scoped not-found guards.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationInboxService")
class NotificationInboxServiceTest {

    private static final Long ACCOUNT_ID = 7L;
    private static final Long NOTIFICATION_ID = 500L;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationInboxService service;

    private Account account() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(ACCOUNT_ID);
        return account;
    }

    @Test
    @DisplayName("getRoomNotifications projects the account's inbox newest-first")
    void list() {
        Account account = account();
        Notification notification = mock(Notification.class);
        when(notificationRepository.findAllByAccountIdOrderByCreatedAtDesc(ACCOUNT_ID))
                .thenReturn(List.of(notification));
        NotificationResponse projected = mock(NotificationResponse.class);
        when(notificationMapper.toDto(notification)).thenReturn(projected);

        assertThat(service.getRoomNotifications(account)).containsExactly(projected);
    }

    @Test
    @DisplayName("unreadCount delegates the null-read counter")
    void unreadCount() {
        Account account = account();
        when(notificationRepository.countByAccountIdAndReadAtNull(ACCOUNT_ID)).thenReturn(3L);

        assertThat(service.unreadCount(account)).isEqualTo(3L);
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("stamps the read time and projects the notification")
        void stampsAndProjects() {
            Account account = account();
            Notification notification = Notification.builder().id(NOTIFICATION_ID).build();
            when(notificationRepository.findByIdAndAccountId(NOTIFICATION_ID, ACCOUNT_ID))
                    .thenReturn(Optional.of(notification));
            NotificationResponse projected = mock(NotificationResponse.class);
            when(notificationMapper.toDto(notification)).thenReturn(projected);

            assertThat(service.markAsRead(NOTIFICATION_ID, account)).isSameAs(projected);
            assertThat(notification.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("throws NotFound when the notification is not owned by the account")
        void notFound() {
            Account account = account();
            when(notificationRepository.findByIdAndAccountId(NOTIFICATION_ID, ACCOUNT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markAsRead(NOTIFICATION_ID, account))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(notificationMapper);
        }
    }

    @Test
    @DisplayName("markAllAsRead bulk-updates with a timestamp")
    void markAllAsRead() {
        Account account = account();

        service.markAllAsRead(account);

        verify(notificationRepository).markAllAsRead(eq(ACCOUNT_ID), any(Instant.class));
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("removes an owned notification")
        void removes() {
            Account account = account();
            Notification notification = mock(Notification.class);
            when(notificationRepository.findByIdAndAccountId(NOTIFICATION_ID, ACCOUNT_ID))
                    .thenReturn(Optional.of(notification));

            service.delete(NOTIFICATION_ID, account);

            verify(notificationRepository).delete(notification);
        }

        @Test
        @DisplayName("throws NotFound and skips deletion for a foreign notification")
        void notFound() {
            Account account = account();
            when(notificationRepository.findByIdAndAccountId(NOTIFICATION_ID, ACCOUNT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(NOTIFICATION_ID, account))
                    .isInstanceOf(NotFoundException.class);
            verify(notificationRepository, never()).delete(any());
        }
    }

    @Test
    @DisplayName("deleteAll clears the account inbox")
    void deleteAll() {
        Account account = account();

        service.deleteAll(account);

        verify(notificationRepository).deleteAllByAccountId(ACCOUNT_ID);
    }
}
