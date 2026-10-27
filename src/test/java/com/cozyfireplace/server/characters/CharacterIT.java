package com.cozyfireplace.server.characters;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.characters.dto.CharacterRequest;
import com.cozyfireplace.server.lore.blocks.Block;
import com.cozyfireplace.server.lore.blocks.BlockRepository;
import com.cozyfireplace.server.lore.blocks.dto.AddedBlockRequest;
import com.cozyfireplace.server.lore.blocks.dto.BlockChangesRequest;
import com.cozyfireplace.server.lore.blocks.dto.UpdatedBlockRequest;
import com.cozyfireplace.server.questions.Question;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.rooms.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link CharacterController} — {@code /api/v1/characters}.
 *
 * <p>Covers character CRUD through the real MVC + security + JPA stack against
 * Testcontainers PostgreSQL. The interesting business rule under test is
 * <b>role-based visibility</b> (see {@code CharacterRepository#existsVisibleCharacter}):
 * a character tagged with roles is visible only to the room creator or to accounts
 * that hold at least one of those roles; role-less characters are visible to every
 * room member. Hidden characters respond with <b>404</b> (never 403) so the API
 * does not leak their existence.</p>
 *
 * <p>Also covers <b>restricted blocks</b> (see {@code BlockService}): a character's content can
 * hold <code>{restrictedBlock localId="x"}</code> placeholders plus a {@code blockChanges} payload.
 * On create/update the placeholders are rewritten to real block ids and the {@code block} rows are
 * owned by the character ({@code block.character_id}); on read the placeholders are expanded into
 * <code>::: restrictedBlock {...} content :::</code> — or dropped when the viewer lacks the
 * block's roles.</p>
 *
 * <p><b>Read surfaces:</b> the supported views are {@code /edit} (everything, for the editor)
 * and {@code /view} ({@code CharacterService#getCharacterForUser}) — the user-facing read that
 * mirrors Lore's {@code /view} but without the inline lore excerpts: it returns the expanded
 * content, the {@code isAuthor} flag, the role list and the unanswered {@code questionCount}.</p>
 *
 * <p><b>Documented status map</b> (consistent across the whole IT suite):
 * <ul>
 *   <li>anonymous request → 403 (stateless chain, default {@code Http403ForbiddenEntryPoint});</li>
 *   <li>authenticated user without an account in the room ({@code @CurrentAccount} fails)
 *       → {@code AccessDeniedException}, mapped by {@code GlobalExceptionHandler} → 403;</li>
 *   <li>bean-validation violations → 400 with {@code $.validationErrors.&lt;field&gt;};</li>
 *   <li>{@code NotFoundException} (missing room/character/role) → 404.</li>
 * </ul>
 *
 * <p>Every mutation is resolved inside the path room first
 * ({@code findByRoomIdAndId} → 404) and only then checks authorship / room-creator
 * (→ 403), so a character id belonging to another room reads as "not found" — the
 * API never confirms it exists elsewhere.</p>
 */
@DisplayName("CharacterIT — /api/v1/characters")
class CharacterIT extends BaseIntegrationTest {

    @Autowired
    CharacterRepository characterRepository;

    @Autowired
    BlockRepository blockRepository;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    jakarta.persistence.EntityManager entityManager;

    Profile creatorProfile;
    Room room;
    Account creatorAccount;
    Profile memberProfile;
    Account memberAccount;

    @BeforeEach
    void seedRoom() {
        creatorProfile = newProfile("dm");
        room = newRoomOwnedBy(creatorProfile, "Character Room");
        creatorAccount = room.getCreator();
        memberProfile = newProfile("player");
        memberAccount = newAccount(memberProfile, room, "Player One");
    }

    /** POSTs a character as the given profile and returns the created id (201 expected). */
    private long createViaApi(Profile actor, String body) throws Exception {
        authenticateAs(actor);
        MvcResult result = mockMvc.perform(post("/api/v1/characters/{roomId}", room.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return Long.parseLong(result.getResponse().getContentAsString(StandardCharsets.UTF_8).trim());
    }

    private static String characterBody(String name, String status, String roleIdsJson) {
        return """
                {
                  "name": "%s",
                  "description": "A wandering cartographer.",
                  "status": "%s",
                  "content": "Plain content without anchors.",
                  "roleIds": %s
                }
                """.formatted(name, status, roleIdsJson);
    }

    /** Persists an unanswered question bound to the given character, for questionCount assertions. */
    private void saveUnansweredQuestion(Account author, Character character, String title) {
        questionRepository.save(Question.builder()
                .title(title)
                .body("question body")
                .author(author)
                .character(character)
                .isAnswered(false)
                .build());
    }

    @Nested
    @DisplayName("POST /api/v1/characters/{roomId} — create")
    class CreateTests {

        @Test
        @DisplayName("creates a character bound to the acting account and returns its id (201)")
        void create_success() throws Exception {
            Role role = newRole(room, "Party Member");

            long id = createViaApi(creatorProfile, characterBody("Kaera", "alive", "[" + role.getId() + "]"));

            Character saved = characterRepository.findById(id).orElseThrow();
            assertThat(saved.getName()).isEqualTo("Kaera");
            assertThat(saved.getStatus()).isEqualTo("alive");
            assertThat(saved.getAccount().getId()).isEqualTo(room.getCreator().getId());
            assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("Party Member");
        }

        @Test
        @DisplayName("unknown role id in roleIds → 404 (resolveRoles refuses missing roles)")
        void create_unknownRole_returns404() throws Exception {
            authenticateAs(creatorProfile);

            mockMvc.perform(post("/api/v1/characters/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(characterBody("Ghost", "alive", "[9999999]")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("blank name fails bean validation → 400 with validationErrors.name")
        void create_blankName_returns400() throws Exception {
            authenticateAs(creatorProfile);

            mockMvc.perform(post("/api/v1/characters/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(characterBody("", "alive", "[]")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.name").exists());
        }

        @Test
        @DisplayName("authenticated user without an account in the room → 403 " +
                "(AccessDeniedException from @CurrentAccount is mapped by GlobalExceptionHandler)")
        void create_nonMember_returns403() throws Exception {
            authenticateAs(newProfile("stranger"));

            mockMvc.perform(post("/api/v1/characters/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(characterBody("Intruder", "alive", "[]")))
                    .andExpect(status().isForbidden());

            assertThat(characterRepository.findAll().stream()
                    .filter(c -> "Intruder".equals(c.getName()))).isEmpty();
        }
    }

    /**
     * Role-restricted characters are read through the native visibility queries of
     * {@link CharacterRepository}. The fixtures flush explicitly before the GET so that the
     * {@code character_roles} rows written by the POST above are for sure visible to those
     * native queries (in production the two requests are separate transactions, so the commit
     * has already flushed them).
     *
     * <p>History: the member-visibility failures of this group were first blamed on that join
     * table not being flushed. The real cause was the test harness: the MockMvc filter chain
     * kept answering as the profile authenticated by the FIRST request of a test, so every
     * "member" request ran with creator privileges. See
     * {@code BaseIntegrationTest#authenticateAs(Profile)}.</p>
     */
    @Nested
    @DisplayName("GET /api/v1/characters/{roomId}/all — list with visibility + filters")
    class ListTests {

        @Test
        @DisplayName("room creator sees every character including role-restricted ones")
        void list_creatorSeesAll() throws Exception {
            createViaApi(creatorProfile, characterBody("Public NPC", "alive", "[]"));
            Role secret = newRole(room, "Secret role");
            createViaApi(creatorProfile, characterBody("Secret NPC", "alive", "[" + secret.getId() + "]"));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("member without roles sees only characters that are not role-restricted")
        void list_memberSeesOnlyUnrestricted() throws Exception {
            createViaApi(creatorProfile, characterBody("Public NPC", "alive", "[]"));
            Role secret = newRole(room, "Secret role");
            createViaApi(creatorProfile, characterBody("Secret NPC", "alive", "[" + secret.getId() + "]"));
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Public NPC"))
                    // Character is bound to the creator account → computed flag is true for it
                    .andExpect(jsonPath("$.content[0].createdByRoomCreator").value(true))
                    .andExpect(jsonPath("$.content[0].accountName").value("Creator"));
        }

        @Test
        @DisplayName("member holding the character's role sees the restricted character too")
        void list_memberWithRoleSeesRestricted() throws Exception {
            Role secret = newRole(room, "Secret role");
            createViaApi(creatorProfile, characterBody("Secret NPC", "alive", "[" + secret.getId() + "]"));
            memberAccount.getRoles().add(secret);
            accountRepository.save(memberAccount);

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.name == 'Secret NPC')]").isNotEmpty());
        }

        @Test
        @DisplayName("name filter matches case-insensitively (substring)")
        void list_filterByName() throws Exception {
            createViaApi(creatorProfile, characterBody("Cartographer", "alive", "[]"));
            createViaApi(creatorProfile, characterBody("Blacksmith", "alive", "[]"));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/all", room.getId())
                            .param("name", "carto"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Cartographer"));
        }

        @Test
        @DisplayName("pagination params are honoured (size=1 → single element per page)")
        void list_pagination() throws Exception {
            createViaApi(creatorProfile, characterBody("One", "alive", "[]"));
            createViaApi(creatorProfile, characterBody("Two", "alive", "[]"));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/all", room.getId())
                            .param("page", "0").param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/characters/{roomId}/{characterId}/edit")
    class EditTests {

        @Test
        @DisplayName("creator gets full content plus role list")
        void edit_success() throws Exception {
            Role role = newRole(room, "Party Member");
            long id = createViaApi(creatorProfile, characterBody("Kaera", "alive", "[" + role.getId() + "]"));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/edit", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.name").value("Kaera"))
                    .andExpect(jsonPath("$.content").value("Plain content without anchors."))
                    .andExpect(jsonPath("$.roles[0].name").value("Party Member"));
        }

        @Test
        @DisplayName("role-restricted character is invisible to a role-less member → 404 (no existence leak)")
        void edit_hiddenForMemberWithoutRole_returns404() throws Exception {
            Role secret = newRole(room, "Secret role");
            long id = createViaApi(creatorProfile, characterBody("Secret NPC", "alive", "[" + secret.getId() + "]"));
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/edit", room.getId(), id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("unknown character id → 404")
        void edit_unknownCharacter_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/edit", room.getId(), 9999999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("member editing someone else's character → 403 Forbidden")
        void edit_memberEditingOthersCharacter_returns403() throws Exception {
            long id = createViaApi(creatorProfile, characterBody("Creator NPC", "alive", "[]"));
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/edit", room.getId(), id))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("member editing their own character → 200 OK")
        void edit_memberEditingOwnCharacter_returns200() throws Exception {
            long id = createViaApi(memberProfile, characterBody("Member Hero", "alive", "[]"));
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/edit", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.name").value("Member Hero"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/characters/{roomId}/{characterId}/view")
    class ViewTests {

        @Test
        @DisplayName("member gets a role-less character; every mapped field is asserted so a summary-query regression is caught")
        void view_success() throws Exception {
            long id = createViaApi(creatorProfile, characterBody("Kaera", "alive", "[]"));
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/view", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.name").value("Kaera"))
                    .andExpect(jsonPath("$.description").value("A wandering cartographer."))
                    .andExpect(jsonPath("$.status").value("alive"))
                    .andExpect(jsonPath("$.content").value("Plain content without anchors."))
                    .andExpect(jsonPath("$.isAuthor").value(false))
                    .andExpect(jsonPath("$.accountName").value("Creator"))
                    .andExpect(jsonPath("$.createdByRoomCreator").value(true))
                    .andExpect(jsonPath("$.roles").isEmpty())
                    .andExpect(jsonPath("$.questionCount").value(0));
        }

        @Test
        @DisplayName("author viewing their own character gets isAuthor=true")
        void view_authorSeesIsAuthorTrue() throws Exception {
            long id = createViaApi(creatorProfile, characterBody("Kaera", "alive", "[]"));
            entityManager.flush();

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/view", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isAuthor").value(true));
        }

        @Test
        @DisplayName("questionCount reflects unanswered questions bound to the character")
        void view_questionCountReflectsUnanswered() throws Exception {
            long id = createViaApi(creatorProfile, characterBody("Kaera", "alive", "[]"));
            Character character = characterRepository.findById(id).orElseThrow();
            saveUnansweredQuestion(creatorAccount, character, "Where is Kaera?");
            entityManager.flush();

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/view", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionCount").value(1));
        }

        @Test
        @DisplayName("role-restricted character is invisible to a role-less member → 404 (no existence leak)")
        void view_hiddenForMember_returns404() throws Exception {
            Role secret = newRole(room, "Secret role");
            long id = createViaApi(creatorProfile, characterBody("Secret NPC", "alive", "[" + secret.getId() + "]"));
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/view", room.getId(), id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("member holding the character's role sees it, with the role list populated")
        void view_memberWithRoleSeesIt() throws Exception {
            Role secret = newRole(room, "Secret role");
            long id = createViaApi(creatorProfile, characterBody("Secret NPC", "alive", "[" + secret.getId() + "]"));
            memberAccount.getRoles().add(secret);
            accountRepository.save(memberAccount);
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/view", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Secret NPC"))
                    .andExpect(jsonPath("$.isAuthor").value(false))
                    .andExpect(jsonPath("$.roles[0].name").value("Secret role"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/characters/{roomId}/{characterId} — update")
    class UpdateTests {

        @Test
        @DisplayName("updates fields, re-places roles and answers 204")
        void update_success() throws Exception {
            Role oldRole = newRole(room, "Old role");
            Role newRole = newRole(room, "New role");
            long id = createViaApi(creatorProfile, characterBody("Kaera", "alive", "[" + oldRole.getId() + "]"));

            authenticateAs(creatorProfile);
            mockMvc.perform(patch("/api/v1/characters/{roomId}/{characterId}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(characterBody("Kaera Renamed", "missing", "[" + newRole.getId() + "]")))
                    .andExpect(status().isNoContent());

            Character saved = characterRepository.findById(id).orElseThrow();
            assertThat(saved.getName()).isEqualTo("Kaera Renamed");
            assertThat(saved.getStatus()).isEqualTo("missing");
            assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("New role");
        }

        @Test
        @DisplayName("empty roleIds list removes every role from the character")
        void update_clearsRoles() throws Exception {
            Role role = newRole(room, "Old role");
            long id = createViaApi(creatorProfile, characterBody("Kaera", "alive", "[" + role.getId() + "]"));

            authenticateAs(creatorProfile);
            mockMvc.perform(patch("/api/v1/characters/{roomId}/{characterId}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(characterBody("Kaera", "alive", "[]")))
                    .andExpect(status().isNoContent());

            assertThat(characterRepository.findById(id).orElseThrow().getRoles()).isEmpty();
        }

        @Test
        @DisplayName("unknown character id → 404")
        void update_unknownCharacter_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(patch("/api/v1/characters/{roomId}/{characterId}", room.getId(), 9999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(characterBody("Ghost", "alive", "[]")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("member updating someone else's character → 403 Forbidden")
        void update_memberUpdatingOthersCharacter_returns403() throws Exception {
            long id = createViaApi(creatorProfile, characterBody("Creator NPC", "alive", "[]"));

            authenticateAs(memberProfile);
            mockMvc.perform(patch("/api/v1/characters/{roomId}/{characterId}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(characterBody("Hacked", "alive", "[]")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("member updating their own character → 204 No Content")
        void update_memberUpdatingOwnCharacter_returns204() throws Exception {
            long id = createViaApi(memberProfile, characterBody("Member Hero", "alive", "[]"));

            authenticateAs(memberProfile);
            mockMvc.perform(patch("/api/v1/characters/{roomId}/{characterId}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(characterBody("Member Hero Renamed", "alive", "[]")))
                    .andExpect(status().isNoContent());

            Character saved = characterRepository.findById(id).orElseThrow();
            assertThat(saved.getName()).isEqualTo("Member Hero Renamed");
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/characters/{roomId}/{characterId}")
    class DeleteTests {

        @Test
        @DisplayName("deletes the character and answers 204")
        void delete_success() throws Exception {
            long id = createViaApi(creatorProfile, characterBody("Doomed", "alive", "[]"));

            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/characters/{roomId}/{characterId}", room.getId(), id))
                    .andExpect(status().isNoContent());

            assertThat(characterRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("unknown character id → 404 (the delete resolves the row inside the path room)")
        void delete_unknownId_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/characters/{roomId}/{characterId}", room.getId(), 9999999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("a character from another room → 404 and survives (no cross-room delete)")
        void delete_otherRoomCharacter_returns404() throws Exception {
            long id = createViaApi(creatorProfile, characterBody("Keep me", "alive", "[]"));
            // memberProfile gets an account in the second room, so @CurrentAccount resolves there
            Room otherRoom = newRoomOwnedBy(memberProfile, "Other Room");

            authenticateAs(memberProfile);
            mockMvc.perform(delete("/api/v1/characters/{roomId}/{characterId}", otherRoom.getId(), id))
                    .andExpect(status().isNotFound());

            assertThat(characterRepository.findById(id)).isPresent();
        }

        @Test
        @DisplayName("member deleting someone else's character → 403 Forbidden")
        void delete_memberDeletingOthersCharacter_returns403() throws Exception {
            long id = createViaApi(creatorProfile, characterBody("Creator NPC", "alive", "[]"));

            authenticateAs(memberProfile);
            mockMvc.perform(delete("/api/v1/characters/{roomId}/{characterId}", room.getId(), id))
                    .andExpect(status().isForbidden());

            assertThat(characterRepository.findById(id)).isPresent();
        }

        @Test
        @DisplayName("member deleting their own character → 204 No Content")
        void delete_memberDeletingOwnCharacter_returns204() throws Exception {
            long id = createViaApi(memberProfile, characterBody("Member Hero", "alive", "[]"));

            authenticateAs(memberProfile);
            mockMvc.perform(delete("/api/v1/characters/{roomId}/{characterId}", room.getId(), id))
                    .andExpect(status().isNoContent());

            assertThat(characterRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("room creator deleting member's character → 204 No Content")
        void delete_creatorDeletingMembersCharacter_returns204() throws Exception {
            long id = createViaApi(memberProfile, characterBody("Member Hero", "alive", "[]"));

            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/characters/{roomId}/{characterId}", room.getId(), id))
                    .andExpect(status().isNoContent());

            assertThat(characterRepository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Restricted blocks (blockChanges / expandBlocks) — character-owned")
    class BlockTests {

        /** Matches the rewritten placeholder {@code {restrictedBlock id="123"}}. */
        private static final Pattern BLOCK_REF = Pattern.compile("\\{restrictedBlock id=\"(\\d+)\"\\}");

        private static BlockChangesRequest added(String localId, String content, Map<String, Object> metadata) {
            return BlockChangesRequest.builder()
                    .addedBlocks(List.of(AddedBlockRequest.builder()
                            .localId(localId)
                            .type("normal")
                            .content(content)
                            .metadata(metadata)
                            .build()))
                    .build();
        }

        private String characterWithBlock(String name, String content, BlockChangesRequest changes) {
            return json(new CharacterRequest(name, "A wandering cartographer.", "alive",
                    content, Set.of(), changes));
        }

        private long createdBlockId(String content) {
            Matcher matcher = BLOCK_REF.matcher(content);
            assertThat(matcher.find()).as("content keeps a rewritten block placeholder").isTrue();
            return Long.parseLong(matcher.group(1));
        }

        @Test
        @DisplayName("POST rewrites localId to the real block id and owns the Block row by the character")
        void create_withAddedBlockStoresCharacterOwnedBlock() throws Exception {
            long id = createViaApi(creatorProfile, characterWithBlock(
                    "Kaera",
                    "Intro {restrictedBlock localId=\"a\"} outro",
                    added("a", "Secret diary page", Map.of())));

            Character saved = characterRepository.findById(id).orElseThrow();
            assertThat(saved.getContent())
                    .doesNotContain("localId")
                    .startsWith("Intro ")
                    .endsWith(" outro");

            Block block = blockRepository.findById(createdBlockId(saved.getContent())).orElseThrow();
            assertThat(block.getContent()).isEqualTo("Secret diary page");
            assertThat(block.getCharacter().getId()).isEqualTo(id);
            assertThat(block.getLore()).isNull();
            assertThat(block.hasChance()).isFalse();
        }

        @Test
        @DisplayName("creator's edit view expands the placeholder into the full ::: restrictedBlock ::: markup")
        void edit_expandsBlockForCreator() throws Exception {
            long id = createViaApi(creatorProfile, characterWithBlock(
                    "Kaera", "Intro {restrictedBlock localId=\"a\"}", added("a", "Secret diary page", Map.of())));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/edit", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", containsString("::: restrictedBlock")))
                    .andExpect(jsonPath("$.content", containsString("Secret diary page")))
                    .andExpect(jsonPath("$.content", not(containsString("{restrictedBlock"))));
        }

        @Test
        @DisplayName("role-gated block is hidden from a member without that role (placeholder dropped)")
        void edit_roleGatedBlockHiddenFromMemberWithoutRole() throws Exception {
            Role insider = newRole(room, "Inner circle");
            long id = createViaApi(memberProfile, characterWithBlock(
                    "Kaera",
                    "Intro {restrictedBlock localId=\"a\"} end",
                    added("a", "Secret diary page", Map.of("roles", List.of(insider.getId().toString())))));

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/edit", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", not(containsString("Secret diary page"))))
                    .andExpect(jsonPath("$.content", not(containsString("restrictedBlock"))))
                    .andExpect(jsonPath("$.content", containsString("Intro")));
        }

        @Test
        @DisplayName("member holding the block's role sees the expanded block")
        void edit_roleGatedBlockVisibleToMemberWithRole() throws Exception {
            Role insider = newRole(room, "Inner circle");
            long id = createViaApi(memberProfile, characterWithBlock(
                    "Kaera",
                    "Intro {restrictedBlock localId=\"a\"}",
                    added("a", "Secret diary page", Map.of("roles", List.of(insider.getId().toString())))));
            memberAccount.getRoles().add(insider);
            accountRepository.save(memberAccount);

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/characters/{roomId}/{characterId}/edit", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", containsString("Secret diary page")));
        }

        @Test
        @DisplayName("PATCH with updatedBlocks rewrites the Block row content and roles")
        void update_updatedBlocksRewritesBlock() throws Exception {
            Role insider = newRole(room, "Inner circle");
            long id = createViaApi(creatorProfile, characterWithBlock(
                    "Kaera", "Intro {restrictedBlock localId=\"a\"}", added("a", "First draft", Map.of())));

            Character saved = characterRepository.findById(id).orElseThrow();
            long blockId = createdBlockId(saved.getContent());

            authenticateAs(creatorProfile);
            mockMvc.perform(patch("/api/v1/characters/{roomId}/{characterId}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CharacterRequest("Kaera", "desc", "alive",
                                    saved.getContent(), Set.of(),
                                    BlockChangesRequest.builder()
                                            .updatedBlocks(List.of(UpdatedBlockRequest.builder()
                                                    .id(String.valueOf(blockId))
                                                    .type("normal")
                                                    .content("Second draft")
                                                    .metadata(Map.of("roles", List.of(insider.getId().toString())))
                                                    .build()))
                                            .build()))))
                    .andExpect(status().isNoContent());

            Block block = blockRepository.findById(blockId).orElseThrow();
            assertThat(block.getContent()).isEqualTo("Second draft");
            assertThat(block.getRoles()).extracting(Role::getName).containsExactly("Inner circle");
            assertThat(block.getCharacter().getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("PATCH with deletedBlockIds removes the Block row (character keeps the rest of content)")
        void update_deletedBlocksRemovesRow() throws Exception {
            long id = createViaApi(creatorProfile, characterWithBlock(
                    "Kaera", "Intro {restrictedBlock localId=\"a\"} tail", added("a", "Doomed page", Map.of())));

            long blockId = createdBlockId(characterRepository.findById(id).orElseThrow().getContent());

            authenticateAs(creatorProfile);
            mockMvc.perform(patch("/api/v1/characters/{roomId}/{characterId}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CharacterRequest("Kaera", "desc", "alive",
                                    "Intro tail", Set.of(),
                                    BlockChangesRequest.builder()
                                            .deletedBlockIds(List.of(String.valueOf(blockId)))
                                            .build()))))
                    .andExpect(status().isNoContent());

            assertThat(blockRepository.findById(blockId)).isEmpty();
            assertThat(characterRepository.findById(id).orElseThrow().getContent()).isEqualTo("Intro tail");
        }

        @Test
        @DisplayName("omitting blockChanges leaves content untouched and creates no Block rows")
        void create_withoutBlockChangesKeepsPlaceholderFreeContent() throws Exception {
            long id = createViaApi(creatorProfile, json(new CharacterRequest(
                    "Kaera", "desc", "alive", "Plain content", Set.of(), null)));

            Character saved = characterRepository.findById(id).orElseThrow();
            assertThat(saved.getContent()).isEqualTo("Plain content");
            assertThat(blockRepository.findAll()).noneMatch(b -> b.getCharacter() != null
                    && b.getCharacter().getId().equals(id));
        }

    }

    @Nested
    @DisplayName("Security")
    class SecurityTests {

        @Test
        @DisplayName("anonymous list request → 403 (default forbidden entry point)")
        void anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(get("/api/v1/characters/{roomId}/all", room.getId()))
                    .andExpect(status().isForbidden());
        }
    }
}
