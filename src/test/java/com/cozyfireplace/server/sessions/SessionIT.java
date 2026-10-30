package com.cozyfireplace.server.sessions;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.sessions.dto.SessionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link SessionController} — {@code /api/v1/sessions}.
 *
 * <p>A session is a scheduled game slot owned by the room <b>creator</b>; only the creator may
 * create/update/delete it. Creating a session with {@code accountIds} opens one pending
 * {@link com.cozyfireplace.server.sessions.participations.Participation} row per invitee
 * ({@code accepted == null}); an invitee then "acknowledges" the invitation with
 * {@code accepted=true/false}.</p>
 *
 * <p><b>Documented status map</b> (consistent across the whole IT suite):
 * <ul>
 *   <li>anonymous request → 403;</li>
 *   <li>create with a stranger (no account in the room) → 403 (the resolver's AccessDeniedException
 *       is mapped by {@code GlobalExceptionHandler}); create by a plain member → 403
 *       (service's {@code ForbiddenException});</li>
 *   <li>{@code time == null} → 400 with {@code $.validationErrors.time} (bean-validation runs
 *       before the {@code @CurrentAccount} resolver);</li>
 *   <li>invite ids that belong to another room → 404;</li>
 *   <li>unknown session id on update/delete/get/acknowledge → 404;</li>
 *   <li>update/delete by a non-creator → 403; acknowledge by a non-invited member → 403.</li>
 * </ul>
 *
 * <p>{@code range}, {@code current-month} and {@code upcoming} read endpoints are time-sensitive;
 * sessions are anchored to {@code Instant.now()} so the fixed windows always contain them.</p>
 */
@DisplayName("SessionIT — /api/v1/sessions")
class SessionIT extends BaseIntegrationTest {

    @Autowired
    SessionRepository sessionRepository;

    Profile creatorProfile;
    Room room;
    Account creatorAccount;
    Profile memberProfile;
    Account memberAccount;

    @BeforeEach
    void seedRoom() {
        creatorProfile = newProfile("dm");
        room = newRoomOwnedBy(creatorProfile, "Game Room");
        creatorAccount = room.getCreator();
        memberProfile = newProfile("player");
        memberAccount = newAccount(memberProfile, room, "Player One");
    }

    private long createViaApi(Profile actor, SessionRequest request) throws Exception {
        authenticateAs(actor);
        MvcResult result = mockMvc.perform(post("/api/v1/sessions/{roomId}", room.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("id").asLong();
    }

    @Nested
    @DisplayName("POST /api/v1/sessions/{roomId} — create")
    class CreateTests {

        @Test
        @DisplayName("creator creates a session, becoming its creator in the response")
        void create_byCreator_success() throws Exception {
            long id = createViaApi(creatorProfile,
                    new SessionRequest(Instant.now().plusSeconds(86_400), "Session 12", Set.of()));

            Session saved = sessionRepository.findById(id).orElseThrow();
            assertThat(saved.getDescription()).isEqualTo("Session 12");
            assertThat(saved.getCreator().getId()).isEqualTo(creatorAccount.getId());
        }

        @Test
        @DisplayName("invite accountIds create pending participations (accepted=null)")
        void create_withInvites_createsParticipations() throws Exception {
            long id = createViaApi(creatorProfile,
                    new SessionRequest(Instant.now().plusSeconds(86_400), "Invited", Set.of(memberAccount.getId())));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/sessions/{sessionId}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.creatorName").value("Creator"))
                    .andExpect(jsonPath("$.participants.length()").value(1))
                    .andExpect(jsonPath("$.participants[0].id").value(memberAccount.getId()))
                    .andExpect(jsonPath("$.participants[0].accepted").doesNotExist());
        }

        @Test
        @DisplayName("a plain member may not create sessions → 403 (ForbiddenException)")
        void create_byMember_returns403() throws Exception {
            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/sessions/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SessionRequest(Instant.now(), "Nope", Set.of()))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("null time fails bean validation → 400 validationErrors.time")
        void create_nullTime_returns400() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/sessions/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SessionRequest(null, "No time", Set.of()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.time").exists());
        }

        @Test
        @DisplayName("inviting an account from another room → 404")
        void create_foreignAccount_returns404() throws Exception {
            Profile otherProfile = newProfile("other");
            Room otherRoom = newRoomOwnedBy(otherProfile, "Other Room");
            Account foreign = newAccount(otherProfile, otherRoom, "Foreign");

            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/sessions/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SessionRequest(Instant.now(), "Bad invite", Set.of(foreign.getId())))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("stranger without an account in the room → 403 (resolver AccessDeniedException)")
        void create_byStranger_returns403() throws Exception {
            authenticateAs(newProfile("stranger"));
            mockMvc.perform(post("/api/v1/sessions/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SessionRequest(Instant.now(), "X", Set.of()))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/sessions/{roomId}/{sessionId} — update")
    class UpdateTests {

        @Test
        @DisplayName("creator updates time and description → 200")
        void update_byCreator_success() throws Exception {
            long id = createViaApi(creatorProfile, new SessionRequest(Instant.now(), "Before", Set.of()));
            Instant newTime = Instant.now().plusSeconds(172_800);

            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/sessions/{roomId}/{sessionId}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SessionRequest(newTime, "After", Set.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("After"));

            assertThat(sessionRepository.findById(id).orElseThrow().getDescription()).isEqualTo("After");
        }

        @Test
        @DisplayName("non-creator member may not update → 403")
        void update_byMember_returns403() throws Exception {
            long id = createViaApi(creatorProfile, new SessionRequest(Instant.now(), "Locked", Set.of()));

            authenticateAs(memberProfile);
            mockMvc.perform(put("/api/v1/sessions/{roomId}/{sessionId}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SessionRequest(Instant.now(), "Hijack", Set.of()))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unknown session id → 404")
        void update_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/sessions/{roomId}/{sessionId}", room.getId(), 9_999_999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SessionRequest(Instant.now(), "X", Set.of()))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("a session from another room → 404 (no cross-room edit)")
        void update_otherRoomSession_returns404() throws Exception {
            long id = createViaApi(creatorProfile, new SessionRequest(Instant.now(), "Mine", Set.of()));
            Room otherRoom = newRoom("Other Room");

            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/sessions/{roomId}/{sessionId}", otherRoom.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SessionRequest(Instant.now(), "Stolen", Set.of()))))
                    .andExpect(status().isNotFound());

            assertThat(sessionRepository.findById(id).orElseThrow().getDescription()).isEqualTo("Mine");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sessions/{sessionId} + list windows")
    class ReadTests {

        @Test
        @DisplayName("get by id returns the session → 200")
        void get_byId_success() throws Exception {
            long id = createViaApi(creatorProfile, new SessionRequest(Instant.now(), "Detail", Set.of()));

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/sessions/{sessionId}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id));
        }

        @Test
        @DisplayName("get by unknown id → 404")
        void get_byId_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/sessions/{sessionId}", 9_999_999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /{roomId}/range returns sessions inside the window")
        void get_range() throws Exception {
            Instant when = Instant.now().plusSeconds(3 * 86_400);
            createViaApi(creatorProfile, new SessionRequest(when, "In range", Set.of()));
            createViaApi(creatorProfile, new SessionRequest(Instant.now().plusSeconds(400L * 86_400), "Far out", Set.of()));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/sessions/{roomId}/range", room.getId())
                            .param("start", Instant.now().minusSeconds(86_400).toString())
                            .param("end", Instant.now().plusSeconds(30 * 86_400).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].description").value("In range"));
        }

        @Test
        @DisplayName("GET /{roomId}/upcoming: creator sees every upcoming session, member only invited ones")
        void get_upcoming_visibility() throws Exception {
            Instant when = Instant.now().plusSeconds(2 * 86_400);
            long openToMember = createViaApi(creatorProfile, new SessionRequest(when, "For member", Set.of(memberAccount.getId())));
            long creatorOnly = createViaApi(creatorProfile, new SessionRequest(when.plusSeconds(60), "Creator only", Set.of()));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/sessions/{roomId}/upcoming", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + openToMember + ")]").exists())
                    .andExpect(jsonPath("$[?(@.id == " + creatorOnly + ")]").exists());

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/sessions/{roomId}/upcoming", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + openToMember + ")]").exists())
                    .andExpect(jsonPath("$[?(@.id == " + creatorOnly + ")]").doesNotExist());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/sessions/{roomId}/{sessionId}")
    class DeleteTests {

        @Test
        @DisplayName("creator deletes a session → 204")
        void delete_byCreator_success() throws Exception {
            long id = createViaApi(creatorProfile, new SessionRequest(Instant.now(), "Doomed", Set.of()));

            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/sessions/{roomId}/{sessionId}", room.getId(), id))
                    .andExpect(status().isNoContent());

            assertThat(sessionRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("non-creator member may not delete → 403")
        void delete_byMember_returns403() throws Exception {
            long id = createViaApi(creatorProfile, new SessionRequest(Instant.now(), "Locked", Set.of()));

            authenticateAs(memberProfile);
            mockMvc.perform(delete("/api/v1/sessions/{roomId}/{sessionId}", room.getId(), id))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unknown session id → 404 (no silent no-op)")
        void delete_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/sessions/{roomId}/{sessionId}", room.getId(), 9_999_999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("a session from another room → 404 and stays in its own room")
        void delete_otherRoomSession_returns404() throws Exception {
            long id = createViaApi(creatorProfile, new SessionRequest(Instant.now(), "Keep me", Set.of()));
            Room otherRoom = newRoom("Other Room");

            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/sessions/{roomId}/{sessionId}", otherRoom.getId(), id))
                    .andExpect(status().isNotFound());

            assertThat(sessionRepository.findById(id)).isPresent();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sessions/{roomId}/{sessionId}/acknowledge")
    class AcknowledgeTests {

        @Test
        @DisplayName("an invitee accepts → 204")
        void acknowledge_byInvitee_success() throws Exception {
            long id = createViaApi(creatorProfile,
                    new SessionRequest(Instant.now(), "Invite", Set.of(memberAccount.getId())));

            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/sessions/{roomId}/{sessionId}/acknowledge", room.getId(), id)
                            .param("accepted", "true"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("a member who was not invited → 403 (ForbiddenException)")
        void acknowledge_nonInvitee_returns403() throws Exception {
            long id = createViaApi(creatorProfile, new SessionRequest(Instant.now(), "Closed", Set.of()));

            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/sessions/{roomId}/{sessionId}/acknowledge", room.getId(), id)
                            .param("accepted", "true"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unknown session id → 404")
        void acknowledge_unknownSession_returns404() throws Exception {
            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/sessions/{roomId}/{sessionId}/acknowledge", room.getId(), 9_999_999L)
                            .param("accepted", "true"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Security")
    class SecurityTests {

        @Test
        @DisplayName("anonymous get-by-id → 403")
        void anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(get("/api/v1/sessions/{sessionId}", 1L))
                    .andExpect(status().isForbidden());
        }
    }
}
