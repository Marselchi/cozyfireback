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
 * <p>Invitations are short-lived join codes bound to a room. No handler takes a
 * {@code @CurrentAccount} parameter — they are simply authentication-gated — but every
 * mutation (and the list) calls {@code RoomSecurityService#isCreator(roomId)}, which
 * internally resolves the acting profile's account <em>in the room from the path</em>.
 * The "use" flow then resolves the acting profile to mint a new room account.</p>
 *
 * <p><b>Documented status map</b> (consistent across the whole IT suite):
 * <ul>
 *   <li>anonymous request → 403;</li>
 *   <li>create for an unknown room → 404 ({@code findById} is checked up front now, so the
 *       old FK-violation 500 is gone);</li>
 *   <li>any room-scoped call by a profile without an account in that room → 403
 *       ({@code AccessDeniedException} from the resolver, mapped by
 *       {@code GlobalExceptionHandler});</li>
 *   <li>room member who is not the creator → 403 (creator-only gate on create / list / delete);</li>
 *   <li>use with an unknown code → 404 (service {@code NotFoundException});</li>
 *   <li>delete resolves the invitation inside the path room ({@code findByRoomIdAndId}) →
 *       an unknown id, or one belonging to another room, is a 404, never a silent no-op;</li>
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
    Profile memberProfile;

    @BeforeEach
    void seedRoom() {
        creatorProfile = newProfile("dm");
        room = newRoomOwnedBy(creatorProfile, "Tavern");
        memberProfile = newProfile("player");
        newAccount(memberProfile, room, "Player One");
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
        @DisplayName("unknown room id → 404 (the room is looked up before the insert)")
        void create_unknownRoom_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/invitations/{roomId}", 9_999_999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("room member who is not the creator → 403 (creator-only gate)")
        void create_byMember_returns403() throws Exception {
            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/invitations/{roomId}", room.getId()))
                    .andExpect(status().isForbidden());

            assertThat(invitationRepository.findAllByRoomId(room.getId())).isEmpty();
        }

        @Test
        @DisplayName("profile without an account in the room → 403 (resolver AccessDeniedException)")
        void create_byStranger_returns403() throws Exception {
            authenticateAs(newProfile("stranger"));
            mockMvc.perform(post("/api/v1/invitations/{roomId}", room.getId()))
                    .andExpect(status().isForbidden());
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

        @Test
        @DisplayName("room member who is not the creator → 403 (invitation codes are creator-only)")
        void list_byMember_returns403() throws Exception {
            createCode(room.getId());

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/invitations/{roomId}/all", room.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/invitations/{roomId}/{invitationId}")
    class DeleteTests {

        /** Creates an invitation via the API and returns its id (200 expected). */
        private long createId() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/invitations/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andReturn();
            return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .path("id").asLong();
        }

        @Test
        @DisplayName("revokes an existing invitation → 204")
        void delete_success() throws Exception {
            authenticateAs(creatorProfile);
            long id = createId();

            mockMvc.perform(delete("/api/v1/invitations/{roomId}/{invitationId}", room.getId(), id))
                    .andExpect(status().isNoContent());

            assertThat(invitationRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("unknown id → 404 (the delete resolves the invitation inside the path room)")
        void delete_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/invitations/{roomId}/{invitationId}", room.getId(), 9_999_999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("an invitation from another room → 404 and survives (no cross-room revoke)")
        void delete_otherRoomInvitation_returns404() throws Exception {
            authenticateAs(creatorProfile);
            long id = createId();
            Room otherRoom = newRoomOwnedBy(creatorProfile, "Other Tavern");

            mockMvc.perform(delete("/api/v1/invitations/{roomId}/{invitationId}", otherRoom.getId(), id))
                    .andExpect(status().isNotFound());

            assertThat(invitationRepository.findById(id)).isPresent();
        }

        @Test
        @DisplayName("room member who is not the creator → 403 and the invitation survives")
        void delete_byMember_returns403() throws Exception {
            authenticateAs(creatorProfile);
            long id = createId();

            authenticateAs(memberProfile);
            mockMvc.perform(delete("/api/v1/invitations/{roomId}/{invitationId}", room.getId(), id))
                    .andExpect(status().isForbidden());

            assertThat(invitationRepository.findById(id)).isPresent();
        }
    }
}
