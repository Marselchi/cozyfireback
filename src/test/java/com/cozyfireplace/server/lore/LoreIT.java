package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.lore.dto.LoreRequest;
import com.cozyfireplace.server.questions.Question;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.tags.Tag;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link LoreController} — {@code /api/v1/lore}.
 *
 * <p>Exercises the full MVC + Spring Security + JPA stack against the shared Testcontainers
 * PostgreSQL. The central business rule under test is <b>role-based visibility</b>
 * (see {@code LoreService#getLoreList} / {@code assertCanReadLore} and the native
 * {@code LoreRepository#findLoreIdsFiltered} / {@code existsVisibleLore} queries): a lore entry
 * tagged with one or more roles is a "restricted"/non-public entry, visible only to the room
 * creator or to accounts holding at least one of those roles; an entry without roles is visible
 * to every member of the room. Entries the viewer may not read answer with <b>404</b> (never 403)
 * so the API does not leak their existence.</p>
 *
 * <p><b>Not covered on purpose:</b> the deprecated {@code viewInline} read view is intentionally
 * excluded (project convention: deprecated endpoints get no IT coverage); the supported read
 * surfaces are {@code /edit} and {@code /view}.</p>
 *
 * <p><b>Documented status map</b> (consistent across the whole IT suite):
 * <ul>
 *   <li>anonymous request → 403 (stateless chain, default {@code Http403ForbiddenEntryPoint});</li>
 *   <li>authenticated user without an account in the room ({@code @CurrentAccount} throws
 *       AccessDeniedException, unmapped by {@code GlobalExceptionHandler}) → 500 (pinned);</li>
 *   <li>bean-validation violations → 400 with {@code $.validationErrors.<field>};</li>
 *   <li>{@code NotFoundException} (missing lore / unknown role id) → 404;</li>
 *   <li>a {@code @CurrentAccount}-based endpoint with an unknown {@code roomId} → 500, because the
 *       argument resolver fails to find an account in the missing room before the handler runs;</li>
 *   <li>{@code deleteLore} uses the lenient {@code repository.deleteById} → unknown id is 204.</li>
 * </ul>
 *
 * <p>Role-restricted visibility is read through native queries, so fixtures that created the
 * {@code lore_roles} join rows in an earlier request within the same test transaction flush
 * explicitly before the GET (in production those are separate, committed transactions).</p>
 */
@DisplayName("LoreIT — /api/v1/lore")
class LoreIT extends BaseIntegrationTest {

    @Autowired
    LoreRepository loreRepository;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    EntityManager entityManager;

    Profile creatorProfile;
    Room room;
    Account creatorAccount;
    Profile memberProfile;
    Account memberAccount;

    @BeforeEach
    void seedRoom() {
        creatorProfile = newProfile("dm");
        room = newRoomOwnedBy(creatorProfile, "Lore Room");
        creatorAccount = room.getCreator();
        memberProfile = newProfile("player");
        memberAccount = newAccount(memberProfile, room, "Player One");
    }

    /** POSTs a lore as the given profile and returns the created id (201 expected). */
    private long createViaApi(Profile actor, String body) throws Exception {
        authenticateAs(actor);
        MvcResult result = mockMvc.perform(post("/api/v1/lore/{roomId}", room.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return Long.parseLong(result.getResponse().getContentAsString(StandardCharsets.UTF_8).trim());
    }

    private String loreBody(String title, String content, Set<Long> roleIds, Set<Long> tagIds) {
        return json(new LoreRequest(title, "A short description.", "Year of the Amber Moon",
                content, null, roleIds, tagIds));
    }

    /** Persists an unanswered question bound to the given lore, for questionCount assertions. */
    private void saveUnansweredQuestion(Account author, Lore lore, String title) {
        questionRepository.save(Question.builder()
                .title(title)
                .body("question body")
                .author(author)
                .lore(lore)
                .isAnswered(false)
                .build());
    }

    @Nested
    @DisplayName("POST /api/v1/lore/{roomId} — create")
    class CreateTests {

        @Test
        @DisplayName("creates a lore bound to the acting account and returns its id (201)")
        void create_success() throws Exception {
            Role role = newRole(room, "Inner circle");
            Tag tag = newTag(room, "history");

            long id = createViaApi(creatorProfile,
                    loreBody("The Broken Compass", "It once pointed true.", Set.of(role.getId()), Set.of(tag.getId())));

            Lore saved = loreRepository.findById(id).orElseThrow();
            assertThat(saved.getTitle()).isEqualTo("The Broken Compass");
            assertThat(saved.getAccount().getId()).isEqualTo(creatorAccount.getId());
            assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("Inner circle");
            assertThat(saved.getTags()).extracting(Tag::getName).containsExactly("history");
        }

        @Test
        @DisplayName("a room member (not only the creator) may create lore → 201")
        void create_byMember_success() throws Exception {
            long id = createViaApi(memberProfile, loreBody("Player Note", "Authored by a member.", Set.of(), Set.of()));

            Lore saved = loreRepository.findById(id).orElseThrow();
            assertThat(saved.getAccount().getId()).isEqualTo(memberAccount.getId());
        }

        @Test
        @DisplayName("unknown role id in roleIds → 404 (resolveRoles refuses missing roles)")
        void create_unknownRole_returns404() throws Exception {
            authenticateAs(creatorProfile);

            mockMvc.perform(post("/api/v1/lore/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loreBody("Ghost", "content", Set.of(9_999_999L), Set.of())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("unknown room id → 500 (@CurrentAccount cannot resolve an account in a " +
                "nonexistent room; the resolver throws before the handler's own 404 is reached)")
        void create_unknownRoom_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);

            mockMvc.perform(post("/api/v1/lore/{roomId}", 9_999_999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loreBody("Anywhere", "content", Set.of(), Set.of())))
                    // Pinned: resolver AccessDeniedException is unmapped → 500 (would become 404 if resolution were deferred).
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("blank title fails bean validation → 400 with validationErrors.title")
        void create_blankTitle_returns400() throws Exception {
            authenticateAs(creatorProfile);

            mockMvc.perform(post("/api/v1/lore/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loreBody("", "content", Set.of(), Set.of())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.title").exists());
        }

        @Test
        @DisplayName("authenticated stranger without an account in the room → 500 " +
                "(AccessDeniedException from @CurrentAccount is not mapped by GlobalExceptionHandler)")
        void create_nonMember_returns500_currentBehaviour() throws Exception {
            authenticateAs(newProfile("stranger"));

            mockMvc.perform(post("/api/v1/lore/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loreBody("Intruder", "content", Set.of(), Set.of())))
                    // Pinned on purpose: becomes 403 once AccessDeniedException gets an @ExceptionHandler.
                    .andExpect(status().isInternalServerError());
        }
    }

    /**
     * Listing reads through the native {@code findLoreIdsFiltered} query, which joins the
     * {@code lore_roles} rows written by the create requests above. The fixtures therefore flush
     * explicitly before the GET so those rows are visible to the native query.
     */
    @Nested
    @DisplayName("GET /api/v1/lore/{roomId}/all — list with visibility + filters")
    class ListTests {

        @Test
        @DisplayName("room creator sees every lore including role-restricted ones")
        void list_creatorSeesAll() throws Exception {
            createViaApi(creatorProfile, loreBody("Public Entry", "content", Set.of(), Set.of()));
            Role secret = newRole(room, "Secret role");
            createViaApi(creatorProfile, loreBody("Secret Entry", "content", Set.of(secret.getId()), Set.of()));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.page.totalElements").value(2));
        }

        @Test
        @DisplayName("member without roles sees only the lore that is not role-restricted")
        void list_memberSeesOnlyUnrestricted() throws Exception {
            createViaApi(creatorProfile, loreBody("Public Entry", "content", Set.of(), Set.of()));
            Role secret = newRole(room, "Secret role");
            createViaApi(creatorProfile, loreBody("Secret Entry", "content", Set.of(secret.getId()), Set.of()));
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("Public Entry"))
                    .andExpect(jsonPath("$.content[0].description").value("A short description."))
                    .andExpect(jsonPath("$.content[0].date").value("Year of the Amber Moon"))
                    .andExpect(jsonPath("$.content[0].nonPublic").value(false))
                    .andExpect(jsonPath("$.content[0].createdByRoomCreator").value(true))
                    .andExpect(jsonPath("$.content[0].accountName").value("Creator"));
        }

        @Test
        @DisplayName("member holding the lore's role sees the restricted entry too")
        void list_memberWithRoleSeesRestricted() throws Exception {
            Role secret = newRole(room, "Secret role");
            createViaApi(creatorProfile, loreBody("Secret Entry", "content", Set.of(secret.getId()), Set.of()));
            memberAccount.getRoles().add(secret);
            accountRepository.save(memberAccount);
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.title == 'Secret Entry')]").isNotEmpty());
        }

        @Test
        @DisplayName("title filter matches case-insensitively (substring)")
        void list_filterByTitle() throws Exception {
            createViaApi(creatorProfile, loreBody("Important History", "content", Set.of(), Set.of()));
            createViaApi(creatorProfile, loreBody("Other Notes", "content", Set.of(), Set.of()));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/all", room.getId())
                            .param("title", "important"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("Important History"));
        }

        @Test
        @DisplayName("pagination params are honoured (size=1 → single element per page)")
        void list_pagination() throws Exception {
            createViaApi(creatorProfile, loreBody("One", "content", Set.of(), Set.of()));
            createViaApi(creatorProfile, loreBody("Two", "content", Set.of(), Set.of()));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/all", room.getId())
                            .param("page", "0").param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("unknown room id → 500 (@CurrentAccount resolution precedes the handler)")
        void list_unknownRoom_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/all", 9_999_999L))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/lore/{roomId}/{loreId}/edit")
    class EditTests {

        @Test
        @DisplayName("creator gets full content plus role and tag list")
        void edit_success() throws Exception {
            Role role = newRole(room, "Inner circle");
            Tag tag = newTag(room, "relic");
            long id = createViaApi(creatorProfile,
                    loreBody("The Broken Compass", "Raw content", Set.of(role.getId()), Set.of(tag.getId())));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/edit", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.title").value("The Broken Compass"))
                    .andExpect(jsonPath("$.description").value("A short description."))
                    .andExpect(jsonPath("$.date").value("Year of the Amber Moon"))
                    .andExpect(jsonPath("$.content").value("Raw content"))
                    .andExpect(jsonPath("$.accountName").value("Creator"))
                    .andExpect(jsonPath("$.createdByRoomCreator").value(true))
                    .andExpect(jsonPath("$.roles[0].name").value("Inner circle"))
                    .andExpect(jsonPath("$.tags[0].name").value("relic"));
        }

        @Test
        @DisplayName("role-restricted lore is invisible to a role-less member → 404 (no existence leak)")
        void edit_hiddenForMemberWithoutRole_returns404() throws Exception {
            Role secret = newRole(room, "Secret role");
            long id = createViaApi(creatorProfile, loreBody("Secret Entry", "content", Set.of(secret.getId()), Set.of()));
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/edit", room.getId(), id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("unknown lore id → 404")
        void edit_unknownLore_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/edit", room.getId(), 9_999_999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/lore/{roomId}/{loreId}/view")
    class ViewTests {

        @Test
        @DisplayName("member gets a role-less entry; every mapped field is asserted so a summary-query regression is caught")
        void view_success() throws Exception {
            long id = createViaApi(creatorProfile, loreBody("View Me", "Public content", Set.of(), Set.of()));
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/view", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.title").value("View Me"))
                    .andExpect(jsonPath("$.description").value("A short description."))
                    .andExpect(jsonPath("$.date").value("Year of the Amber Moon"))
                    .andExpect(jsonPath("$.content").value("Public content"))
                    .andExpect(jsonPath("$.isAuthor").value(false))
                    .andExpect(jsonPath("$.accountName").value("Creator"))
                    .andExpect(jsonPath("$.nonPublic").value(false))
                    .andExpect(jsonPath("$.createdByRoomCreator").value(true))
                    .andExpect(jsonPath("$.tags").isEmpty())
                    // Creator's view is recorded at creation; the count reflects views BEFORE this request, so 1.
                    .andExpect(jsonPath("$.viewCount").value(1))
                    .andExpect(jsonPath("$.questionCount").value(0));
        }

        @Test
        @DisplayName("viewCount tracks distinct viewers via the lore_view subquery")
        void view_viewCountTracksDistinctViewers() throws Exception {
            long id = createViaApi(creatorProfile, loreBody("Seen", "content", Set.of(), Set.of()));
            entityManager.flush();

            // First member view: only the creator has a lore_view row (recorded on create) → 1.
            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/view", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.viewCount").value(1));

            // Creator view: creator + member rows now exist → 2 (proves the count reads real lore_view rows,
            // not a constant or a broken subquery).
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/view", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.viewCount").value(2));
        }

        @Test
        @DisplayName("role-restricted entry seen by a role-holding member reports nonPublic=true; questionCount reflects unanswered questions")
        void view_nonPublicAndQuestionCount() throws Exception {
            Role secret = newRole(room, "Inner circle");
            Tag tag = newTag(room, "relic");
            long id = createViaApi(creatorProfile,
                    loreBody("Secret Entry", "content", Set.of(secret.getId()), Set.of(tag.getId())));
            saveUnansweredQuestion(creatorAccount, loreRepository.findById(id).orElseThrow(), "Unanswered?");
            // A role-holding (non-creator) viewer sees the entry, so nonPublic surfaces as true for them;
            // the creator short-circuits it to false via canSeeAll.
            memberAccount.getRoles().add(secret);
            accountRepository.save(memberAccount);
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/view", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nonPublic").value(true))
                    .andExpect(jsonPath("$.isAuthor").value(false))
                    .andExpect(jsonPath("$.tags[0].name").value("relic"))
                    .andExpect(jsonPath("$.questionCount").value(1));
        }

        @Test
        @DisplayName("author viewing their own entry gets isAuthor=true")
        void view_authorSeesIsAuthorTrue() throws Exception {
            long id = createViaApi(creatorProfile, loreBody("Mine", "content", Set.of(), Set.of()));
            entityManager.flush();

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/view", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isAuthor").value(true));
        }

        @Test
        @DisplayName("role-restricted entry is invisible to a role-less member → 404")
        void view_hiddenForMember_returns404() throws Exception {
            Role secret = newRole(room, "Secret role");
            long id = createViaApi(creatorProfile, loreBody("Secret Entry", "content", Set.of(secret.getId()), Set.of()));
            entityManager.flush();

            authenticateAs(memberProfile);
            mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/view", room.getId(), id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/lore/{roomId}/{loreId} — update")
    class UpdateTests {

        @Test
        @DisplayName("updates title/content and re-places roles → 204")
        void update_success() throws Exception {
            Role oldRole = newRole(room, "Old role");
            Role newRole = newRole(room, "New role");
            long id = createViaApi(creatorProfile, loreBody("Draft", "First", Set.of(oldRole.getId()), Set.of()));

            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/lore/{roomId}/{loreId}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loreBody("Final", "Second", Set.of(newRole.getId()), Set.of())))
                    .andExpect(status().isNoContent());

            Lore saved = loreRepository.findById(id).orElseThrow();
            assertThat(saved.getTitle()).isEqualTo("Final");
            assertThat(saved.getContent()).isEqualTo("Second");
            assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("New role");
        }

        @Test
        @DisplayName("unknown lore id → 404")
        void update_unknownLore_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/lore/{roomId}/{loreId}", room.getId(), 9_999_999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loreBody("Ghost", "content", Set.of(), Set.of())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("blank content fails bean validation → 400 with validationErrors.content")
        void update_blankContent_returns400() throws Exception {
            long id = createViaApi(creatorProfile, loreBody("Draft", "First", Set.of(), Set.of()));

            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/lore/{roomId}/{loreId}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loreBody("Final", "", Set.of(), Set.of())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.content").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/lore/{loreId}")
    class DeleteTests {

        @Test
        @DisplayName("deletes the lore and answers 204")
        void delete_success() throws Exception {
            long id = createViaApi(creatorProfile, loreBody("Doomed", "content", Set.of(), Set.of()));

            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/lore/{loreId}", id))
                    .andExpect(status().isNoContent());

            assertThat(loreRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("unknown lore id → 204 (deleteById is lenient, no existence check in service)")
        void delete_unknownId_isSilentlyIgnored() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/lore/{loreId}", 9_999_999L))
                    // Pinned current behaviour: LoreService#deleteLore never verifies existence.
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Security")
    class SecurityTests {

        @Test
        @DisplayName("anonymous list request → 403 (default forbidden entry point)")
        void anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(get("/api/v1/lore/{roomId}/all", room.getId()))
                    .andExpect(status().isForbidden());
        }
    }
}
