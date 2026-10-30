package com.cozyfireplace.server.polls;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.polls.dto.PollCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link PollController} — {@code /api/v1/polls}.
 *
 * <p>A poll is a free-form feedback entry tied to the authenticated {@link Profile} (resolved from
 * the {@code SecurityContext} principal, not a room {@code @CurrentAccount}). The create endpoint
 * is mapped on the collection root and always answers {@code 204 No Content}; the read endpoint
 * filters by the {@code source} query parameter and projects the submitter's username/email.</p>
 *
 * <p><b>Documented status map</b>:
 * <ul>
 *   <li>anonymous request → 403 (endpoint is authentication-gated);</li>
 *   <li>create succeeds with {@code 204} and stores the acting profile as the poll author;</li>
 *   <li>{@code source}/{@code content} carry no bean-validation, but the columns are {@code NOT NULL},
 *       so a null {@code source} surfaces as a data-integrity failure → 500 (pinned);</li>
 *   <li>read with an unknown source simply returns an empty list → 200.</li>
 * </ul>
 */
@DisplayName("PollIT — /api/v1/polls")
class PollIT extends BaseIntegrationTest {

    @Autowired
    PollRepository pollRepository;

    Profile submitter;

    @BeforeEach
    void seedProfile() {
        submitter = newProfile("voter");
    }

    @Nested
    @DisplayName("POST /api/v1/polls — create")
    class CreateTests {

        @Test
        @DisplayName("submits a poll and records the acting profile as author → 204")
        void create_success() throws Exception {
            authenticateAs(submitter);
            mockMvc.perform(post("/api/v1/polls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new PollCreateRequest("landing", "Love the lore editor!"))))
                    .andExpect(status().isNoContent());

            Poll saved = pollRepository.findAll().stream()
                    .filter(p -> p.getContent().equals("Love the lore editor!"))
                    .findFirst().orElseThrow();
            assertThat(saved.getSource()).isEqualTo("landing");
            assertThat(saved.getProfile().getId()).isEqualTo(submitter.getId());
        }

        @Test
        @DisplayName("null source violates the NOT NULL column → 500 (pinned, no bean validation)")
        void create_nullSource_returns500_currentBehaviour() throws Exception {
            authenticateAs(submitter);
            mockMvc.perform(post("/api/v1/polls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new PollCreateRequest(null, "content"))))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("anonymous submit → 403")
        void create_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(post("/api/v1/polls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new PollCreateRequest("landing", "x"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/polls/source — read by source")
    class ReadTests {

        @Test
        @DisplayName("returns only polls matching the requested source, with author projection")
        void get_bySource_filtersAndProjects() throws Exception {
            authenticateAs(submitter);
            mockMvc.perform(post("/api/v1/polls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new PollCreateRequest("landing", "Yes"))))
                    .andExpect(status().isNoContent());
            mockMvc.perform(post("/api/v1/polls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new PollCreateRequest("app", "Nope"))))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/polls/source").param("source", "landing"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].source").value("landing"))
                    .andExpect(jsonPath("$[0].content").value("Yes"))
                    .andExpect(jsonPath("$[0].profileName").value(submitter.getUsername()))
                    .andExpect(jsonPath("$[0].profileEmail").value(submitter.getEmail()));
        }

        @Test
        @DisplayName("unknown source → empty list (200)")
        void get_bySource_unknown_returnsEmpty() throws Exception {
            authenticateAs(submitter);
            mockMvc.perform(get("/api/v1/polls/source").param("source", "no-such-source"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("anonymous read → 403")
        void get_bySource_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(get("/api/v1/polls/source").param("source", "landing"))
                    .andExpect(status().isForbidden());
        }
    }
}
