package com.cozyfireplace.server.answers;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.answers.dto.AnswerCreateRequest;
import com.cozyfireplace.server.answers.dto.AnswerUpdateRequest;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.questions.Question;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.rooms.Room;
import jakarta.persistence.EntityManager;
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
 * Integration tests for {@link AnswerController} — {@code /api/v1/answers}.
 *
 * <p>Answers form a thread: a first-level answer targets a {@code questionId}, a nested reply
 * targets a {@code parentId} (exactly one of the two, never both). The question a reply belongs to
 * is inherited from its parent. The acting {@code @CurrentAccount} is always the answer's author,
 * and — when that account is the room creator ({@code isAdmin}) — answering a question flips the
 * question's {@code isAnswered} flag to {@code true}; a plain member's answer leaves it {@code false}.</p>
 *
 * <p><b>Documented status map</b> (consistent across the whole IT suite):
 * <ul>
 *   <li>anonymous request → 403;</li>
 *   <li>{@code @CurrentAccount} with a profile that has no account in the path room → 403
 *       (the resolver's {@code AccessDeniedException} is mapped by
 *       {@code GlobalExceptionHandler}); bean-validation on the request body runs
 *       <em>before</em> argument resolution, so a blank {@code content} short-circuits to 400
 *       even for a stranger;</li>
 *   <li>neither {@code questionId} nor {@code parentId} → {@code IllegalArgumentException} → 500
 *       (pinned; no dedicated handler);</li>
 *   <li>unknown {@code questionId} / {@code parentId} / answer id → 404;</li>
 *   <li>bean-validation violations → 400 with {@code $.validationErrors.content}.</li>
 * </ul>
 *
 * <p>Read endpoints that aggregate reply counts run through JPQL which auto-flushes the test
 * transaction, so rows created via the API within the same rolled-back transaction are visible.</p>
 */
@DisplayName("AnswerIT — /api/v1/answers")
class AnswerIT extends BaseIntegrationTest {

    @Autowired
    AnswerRepository answerRepository;
    @Autowired
    QuestionRepository questionRepository;
    @Autowired
    EntityManager entityManager;

    Profile creatorProfile;
    Room room;
    Account creatorAccount;
    Profile memberProfile;
    Account memberAccount;
    Question question;

    @BeforeEach
    void seedRoom() {
        creatorProfile = newProfile("dm");
        room = newRoomOwnedBy(creatorProfile, "Q&A Room");
        creatorAccount = room.getCreator();
        memberProfile = newProfile("player");
        memberAccount = newAccount(memberProfile, room, "Player One");
        question = questionRepository.save(Question.builder()
                .title("Who guards the gate?")
                .body("Any lore?")
                .author(memberAccount)
                .isAnswered(false)
                .build());
    }

    /** POSTs an answer as the given profile and returns the created id (201 expected). */
    private long createViaApi(Profile actor, AnswerCreateRequest body) throws Exception {
        authenticateAs(actor);
        MvcResult result = mockMvc.perform(post("/api/v1/answers/{roomId}", room.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private long readId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("id").asLong();
    }

    @Nested
    @DisplayName("POST /api/v1/answers/{roomId} — create")
    class CreateTests {

        @Test
        @DisplayName("creator answering marks the question answered and is flagged isAdmin")
        void create_answerToQuestion_byCreator() throws Exception {
            long id = createViaApi(creatorProfile, new AnswerCreateRequest("The gate was barred.", question.getId(), null));

            Answer saved = answerRepository.findById(id).orElseThrow();
            assertThat(saved.getContent()).isEqualTo("The gate was barred.");
            assertThat(saved.getAuthor().getId()).isEqualTo(creatorAccount.getId());
            assertThat(saved.getQuestion().getId()).isEqualTo(question.getId());
            assertThat(saved.getParent()).isNull();
            assertThat(questionRepository.findById(question.getId()).orElseThrow().getIsAnswered()).isTrue();
        }

        @Test
        @DisplayName("a member's answer leaves the question unanswered (isAdmin=false)")
        void create_answerToQuestion_byMember() throws Exception {
            authenticateAs(memberProfile);
            mockMvc.perform(post("/api/v1/answers/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerCreateRequest("As a player.", question.getId(), null))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.isAdmin").value(false))
                    .andExpect(jsonPath("$.replyCount").value(0));

            assertThat(questionRepository.findById(question.getId()).orElseThrow().getIsAnswered()).isFalse();
        }

        @Test
        @DisplayName("the response carries the author's room name")
        void create_responseCarriesAuthor() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/answers/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerCreateRequest("T", question.getId(), null))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.author.username").value("Creator"));
        }

        @Test
        @DisplayName("a reply to an existing answer inherits its question")
        void create_answerToParent() throws Exception {
            long rootId = createViaApi(creatorProfile, new AnswerCreateRequest("Root.", question.getId(), null));

            long childId = createViaApi(memberProfile, new AnswerCreateRequest("Reply.", null, rootId));

            Answer child = answerRepository.findById(childId).orElseThrow();
            assertThat(child.getParent().getId()).isEqualTo(rootId);
            assertThat(child.getQuestion().getId()).isEqualTo(question.getId());
        }

        @Test
        @DisplayName("neither questionId nor parentId → 500 (unmapped IllegalArgumentException)")
        void create_neitherTarget_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/answers/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerCreateRequest("Orphan.", null, null))))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("unknown questionId → 404")
        void create_unknownQuestion_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/answers/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerCreateRequest("T", 9_999_999L, null))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("unknown parentId → 404")
        void create_unknownParent_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/answers/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerCreateRequest("T", null, 9_999_999L))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("blank content fails bean validation → 400 validationErrors.content")
        void create_blankContent_returns400() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/answers/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerCreateRequest("", question.getId(), null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.content").exists());
        }

        @Test
        @DisplayName("stranger without an account in the room → 403 (resolver AccessDeniedException)")
        void create_nonMember_returns403() throws Exception {
            authenticateAs(newProfile("stranger"));
            mockMvc.perform(post("/api/v1/answers/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerCreateRequest("T", question.getId(), null))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/answers/{roomId}/{id} — update")
    class UpdateTests {

        @Test
        @DisplayName("edits the answer content and returns the new state (200)")
        void update_success() throws Exception {
            long id = createViaApi(creatorProfile, new AnswerCreateRequest("Before", question.getId(), null));

            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/answers/{roomId}/{id}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerUpdateRequest("After"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("After"));

            assertThat(answerRepository.findById(id).orElseThrow().getContent()).isEqualTo("After");
        }

        @Test
        @DisplayName("unknown answer id → 404")
        void update_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/answers/{roomId}/{id}", room.getId(), 9_999_999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerUpdateRequest("X"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("an answer that lives in another room → 404 (no cross-room edit)")
        void update_otherRoomAnswer_returns404() throws Exception {
            long id = createViaApi(creatorProfile, new AnswerCreateRequest("Mine", question.getId(), null));
            // memberProfile gets an account in the second room, so @CurrentAccount resolves there
            Room otherRoom = newRoomOwnedBy(memberProfile, "Other Room");

            authenticateAs(memberProfile);
            mockMvc.perform(put("/api/v1/answers/{roomId}/{id}", otherRoom.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerUpdateRequest("Stolen"))))
                    .andExpect(status().isNotFound());

            assertThat(answerRepository.findById(id).orElseThrow().getContent()).isEqualTo("Mine");
        }

        @Test
        @DisplayName("blank content fails bean validation → 400 validationErrors.content")
        void update_blankContent_returns400() throws Exception {
            long id = createViaApi(creatorProfile, new AnswerCreateRequest("T", question.getId(), null));

            authenticateAs(creatorProfile);
            mockMvc.perform(put("/api/v1/answers/{roomId}/{id}", room.getId(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AnswerUpdateRequest(""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.content").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/answers — read tree endpoints")
    class ReadTests {

        @Test
        @DisplayName("GET /{id} returns the answer with its aggregated replyCount")
        void get_byId_withReplyCount() throws Exception {
            long rootId = createViaApi(creatorProfile, new AnswerCreateRequest("Root", question.getId(), null));
            createViaApi(memberProfile, new AnswerCreateRequest("Child A", null, rootId));
            createViaApi(memberProfile, new AnswerCreateRequest("Child B", null, rootId));
            entityManager.flush();

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/answers/{id}", rootId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rootId))
                    .andExpect(jsonPath("$.replyCount").value(2));
        }

        @Test
        @DisplayName("GET /{id} with an unknown id → 404")
        void get_byId_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/answers/{id}", 9_999_999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /question/{id} returns only the first-level answers")
        void get_questionAnswers_rootOnly() throws Exception {
            long rootId = createViaApi(creatorProfile, new AnswerCreateRequest("Root", question.getId(), null));
            createViaApi(memberProfile, new AnswerCreateRequest("Child", null, rootId));
            entityManager.flush();

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/answers/question/{id}", question.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(rootId));
        }

        @Test
        @DisplayName("GET /question/{id} with an unknown question → 404")
        void get_questionAnswers_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/answers/question/{id}", 9_999_999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /{parentId}/children returns the next nesting level")
        void get_children() throws Exception {
            long rootId = createViaApi(creatorProfile, new AnswerCreateRequest("Root", question.getId(), null));
            long childId = createViaApi(memberProfile, new AnswerCreateRequest("Child", null, rootId));
            entityManager.flush();

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/answers/{parentId}/children", rootId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(childId));
        }

        @Test
        @DisplayName("GET /{parentId}/children with an unknown parent → 404")
        void get_children_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/answers/{parentId}/children", 9_999_999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/answers/{roomId}/{id}")
    class DeleteTests {

        @Test
        @DisplayName("deletes an existing answer → 204")
        void delete_success() throws Exception {
            long id = createViaApi(creatorProfile, new AnswerCreateRequest("Doomed", question.getId(), null));

            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/answers/{roomId}/{id}", room.getId(), id))
                    .andExpect(status().isNoContent());

            assertThat(answerRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("deleting a leaf reply removes it; the parent survives")
        void delete_leafReply() throws Exception {
            long rootId = createViaApi(creatorProfile, new AnswerCreateRequest("Root", question.getId(), null));
            long childId = createViaApi(memberProfile, new AnswerCreateRequest("Child", null, rootId));

            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/answers/{roomId}/{id}", room.getId(), childId))
                    .andExpect(status().isNoContent());

            assertThat(answerRepository.findById(childId)).isEmpty();
            assertThat(answerRepository.findById(rootId)).isPresent();
        }

        @Test
        @DisplayName("unknown answer id → 404 (no silent no-op)")
        void delete_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(delete("/api/v1/answers/{roomId}/{id}", room.getId(), 9_999_999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("an answer from another room → 404 (no cross-room delete)")
        void delete_otherRoomAnswer_returns404() throws Exception {
            long id = createViaApi(creatorProfile, new AnswerCreateRequest("Keep me", question.getId(), null));
            Room otherRoom = newRoomOwnedBy(memberProfile, "Other Room");

            authenticateAs(memberProfile);
            mockMvc.perform(delete("/api/v1/answers/{roomId}/{id}", otherRoom.getId(), id))
                    .andExpect(status().isNotFound());

            assertThat(answerRepository.findById(id)).isPresent();
        }
    }

    @Nested
    @DisplayName("Security")
    class SecurityTests {

        @Test
        @DisplayName("anonymous request → 403")
        void anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(get("/api/v1/answers/{id}", 1L))
                    .andExpect(status().isForbidden());
        }
    }
}
