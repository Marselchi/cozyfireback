package com.cozyfireplace.server.roles;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.rooms.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link RoleController} — {@code /api/v1/roles}.
 *
 * <p>Roles are room-scoped labels used to restrict visibility of characters and lore.
 * The suite pins the enforced invariants: role names are unique <em>per room</em>
 * (creating the same name twice in one room is a 409, in two different rooms it is fine),
 * and unknown rooms/roles answer 404 via {@code NotFoundException}.</p>
 *
 * <p><b>Room scoping and permissions:</b> every {@code {roomId}/{roleId}} endpoint resolves
 * the role with {@code findByIdAndRoomId} first, so a role belonging to another room is a
 * 404 rather than a silent write into someone else's room; create / update / delete are then
 * gated on {@code RoomSecurityService#isCreator(roomId)} → 403 for a plain member, and 403
 * for a profile without any account in the room (the resolver's {@code AccessDeniedException}
 * is mapped by {@code GlobalExceptionHandler}). Reads ({@code /all}, {@code /allEdit},
 * {@code /{roomId}/{roleId}}) stay open to every room member.</p>
 *
 * <p>The DTOs carry no bean validation ({@code RoleCreateRequest}/{@code RoleUpdateRequest}
 * are plain holders), so blank names are accepted by the API and only stopped by the database
 * when nullable.</p>
 */
@DisplayName("RoleIT — /api/v1/roles")
class RoleIT extends BaseIntegrationTest {

    Profile dm;
    Room room;

    @BeforeEach
    void seedRoom() {
        dm = newProfile("dm");
        room = newRoomOwnedBy(dm, "Role Room");
        authenticateAs(dm);
    }

    private static String roleBody(String name) {
        return "{ \"name\": \"" + name + "\" }";
    }

    @Nested
    @DisplayName("POST /api/v1/roles/{roomId} — create")
    class CreateTests {

        @Test
        @DisplayName("creates a role in the room and answers 204")
        void create_success() throws Exception {
            mockMvc.perform(post("/api/v1/roles/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Party Member")))
                    .andExpect(status().isNoContent());

            assertThat(roleRepository.findAll().stream()
                    .filter(r -> r.getName().equals("Party Member") && r.getRoom().getId().equals(room.getId())))
                    .hasSize(1);
        }

        @Test
        @DisplayName("duplicate name inside the same room → 409")
        void create_duplicateInSameRoom_returns409() throws Exception {
            newRole(room, "Guard");

            mockMvc.perform(post("/api/v1/roles/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Guard")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Guard")));
        }

        @Test
        @DisplayName("same name in a different room is allowed (uniqueness is per room)")
        void create_sameNameOtherRoom_isAllowed() throws Exception {
            newRole(room, "Guard");
            // dm must be the creator of the second room too, otherwise the creator gate fires first
            Room otherRoom = newRoomOwnedBy(dm, "Other Role Room");

            mockMvc.perform(post("/api/v1/roles/{roomId}", otherRoom.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Guard")))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("unknown room id → 404")
        void create_unknownRoom_returns404() throws Exception {
            mockMvc.perform(post("/api/v1/roles/{roomId}", 9999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Nobody's role")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("room member who is not the creator → 403 (role management is creator-only)")
        void create_byMember_returns403() throws Exception {
            Profile player = newProfile("player");
            newAccount(player, room, "Player One");

            authenticateAs(player);
            mockMvc.perform(post("/api/v1/roles/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Self-made")))
                    .andExpect(status().isForbidden());

            assertThat(roleRepository.findAll().stream()
                    .filter(r -> "Self-made".equals(r.getName()))).isEmpty();
        }

        @Test
        @DisplayName("profile without an account in the room → 403 (resolver AccessDeniedException)")
        void create_byStranger_returns403() throws Exception {
            authenticateAs(newProfile("stranger"));

            mockMvc.perform(post("/api/v1/roles/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Intruder")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("anonymous request → 403")
        void create_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(post("/api/v1/roles/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Anon")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/roles/{roomId}/{roleId} — update")
    class UpdateTests {

        @Test
        @DisplayName("renames the role and answers 204")
        void update_success() throws Exception {
            Role role = newRole(room, "Guard");

            mockMvc.perform(put("/api/v1/roles/{roomId}/{roleId}", room.getId(), role.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Trusted Guard")))
                    .andExpect(status().isNoContent());

            assertThat(roleRepository.findById(role.getId()).orElseThrow().getName()).isEqualTo("Trusted Guard");
        }

        @Test
        @DisplayName("unknown role id → 404")
        void update_unknownRole_returns404() throws Exception {
            mockMvc.perform(put("/api/v1/roles/{roomId}/{roleId}", room.getId(), 9999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Ghost")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("a role from another room → 404 and keeps its name (no cross-room rename)")
        void update_otherRoomRole_returns404() throws Exception {
            Room otherRoom = newRoomOwnedBy(dm, "Other Role Room");
            Role foreign = newRole(otherRoom, "Foreign guard");

            mockMvc.perform(put("/api/v1/roles/{roomId}/{roleId}", room.getId(), foreign.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Stolen")))
                    .andExpect(status().isNotFound());

            assertThat(roleRepository.findById(foreign.getId()).orElseThrow().getName()).isEqualTo("Foreign guard");
        }

        @Test
        @DisplayName("room member who is not the creator → 403")
        void update_byMember_returns403() throws Exception {
            Role role = newRole(room, "Guard");
            Profile player = newProfile("player");
            newAccount(player, room, "Player One");

            authenticateAs(player);
            mockMvc.perform(put("/api/v1/roles/{roomId}/{roleId}", room.getId(), role.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(roleBody("Renamed by member")))
                    .andExpect(status().isForbidden());

            assertThat(roleRepository.findById(role.getId()).orElseThrow().getName()).isEqualTo("Guard");
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/roles/{roomId}/{roleId}")
    class DeleteTests {

        @Test
        @DisplayName("deletes the role and answers 204")
        void delete_success() throws Exception {
            Role role = newRole(room, "Doomed role");

            mockMvc.perform(delete("/api/v1/roles/{roomId}/{roleId}", room.getId(), role.getId()))
                    .andExpect(status().isNoContent());

            assertThat(roleRepository.findById(role.getId())).isEmpty();
        }

        @Test
        @DisplayName("unknown role id → 404 (the delete resolves the role inside the path room)")
        void delete_unknownId_returns404() throws Exception {
            mockMvc.perform(delete("/api/v1/roles/{roomId}/{roleId}", room.getId(), 9999999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("a role from another room → 404 and survives (no cross-room delete)")
        void delete_otherRoomRole_returns404() throws Exception {
            Room otherRoom = newRoomOwnedBy(dm, "Other Role Room");
            Role foreign = newRole(otherRoom, "Foreign guard");

            mockMvc.perform(delete("/api/v1/roles/{roomId}/{roleId}", room.getId(), foreign.getId()))
                    .andExpect(status().isNotFound());

            assertThat(roleRepository.findById(foreign.getId())).isPresent();
        }

        @Test
        @DisplayName("room member who is not the creator → 403 and the role survives")
        void delete_byMember_returns403() throws Exception {
            Role role = newRole(room, "Guard");
            Profile player = newProfile("player");
            newAccount(player, room, "Player One");

            authenticateAs(player);
            mockMvc.perform(delete("/api/v1/roles/{roomId}/{roleId}", room.getId(), role.getId()))
                    .andExpect(status().isForbidden());

            assertThat(roleRepository.findById(role.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("GET endpoints")
    class ReadTests {

        @Test
        @DisplayName("single role returns id + name")
        void get_success() throws Exception {
            Role role = newRole(room, "Scout");

            mockMvc.perform(get("/api/v1/roles/{roomId}/{roleId}", room.getId(), role.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(role.getId()))
                    .andExpect(jsonPath("$.name").value("Scout"));
        }

        @Test
        @DisplayName("unknown role id → 404")
        void get_unknownRole_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/roles/{roomId}/{roleId}", room.getId(), 9999999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("all roles of the room are listed")
        void getAll_success() throws Exception {
            newRole(room, "Scout");
            newRole(room, "Mage");

            mockMvc.perform(get("/api/v1/roles/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", hasItems("Scout", "Mage")));
        }

        @Test
        @DisplayName("allEdit exposes the account names currently holding each role")
        void getAllEdit_includesUsers() throws Exception {
            Role role = newRole(room, "Scout");
            Account holder = newAccount(newProfile("player"), room, "Player One");
            holder.getRoles().add(role);
            accountRepository.save(holder);

            mockMvc.perform(get("/api/v1/roles/{roomId}/allEdit", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.name == 'Scout')].users[0]").value("Player One"));
        }
    }
}
