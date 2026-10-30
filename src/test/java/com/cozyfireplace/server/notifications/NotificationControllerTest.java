package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.notifications.dto.NotificationPreferenceRequest;
import com.cozyfireplace.server.notifications.dto.NotificationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationController}.
 * <p>
 * Thin facade over the inbox and preference services. Pins the status codes (reads/lists 200,
 * the bulk-read and delete mutations 204), the body passthrough and — notably — that the
 * {@code roomId} path variable is ignored while the resolved account scopes every operation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController")
class NotificationControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long NOTIFICATION_ID = 500L;

    @Mock
    private NotificationInboxService notificationInboxService;
    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @InjectMocks
    private NotificationController controller;

    @Test
    @DisplayName("GET list returns the inbox")
    void getAll() {
        Account account = mock(Account.class);
        List<NotificationResponse> body = List.of(mock(NotificationResponse.class));
        when(notificationInboxService.getRoomNotifications(account)).thenReturn(body);

        ResponseEntity<List<NotificationResponse>> response = controller.getAll(ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("GET unread-count returns the counter")
    void unreadCount() {
        Account account = mock(Account.class);
        when(notificationInboxService.unreadCount(account)).thenReturn(3L);

        ResponseEntity<Long> response = controller.unreadCount(ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(3L);
    }

    @Test
    @DisplayName("POST read marks the notification and returns it")
    void markAsRead() {
        Account account = mock(Account.class);
        NotificationResponse body = mock(NotificationResponse.class);
        when(notificationInboxService.markAsRead(NOTIFICATION_ID, account)).thenReturn(body);

        ResponseEntity<NotificationResponse> response = controller.markAsRead(NOTIFICATION_ID, ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("POST read-all returns 204")
    void markAllAsRead() {
        Account account = mock(Account.class);

        ResponseEntity<Void> response = controller.markAllAsRead(ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(notificationInboxService).markAllAsRead(account);
    }

    @Test
    @DisplayName("DELETE a notification returns 204")
    void delete() {
        Account account = mock(Account.class);

        ResponseEntity<Void> response = controller.delete(NOTIFICATION_ID, ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(notificationInboxService).delete(NOTIFICATION_ID, account);
    }

    @Test
    @DisplayName("DELETE all returns 204")
    void deleteAll() {
        Account account = mock(Account.class);

        ResponseEntity<Void> response = controller.deleteAll(ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(notificationInboxService).deleteAll(account);
    }

    @Test
    @DisplayName("GET preferences returns the list")
    void getPreferences() {
        Account account = mock(Account.class);
        List<NotificationPreferenceRequest> body = List.of(mock(NotificationPreferenceRequest.class));
        when(notificationPreferenceService.getAll(account)).thenReturn(body);

        ResponseEntity<List<NotificationPreferenceRequest>> response = controller.getPreferences(ROOM_ID, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("POST preferences returns 204 and forwards the replacement")
    void replacePreferences() {
        Account account = mock(Account.class);
        List<NotificationPreferenceRequest> request = List.of(mock(NotificationPreferenceRequest.class));

        ResponseEntity<Void> response = controller.replacePreferences(ROOM_ID, request, account);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(notificationPreferenceService).replaceAll(account, request);
    }
}
