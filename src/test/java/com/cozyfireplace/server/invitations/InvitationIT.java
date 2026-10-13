package com.cozyfireplace.server.invitations;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.invitations.dto.InvitationRequest;
import com.cozyfireplace.server.rooms.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link InvitationController} — {@code /api/v1/invitations}.
 *
 * <p>Invitations are short-lived join codes bound to a room. None of the endpoints take a
 * {@code @CurrentAccount} parameter — they are simply authentication-gated — except that the
 * "use" flow internally resolves the acting profile to mint a new room account.</p>
 *
 * <p><b>Documented status map</b> (consistent across the whole IT suite):
 * <ul>
 *   <li>anonymous request → 403;</li>
 *   <li>create for a real room → 200 with an 8-character code; for an unknown room the lazy
 *       {@code getReferenceById} proxy is only materialised on insert, so the FK violation
 *       surfaces as 500 (pinned — <em>not</em> 404);</li>
 *   <li>use with an unknown code → 404 (service {@code NotFoundException});</li>
 *   <li>delete is a bare {@code deleteById} and is therefore lenient: unknown id → 204 (pinned);</li>
 *   <li>list returns only the given room's invitations.</li>
 * </ul>
 */
@DisplayName("InvitationIT — /api/v1/invitations")
class InvitationIT extends BaseIntegrationTest {

    @Autowired
    InvitationRepository invitationRepository;
    @Autowired
    AccountRepository accountRepository;

    Profile creatorProfile;
    Room room;

    @BeforeEach
    void seedRoom() {
        creatorProfile = newProfile("dm");
        room = newRoomOwnedBy(creatorProfile, "Tavern");
    }

    /** Creates an invitation via the API and returns the generated code (200 expected). */
    private String createCode(Long roomId) throws Exception {
        authenticateAs(creatorProfile);
        MvcResult result = mockMvc.perform(post("/api/v1/invitations/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("code").asString();
    }

    @Nested
    @DisplayName("POST /api/v1/invitations/{roomId} — create")
    class CreateTests {

        @Test
        @DisplayName("generates an 8-character code for the room")
        void create_success() throws Exception {
            String code = createCode(room.getId());

            assertThat(code).hasSize(8);
            assertThat(invitationRepository.findByCode(code)).isPresent();
        }

        @Test
        @DisplayName("unknown room id → 500 (FK violation on insert, pinned — not 404)")
        void create_unknownRoom_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/invitations/{roomId}", 9_999_999L))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("anonymous create → 403")
        void create_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(post("/api/v1/invitations/{roomId}", room.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/invitations/use — redeem")
    class UseTests {

        @Test
        @DisplayName("redeeming a valid code mints an account for the acting profile in the room")
        void use_validCode_joinsRoom() throws Exception {
            String code = createCode(room.getId());

            Profile joiner = newProfile("joiner");
            authenticateAs(joiner);
            mockMvc.perform(post("/api/v1/invitations/use")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new InvitationRequest(code))))
                    .andExpect(status().isNoContent());

            assertThat(accountRepository.findByProfileAndRoomId(joiner, room.getId())).isPresent();
        }

        @Test
        @DisplayName("unknown code → 404")
        void use_unknownCode_returns404() throws Exception {
            authenticateAs(newProfile("joiner"));
            mockMvc.perform(post("/api/v1/invitations/use")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new InvitationRequest("nosuchcode"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("anonymous use → 403")
        void use_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(post("/api/v1/invitations/use")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new InvitationRequest("whatever"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/invitations/{roomId}/all — list")
    class ListTests {

        @Test
        @DisplayName("returns every invitation belonging to the room")
        void list_success() throws Exception {
            createCode(room.getId());
            createCode(room.getId());

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/invitations/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("empty for a room without invitations")
        void list_empty() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/invitations/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/invitations/{invitationId}")
    class DeleteTests {

        @Test
        @DisplayName("revokes an existing invitation → 204")
        void delete_success() throws Exception {
            authenticateAs(creatorProfile);
            MvcResult result = mockMvc.perform(post("/api/v1/invitations/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andReturn();
            long id = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .path("id").asLong();

            mockMvc.perform(delete("/api/v1/invitations/{invitationId}", id))
                    .andExpect(status().isNoContent());

            assertThat(invitationRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("unknown id → 204 (bare deleteById is lenient, pinned)")
        void delete_unknown_returns204_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/invitations/{invitationId}", 9_999_999L))
                    .andExpect(status().isNoContent());
        }
    }
}
