package com.cozyfireplace.server.accounts;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.rooms.Room;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the accounts module ({@code /api/v1/accounts/**}).
 *
 * <p>Covers self-service account data ({@code @CurrentAccount} resolution against real
 * room membership), the room account listing (which excludes the requesting account),
 * role assignment management (add / remove / swap with 404/409 semantics) and the
 * profile self-update. The {@code POST /create} endpoint is currently a stub and the
 * test documents that it answers 200 without persisting anything.</p>
 */
@DisplayName("Accounts module IT: /api/v1/accounts")
class AccountIT extends BaseIntegrationTest {

    // ====== GET /api/v1/accounts/{roomId} ======
    @Nested
    @DisplayName("GET /api/v1/accounts/{roomId}")
    class GetAccountTests {

        @Test
        @DisplayName("should return current account data with admin flag for room creator")
        void getAccount_creator() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Acc Room");
            authenticateAs(dm);

            mockMvc.perform(get("/api/v1/accounts/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", equalTo(room.getCreator().getId().intValue())))
                    .andExpect(jsonPath("$.username", equalTo("Creator")))
                    // Lombok getter isAdmin() -> Jackson property "admin"
                    .andExpect(jsonPath("$.admin", is(true)))
                    .andExpect(jsonPath("$.roles", hasSize(0)));
        }

        @Test
        @DisplayName("should include assigned roles in the response")
        void getAccount_withRoles() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Acc Room");
            Role role = newRole(room, "Guild Master");
            room.getCreator().getRoles().add(role);
            accountRepository.save(room.getCreator());
            authenticateAs(dm);

            mockMvc.perform(get("/api/v1/accounts/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles", hasSize(1)))
                    .andExpect(jsonPath("$.roles[0].name", equalTo("Guild Master")));
        }

        @Test
        @DisplayName("follows the most recently authenticated profile, not the one of the first request")
        void getAccount_followsLatestAuthentication() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Switch Room");
            Profile player = newProfile("player");
            Account member = newAccount(player, room, "Nyra");

            authenticateAs(dm);
            mockMvc.perform(get("/api/v1/accounts/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", equalTo(room.getCreator().getId().intValue())));

            // Regression: the MockMvc filter chain used to keep answering as the profile of the
            // first request, which silently gave every later "member" request creator privileges.
            authenticateAs(player);
            mockMvc.perform(get("/api/v1/accounts/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", equalTo(member.getId().intValue())))
                    .andExpect(jsonPath("$.username", equalTo("Nyra")))
                    .andExpect(jsonPath("$.admin", is(false)));
        }
    }

    // ====== GET /api/v1/accounts/{roomId}/all ======
    @Nested
    @DisplayName("GET /api/v1/accounts/{roomId}/all")
    class ListAccountsTests {

        @Test
        @DisplayName("should list other room accounts but exclude the requesting account")
        void getAllAccounts_excludesSelf() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Crowded Room");
            Profile player = newProfile("player");
            Account member = newAccount(player, room, "Nyra");
            authenticateAs(dm);

            mockMvc.perform(get("/api/v1/accounts/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", equalTo(member.getId().intValue())))
                    .andExpect(jsonPath("$[0].username", equalTo("Nyra")))
                    .andExpect(jsonPath("$[0].profileName", equalTo(player.getUsername())));
        }

        @Test
        @DisplayName("should fail when requester has no account in the room")
        void getAllAccounts_notAMember() throws Exception {
            Room room = newRoom("Alien Room");
            Profile stranger = newProfile("stranger");
            newRoomOwnedBy(stranger, "Home Room");
            authenticateAs(stranger);

            // CurrentAccountResolver -> AccessDeniedException is unmapped today and
            // surfaces as 500 through the generic advice. Pinned intentionally.
            mockMvc.perform(get("/api/v1/accounts/{roomId}/all", room.getId()))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ====== POST /create (stub) ======
    @Nested
    @DisplayName("POST /api/v1/accounts/create")
    class CreateAccountTests {

        @Test
        @DisplayName("should answer 200 without persisting (endpoint is a stub today)")
        void createAccount_stubbed() throws Exception {
            authenticateAs(newProfile("dm"));

            long before = accountRepository.count();

            mockMvc.perform(post("/api/v1/accounts/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Nyra","roles":[]}
                                    """))
                    .andExpect(status().isOk());

            assertThat(accountRepository.count(), equalTo(before));
        }
    }

    // ====== Role management on an account ======
    @Nested
    @DisplayName("POST/DELETE/PUT /api/v1/accounts/{roomId}/{accountId}/role")
    class AccountRoleTests {

        @Test
        @DisplayName("POST should assign an existing role to the account")
        void addRole_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Role Room");
            Role role = newRole(room, "Bard");
            authenticateAs(dm);

            mockMvc.perform(post("/api/v1/accounts/{roomId}/{accountId}/role", room.getId(), room.getCreator().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.valueOf(role.getId())))
                    .andExpect(status().isOk());

            Account reloaded = accountRepository.findById(room.getCreator().getId()).orElseThrow();
            assertThat(reloaded.getRoles(), hasItem(hasProperty("name", equalTo("Bard"))));
        }

        @Test
        @DisplayName("POST should return 404 when role does not exist")
        void addRole_roleNotFound() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Role Room");
            authenticateAs(dm);

            mockMvc.perform(post("/api/v1/accounts/{roomId}/{accountId}/role", room.getId(), room.getCreator().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("999999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST should return 409 when the account already has that role")
        void addRole_duplicate() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Role Room");
            Role role = newRole(room, "Bard");
            room.getCreator().getRoles().add(role);
            accountRepository.save(room.getCreator());
            authenticateAs(dm);

            mockMvc.perform(post("/api/v1/accounts/{roomId}/{accountId}/role", room.getId(), room.getCreator().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.valueOf(role.getId())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("DELETE should detach the role from the account")
        void deleteRole_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Role Room");
            Role role = newRole(room, "Bard");
            room.getCreator().getRoles().add(role);
            accountRepository.save(room.getCreator());
            authenticateAs(dm);

            mockMvc.perform(delete("/api/v1/accounts/{roomId}/{accountId}/role", room.getId(), room.getCreator().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.valueOf(role.getId())))
                    .andExpect(status().isOk());

            Account reloaded = accountRepository.findById(room.getCreator().getId()).orElseThrow();
            assertThat(reloaded.getRoles(), hasSize(0));
        }

        @Test
        @DisplayName("DELETE should return 404 when account does not hold the role")
        void deleteRole_notAssigned() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Role Room");
            Role role = newRole(room, "Bard");
            authenticateAs(dm);

            mockMvc.perform(delete("/api/v1/accounts/{roomId}/{accountId}/role", room.getId(), room.getCreator().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.valueOf(role.getId())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT should swap an old role for a new one")
        void updateRole_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Role Room");
            Role oldRole = newRole(room, "Bard");
            Role newRole = newRole(room, "Druid");
            room.getCreator().getRoles().add(oldRole);
            accountRepository.save(room.getCreator());
            authenticateAs(dm);

            mockMvc.perform(put("/api/v1/accounts/{roomId}/{accountId}/role", room.getId(), room.getCreator().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"oldRoleId":%d,"newRoleId":%d}
                                    """.formatted(oldRole.getId(), newRole.getId())))
                    .andExpect(status().isOk());

            Account reloaded = accountRepository.findById(room.getCreator().getId()).orElseThrow();
            assertThat(reloaded.getRoles(), hasSize(1));
            assertThat(reloaded.getRoles().iterator().next().getName(), equalTo("Druid"));
        }

        @Test
        @DisplayName("PUT should return 404 when the old role is not assigned")
        void updateRole_oldRoleMissing() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Role Room");
            Role oldRole = newRole(room, "Bard");
            Role newRole = newRole(room, "Druid");
            authenticateAs(dm);

            mockMvc.perform(put("/api/v1/accounts/{roomId}/{accountId}/role", room.getId(), room.getCreator().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"oldRoleId":%d,"newRoleId":%d}
                                    """.formatted(oldRole.getId(), newRole.getId())))
                    .andExpect(status().isNotFound());
        }
    }

    // ====== PUT /{roomId}/update ======
    @Nested
    @DisplayName("PUT /api/v1/accounts/{roomId}/update")
    class UpdateSelfTests {

        @Test
        @DisplayName("should update the account's own display name")
        void updateSelf_success() throws Exception {
            Profile player = newProfile("player");
            Room room = newRoom("Self Room");
            Account member = newAccount(player, room, "Old Name");
            authenticateAs(player);

            mockMvc.perform(put("/api/v1/accounts/{roomId}/update", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Nyra the Wanderer"}
                                    """))
                    .andExpect(status().isOk());

            Account reloaded = accountRepository.findById(member.getId()).orElseThrow();
            assertThat(reloaded.getName(), equalTo("Nyra the Wanderer"));
        }

        @Test
        @DisplayName("should not touch other accounts of the same room")
        void updateSelf_scopedToCurrentAccount() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Two People Room");
            Account other = newAccount(newProfile("other"), room, "Untouchable");
            authenticateAs(dm);

            mockMvc.perform(put("/api/v1/accounts/{roomId}/update", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Renamed"}
                                    """))
                    .andExpect(status().isOk());

            assertThat(accountRepository.findById(other.getId()).orElseThrow().getName(),
                    equalTo("Untouchable"));
        }
    }
}
