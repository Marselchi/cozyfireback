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
 * <p><b>Documented gap:</b> these endpoints perform no membership check — any
 * authenticated account may manage roles of any room (only the repository-level
 * room uniqueness is enforced). The DTOs carry no bean validation either
 * ({@code RoleCreateRequest}/{@code RoleUpdateRequest} are plain holders), so blank
 * names are accepted by the API and only stopped by the database when nullable.</p>
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
            Room otherRoom = newRoom("Other Role Room");

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
        @DisplayName("unknown role id → 204 (deleteById is lenient, service skips the existence check)")
        void delete_unknownId_isSilentlyIgnored() throws Exception {
            mockMvc.perform(delete("/api/v1/roles/{roomId}/{roleId}", room.getId(), 9999999L))
                    // Pinned current behaviour: RoleService#deleteRole never verifies existence.
                    .andExpect(status().isNoContent());
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
