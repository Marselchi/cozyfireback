package com.cozyfireplace.server.questions;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.LoreRepository;
import com.cozyfireplace.server.questions.dto.QuestionCreateRequest;
import com.cozyfireplace.server.questions.dto.QuestionUpdateRequest;
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
 * Integration tests for {@link QuestionController} — {@code /api/v1/questions}.
 *
 * <p>Drives questions through the real MVC + Spring Security + JPA stack against the shared
 * Testcontainers PostgreSQL. A question is always authored by the {@code @CurrentAccount} of the
 * acting profile, and may optionally be linked to a lore entry and/or a character (unknown link ids
 * are rejected with 404 by the service before the row is saved).</p>
 *
 * <p><b>Documented status map</b> (consistent across the whole IT suite):
 * <ul>
 *   <li>anonymous request → 403;</li>
 *   <li>{@code @CurrentAccount} write endpoints with a stranger / unknown room → 500
 *       (the argument resolver's AccessDeniedException is unmapped);</li>
 *   <li>bean-validation violations → 400 with {@code $.validationErrors.<field>};</li>
 *   <li>{@code NotFoundException} (missing question / lore / character) → 404;</li>
 *   <li>the contradictory filter combination {@code loreId + hasLore=false} raises a plain
 *       {@code IllegalArgumentException} which is unmapped → 500 (pinned).</li>
 * </ul>
 *
 * <p>Listing ({@code GET /{roomId}}) has no {@code @CurrentAccount} parameter, so it is only
 * gated by authentication — the room's questions are returned for any authenticated caller.</p>
 */
@DisplayName("QuestionIT — /api/v1/questions")
class QuestionIT extends BaseIntegrationTest {

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    LoreRepository loreRepository;

    Profile creatorProfile;
    Room room;
    Account creatorAccount;
    Profile memberProfile;
    Account memberAccount;

    @BeforeEach
    void seedRoom() {
        creatorProfile = newProfile("dm");
        room = newRoomOwnedBy(creatorProfile, "Q&A Room");
        creatorAccount = room.getCreator();
        memberProfile = newProfile("player");
        memberAccount = newAccount(memberProfile, room, "Player One");
    }

    /** POSTs a question as the given profile and returns the created id (201 expected). */
    private long createViaApi(Profile actor, String body) throws Exception {
        authenticateAs(actor);
        MvcResult result = mockMvc.perform(post("/api/v1/questions/{roomId}", room.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private long readId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("id").asLong();
    }

    private String createBody(String title, String body, Long category, Long loreId, Long characterId) {
        return json(new QuestionCreateRequest(title, body, category, loreId, characterId));
    }

    @Nested
    @DisplayName("POST /api/v1/questions/{roomId} — create")
    class CreateTests {

        @Test
        @DisplayName("creates a question bound to the acting account (isAnswered=false)")
        void create_success() throws Exception {
            long id = createViaApi(creatorProfile, createBody("Who guards the gate?", "Any lore?", 2L, null, null));

            Question saved = questionRepository.findById(id).orElseThrow();
            assertThat(saved.getTitle()).isEqualTo("Who guards the gate?");
            assertThat(saved.getCategory()).isEqualTo(2);
            assertThat(saved.getIsAnswered()).isFalse();
            assertThat(saved.getAuthor().getId()).isEqualTo(creatorAccount.getId());
        }

        @Test
        @DisplayName("author username in the response is the account's room name")
        void create_responseCarriesAuthor() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/questions/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("T", "B", null, null, null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("T"))
                    .andExpect(jsonPath("$.isAnswered").value(false))
                    .andExpect(jsonPath("$.author.username").value("Creator"));
        }

        @Test
        @DisplayName("a room member may ask questions too → 201")
        void create_byMember_success() throws Exception {
            long id = createViaApi(memberProfile, createBody("Member Q", "B", null, null, null));

            assertThat(questionRepository.findById(id).orElseThrow().getAuthor().getId())
                    .isEqualTo(memberAccount.getId());
        }

        @Test
        @DisplayName("links an existing lore entry (unknown id → 404)")
        void create_linksLore() throws Exception {
            Lore lore = loreRepository.save(Lore.builder()
                    .title("Gate lore").content("Sentries.").account(creatorAccount).build());

            long id = createViaApi(creatorProfile, createBody("About the gate", "B", null, lore.getId(), null));

            assertThat(questionRepository.findById(id).orElseThrow().getLore().getId()).isEqualTo(lore.getId());
        }

        @Test
        @DisplayName("unknown loreId → 404")
        void create_unknownLore_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/questions/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("T", "B", null, 9_999_999L, null)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("blank title fails bean validation → 400 validationErrors.title")
        void create_blankTitle_returns400() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/questions/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("", "B", null, null, null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.title").exists());
        }

        @Test
        @DisplayName("category out of 0..6 → 400 validationErrors.category")
        void create_badCategory_returns400() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/questions/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("T", "B", 7L, null, null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.category").exists());
        }

        @Test
        @DisplayName("stranger without an account in the room → 500 (unmapped AccessDeniedException)")
        void create_nonMember_returns500_currentBehaviour() throws Exception {
            authenticateAs(newProfile("stranger"));
            mockMvc.perform(post("/api/v1/questions/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("T", "B", null, null, null)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/questions/{roomId} — list with filters")
    class ListTests {

        @Test
        @DisplayName("returns the room's questions as a page")
        void list_success() throws Exception {
            createViaApi(creatorProfile, createBody("First", "B", null, null, null));
            createViaApi(memberProfile, createBody("Second", "B", null, null, null));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/questions/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.page.totalElements").value(2));
        }

        @Test
        @DisplayName("search filter matches the title substring case-insensitively")
        void list_searchFilter() throws Exception {
            createViaApi(creatorProfile, createBody("Gate guard", "B", null, null, null));
            createViaApi(creatorProfile, createBody("Blacksmith", "B", null, null, null));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/questions/{roomId}", room.getId())
                            .param("search", "gate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("Gate guard"));
        }

        @Test
        @DisplayName("authorId filter restricts to a single author's questions")
        void list_authorFilter() throws Exception {
            createViaApi(creatorProfile, createBody("DM Q", "B", null, null, null));
            createViaApi(memberProfile, createBody("Player Q", "B", null, null, null));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/questions/{roomId}", room.getId())
                            .param("authorId", String.valueOf(memberAccount.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("Player Q"));
        }

        @Test
        @DisplayName("contradictory filters (loreId + hasLore=false) → 500 (unmapped IllegalArgumentException)")
        void list_filterConflict_returns500_currentBehaviour() throws Exception {
            Lore lore = loreRepository.save(Lore.builder()
                    .title("L").content("C").account(creatorAccount).build());

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/questions/{roomId}", room.getId())
                            .param("loreId", String.valueOf(lore.getId()))
                            .param("hasLore", "false"))
                    // Pinned: the service throws a bare IllegalArgumentException with no @ExceptionHandler → 500.
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("GET /{roomId}/authors returns the distinct question authors")
        void authors_success() throws Exception {
            createViaApi(creatorProfile, createBody("DM Q", "B", null, null, null));
            createViaApi(memberProfile, createBody("Player Q", "B", null, null, null));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/questions/{roomId}/authors", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + creatorAccount.getId() + ")]").exists())
                    .andExpect(jsonPath("$[?(@.id == " + memberAccount.getId() + ")]").exists());
        }

        @Test
        @DisplayName("GET /{roomId}/{id} → 500 (pinned: MultipleBagFetchException on the fetch-join)")
        void get_one_returns500_currentBehaviour() throws Exception {
            long id = createViaApi(creatorProfile, createBody("Single", "B", null, null, null));

            authenticateAs(creatorProfile);
            // Pinned: findByIdWithAnswersAndAuthor eagerly fetches two List bags
            // (Question.answers + Answer.replies), which Hibernate rejects with
            // MultipleBagFetchException -> the endpoint 500s for every id. Use /single instead.
            mockMvc.perform(get("/api/v1/questions/{roomId}/{id}", room.getId(), id))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("GET /{roomId}/{id}/single returns the detail wrapper")
        void get_single_success() throws Exception {
            long id = createViaApi(creatorProfile, createBody("Detail", "B", null, null, null));

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/questions/{roomId}/{id}/single", room.getId(), id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.question.id").value(id))
                    .andExpect(jsonPath("$.answers").isArray());
        }

        @Test
        @DisplayName("GET /{roomId}/{id} with an unknown id → 500 (same fetch-join bug, pre-checks the query)")
        void get_one_unknown_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/questions/{roomId}/{id}", room.getId(), 9_999_999L))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/questions/{id}")
    class UpdateTests {

        @Test
        @DisplayName("updates title/body/category and returns the new state (200)")
        void update_success() throws Exception {
            long id = createViaApi(creatorProfile, createBody("Old", "Old body", 1L, null, null));

            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/questions/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new QuestionUpdateRequest("New", "New body", 3L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("New"))
                    .andExpect(jsonPath("$.category").value(3));

            assertThat(questionRepository.findById(id).orElseThrow().getTitle()).isEqualTo("New");
        }

        @Test
        @DisplayName("unknown question id → 404")
        void update_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/questions/{id}", 9_999_999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new QuestionUpdateRequest("T", "B", null))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("blank body fails bean validation → 400 validationErrors.body")
        void update_blankBody_returns400() throws Exception {
            long id = createViaApi(creatorProfile, createBody("T", "B", null, null, null));

            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/questions/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new QuestionUpdateRequest("T", "", null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.body").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/questions/{id}")
    class DeleteTests {

        @Test
        @DisplayName("deletes an existing question → 204")
        void delete_success() throws Exception {
            long id = createViaApi(creatorProfile, createBody("Doomed", "B", null, null, null));

            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/questions/{id}", id))
                    .andExpect(status().isNoContent());

            assertThat(questionRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("unknown question id → 404 (deleteQuestion checks existsById first)")
        void delete_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/questions/{id}", 9_999_999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Security")
    class SecurityTests {

        @Test
        @DisplayName("anonymous list request → 403")
        void anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(get("/api/v1/questions/{roomId}", room.getId()))
                    .andExpect(status().isForbidden());
        }
    }
}
