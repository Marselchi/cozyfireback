package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.notifications.dto.NotificationChannel;
import com.cozyfireplace.server.notifications.dto.NotificationEventCode;
import com.cozyfireplace.server.notifications.dto.NotificationPreferenceRequest;
import com.cozyfireplace.server.notifications.dto.NotificationType;
import com.cozyfireplace.server.rooms.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link NotificationController} — {@code /api/v1/notifications}.
 *
 * <p>The inbox is scoped to the acting {@code @CurrentAccount} (a room account); notifications are
 * delivered by event listeners that only fire on a committed transaction, so these tests seed
 * rows directly through {@link NotificationRepository}. Per-event delivery preferences are stored
 * per (account, eventCode, channel) and are replaced wholesale with an upsert.</p>
 *
 * <p><b>Documented status map</b> (consistent across the whole IT suite):
 * <ul>
 *   <li>every endpoint takes {@code @CurrentAccount} → anonymous → 403, stranger (no account in
 *       the room) → 500 (unmapped resolver {@code AccessDeniedException});</li>
 *   <li>{@code read} / {@code delete} of a notification that does not belong to the acting
 *       account (or is unknown) → 404;</li>
 *   <li>{@code read-all} / {@code delete-all} / {@code preferences} save → 204;</li>
 *   <li>{@code unread-count} returns a bare JSON number.</li>
 * </ul>
 */
@DisplayName("NotificationIT — /api/v1/notifications")
class NotificationIT extends BaseIntegrationTest {

    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    NotificationPreferenceRepository preferenceRepository;

    Profile creatorProfile;
    Room room;
    Profile memberProfile;
    Account memberAccount;
    Account otherAccount;

    @BeforeEach
    void seedRoom() {
        creatorProfile = newProfile("dm");
        room = newRoomOwnedBy(creatorProfile, "Bell Room");
        memberProfile = newProfile("player");
        memberAccount = newAccount(memberProfile, room, "Player One");
        otherAccount = newAccount(newProfile("other"), room, "Player Two");
    }

    private Notification newNotification(Account account, boolean read) {
        return notificationRepository.save(Notification.builder()
                .account(account)
                .type(NotificationType.QA)
                .eventCode(NotificationEventCode.QA_ANSWERED)
                .title("New answer")
                .body("Someone answered your question")
                .route("/room/1/q/2")
                .readAt(read ? Instant.now() : null)
                .build());
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/{roomId} — inbox")
    class InboxTests {

        @Test
        @DisplayName("returns only the acting account's notifications")
        void list_scopedToAccount() throws Exception {
            newNotification(memberAccount, false);
            newNotification(memberAccount, true);
            newNotification(otherAccount, false);

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/notifications/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].accountId").value(memberAccount.getId()))
                    .andExpect(jsonPath("$[0].eventCode").value("QA_ANSWERED"));
        }

        @Test
        @DisplayName("empty inbox → empty list")
        void list_empty() throws Exception {
            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/notifications/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("unread-count returns the number of unread rows")
        void unreadCount() throws Exception {
            newNotification(memberAccount, false);
            newNotification(memberAccount, false);
            newNotification(memberAccount, true);

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/notifications/{roomId}/unread-count", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("2"));
        }
    }

    @Nested
    @DisplayName("POST read endpoints")
    class ReadMarkingTests {

        @Test
        @DisplayName("markAsRead stamps readAt and returns the notification → 200")
        void markAsRead_success() throws Exception {
            Notification n = newNotification(memberAccount, false);

            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/notifications/{roomId}/{notificationId}/read", room.getId(), n.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(n.getId()))
                    .andExpect(jsonPath("$.readAt").exists());

            assertThat(notificationRepository.findById(n.getId()).orElseThrow().getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("markAsRead on another account's notification → 404")
        void markAsRead_foreign_returns404() throws Exception {
            Notification other = newNotification(otherAccount, false);

            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/notifications/{roomId}/{notificationId}/read", room.getId(), other.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("markAsRead on an unknown id → 404")
        void markAsRead_unknown_returns404() throws Exception {
            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/notifications/{roomId}/{notificationId}/read", room.getId(), 9_999_999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("markAllAsRead clears every unread row for the account → 204")
        void markAllAsRead_success() throws Exception {
            newNotification(memberAccount, false);
            newNotification(memberAccount, false);

            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/notifications/{roomId}/read-all", room.getId()))
                    .andExpect(status().isNoContent());

            assertThat(notificationRepository.countByAccountIdAndReadAtNull(memberAccount.getId())).isZero();
        }
    }

    @Nested
    @DisplayName("DELETE inbox endpoints")
    class DeleteTests {

        @Test
        @DisplayName("delete removes the account's notification → 204")
        void delete_success() throws Exception {
            Notification n = newNotification(memberAccount, false);

            authenticateAs(memberProfile);
            mockMvc.perform(delete("/api/v1/notifications/{roomId}/{notificationId}", room.getId(), n.getId()))
                    .andExpect(status().isNoContent());

            assertThat(notificationRepository.findById(n.getId())).isEmpty();
        }

        @Test
        @DisplayName("delete of another account's notification → 404")
        void delete_foreign_returns404() throws Exception {
            Notification other = newNotification(otherAccount, false);

            authenticateAs(memberProfile);
            mockMvc.perform(delete("/api/v1/notifications/{roomId}/{notificationId}", room.getId(), other.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deleteAll clears the whole inbox → 204")
        void deleteAll_success() throws Exception {
            newNotification(memberAccount, false);
            newNotification(memberAccount, true);

            authenticateAs(memberProfile);
            mockMvc.perform(delete("/api/v1/notifications/{roomId}/all", room.getId()))
                    .andExpect(status().isNoContent());

            assertThat(notificationRepository.findAllByAccountIdOrderByCreatedAtDesc(memberAccount.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Preferences endpoints")
    class PreferenceTests {

        @Test
        @DisplayName("replace persists preferences and get reads them back")
        void replaceAndGet() throws Exception {
            List<NotificationPreferenceRequest> prefs = List.of(
                    new NotificationPreferenceRequest(NotificationEventCode.QA_ANSWERED, NotificationChannel.PUSH, true),
                    new NotificationPreferenceRequest(NotificationEventCode.SESSION_CREATED, NotificationChannel.POLLING, false));

            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/notifications/{roomId}/preferences", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(prefs)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/notifications/{roomId}/preferences", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("re-sending the same (eventCode, channel) upserts instead of duplicating")
        void replace_isIdempotent() throws Exception {
            authenticateAs(memberProfile);
            List<NotificationPreferenceRequest> prefs = List.of(
                    new NotificationPreferenceRequest(NotificationEventCode.QA_ANSWERED, NotificationChannel.PUSH, true));
            mockMvc.perform(post("/api/v1/notifications/{roomId}/preferences", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(prefs)))
                    .andExpect(status().isNoContent());
            mockMvc.perform(post("/api/v1/notifications/{roomId}/preferences", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(List.of(
                                    new NotificationPreferenceRequest(NotificationEventCode.QA_ANSWERED, NotificationChannel.PUSH, false)))))
                    .andExpect(status().isNoContent());

            List<NotificationPreference> stored = preferenceRepository.findAllByAccountId(memberAccount.getId());
            assertThat(stored).hasSize(1);
            assertThat(stored.getFirst().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("empty preference list → empty inbox preferences")
        void get_empty() throws Exception {
            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/notifications/{roomId}/preferences", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Security")
    class SecurityTests {

        @Test
        @DisplayName("anonymous inbox → 403")
        void anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(get("/api/v1/notifications/{roomId}", room.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("stranger without an account in the room → 500 (unmapped AccessDeniedException)")
        void stranger_returns500_currentBehaviour() throws Exception {
            authenticateAs(newProfile("stranger"));
            mockMvc.perform(get("/api/v1/notifications/{roomId}", room.getId()))
                    .andExpect(status().isInternalServerError());
        }
    }
}
