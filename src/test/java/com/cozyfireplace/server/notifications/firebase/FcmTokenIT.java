package com.cozyfireplace.server.notifications.firebase;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.notifications.firebase.dto.RegisterFcmFidRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link FcmTokenController} — {@code /api/v1/fcm}.
 *
 * <p>FCM device tokens are owned by the authenticated {@link Profile} (resolved from the
 * {@code SecurityContext} principal, not a room {@code @CurrentAccount}). Registering a token is
 * an upsert keyed on the unique {@code token} string: a fresh token is created and bound to the
 * acting profile, an already-known token is re-activated and re-bound. Deleting simply flips
 * {@code active} to {@code false}. Both endpoints return {@code void} → 200.</p>
 *
 * <p><b>Documented status map</b>:
 * <ul>
 *   <li>anonymous request → 403 (endpoints are authentication-gated);</li>
 *   <li>register/delete happy path → 200;</li>
 *   <li>delete of an unknown token is a no-op → 200 (uses {@code ifPresent});</li>
 *   <li>register with a {@code null} token violates the NOT NULL/unique column → 500 (pinned).</li>
 * </ul>
 */
@DisplayName("FcmTokenIT — /api/v1/fcm")
class FcmTokenIT extends BaseIntegrationTest {

    @Autowired
    FcmTokenRepository fcmTokenRepository;

    Profile profile;

    @BeforeEach
    void seedProfile() {
        profile = newProfile("device");
    }

    @Nested
    @DisplayName("POST /api/v1/fcm/push-token — register")
    class RegisterTests {

        @Test
        @DisplayName("registers a new token bound to the acting profile → 200")
        void register_newToken() throws Exception {
            authenticateAs(profile);
            mockMvc.perform(post("/api/v1/fcm/push-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new RegisterFcmFidRequest("device-token-abc"))))
                    .andExpect(status().isOk());

            FcmToken saved = fcmTokenRepository.findByFid("device-token-abc").orElseThrow();
            assertThat(saved.isActive()).isTrue();
            assertThat(saved.getProfile().getId()).isEqualTo(profile.getId());
        }

        @Test
        @DisplayName("re-registering a known token re-activates and re-binds it (no duplicate)")
        void register_existingToken_reactivates() throws Exception {
            FcmToken stale = fcmTokenRepository.save(FcmToken.builder()
                    .profile(profile).fid("shared-token").active(false)
                    .createdAt(java.time.Instant.now()).build());

            Profile other = newProfile("other");
            authenticateAs(other);
            mockMvc.perform(post("/api/v1/fcm/push-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new RegisterFcmFidRequest("shared-token"))))
                    .andExpect(status().isOk());

            FcmToken reloaded = fcmTokenRepository.findById(stale.getId()).orElseThrow();
            assertThat(reloaded.isActive()).isTrue();
            assertThat(reloaded.getProfile().getId()).isEqualTo(other.getId());
            assertThat(fcmTokenRepository.findByFid("shared-token")).isPresent();
        }

        @Test
        @DisplayName("null token violates the NOT NULL column → 500 (pinned)")
        void register_nullToken_returns500_currentBehaviour() throws Exception {
            authenticateAs(profile);
            mockMvc.perform(post("/api/v1/fcm/push-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new RegisterFcmFidRequest(null))))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("anonymous register → 403")
        void register_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(post("/api/v1/fcm/push-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new RegisterFcmFidRequest("x"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/fcm/push-token — deactivate")
    class DeleteTests {

        @Test
        @DisplayName("deactivates a known token → 200 (active=false)")
        void delete_success() throws Exception {
            fcmTokenRepository.save(FcmToken.builder()
                    .profile(profile).fid("to-revoke").active(true)
                    .createdAt(java.time.Instant.now()).build());

            authenticateAs(profile);
            mockMvc.perform(delete("/api/v1/fcm/push-token").param("fid", "to-revoke"))
                    .andExpect(status().isOk());

            assertThat(fcmTokenRepository.findByFid("to-revoke").orElseThrow().isActive()).isFalse();
        }

        @Test
        @DisplayName("deleting an unknown token is a no-op → 200")
        void delete_unknown_returns200_currentBehaviour() throws Exception {
            authenticateAs(profile);
            mockMvc.perform(delete("/api/v1/fcm/push-token").param("fid", "never-registered"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("anonymous delete → 403")
        void delete_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(delete("/api/v1/fcm/push-token").param("fid", "x"))
                    .andExpect(status().isForbidden());
        }
    }
}
