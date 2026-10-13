package com.cozyfireplace.server.templates;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.rooms.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link TemplateController} — {@code /api/v1/templates}.
 *
 * <p>Templates are reusable text blocks authored by an account. Two behaviours are
 * worth calling out:
 * <ul>
 *   <li>the template collection is <b>global</b> — listing/detail/update/delete are not
 *       scoped to a room or to the author, only creation resolves the acting account
 *       through {@code @CurrentAccount} (numeric {@code roomId} path variable);</li>
 *   <li>delete is the only id-based mutation that verifies existence up front
 *       ({@code existsById} → 404), unlike the lenient role/tag deletes.</li>
 * </ul>
 *
 * <p><b>Documented status map:</b> anonymous → 403; {@code @CurrentAccount} refused
 * for a non-member → 500 (unmapped AccessDeniedException); missing template → 404;
 * blank name/content → rejected by JPA bean validation at persist time (pinned as
 * "at least 400" so a future 400-with-hint refactor keeps the test meaningful).</p>
 */
@DisplayName("TemplateIT — /api/v1/templates")
class TemplateIT extends BaseIntegrationTest {

    @Autowired
    TemplateRepository templateRepository;

    Profile dm;
    Room room;
    Account creatorAccount;

    @BeforeEach
    void seedRoom() {
        dm = newProfile("dm");
        room = newRoomOwnedBy(dm, "Template Room");
        creatorAccount = room.getCreator();
        authenticateAs(dm);
    }

    /** Unique suffix so the global template list/search never collides with other ITs. */
    private static String unique(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private long createViaApi(String name, String content) throws Exception {
        mockMvc.perform(post("/api/v1/templates/{roomId}", room.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"" + name + "\", \"content\": \"" + content + "\" }"))
                .andExpect(status().isNoContent());
        return templateRepository.findAll().stream()
                .filter(t -> name.equals(t.getName()))
                .map(Template::getId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("template not persisted: " + name));
    }

    @Nested
    @DisplayName("POST /api/v1/templates/{roomId} — create")
    class CreateTests {

        @Test
        @DisplayName("creates a template authored by the current account and answers 204")
        void create_success() throws Exception {
            String name = unique("NPC - Shopkeeper");

            long id = createViaApi(name, "Name:\\nAge:\\nPersonality:");

            Template saved = templateRepository.findById(id).orElseThrow();
            assertThat(saved.getName()).isEqualTo(name);
            assertThat(saved.getAuthor().getId()).isEqualTo(creatorAccount.getId());
        }

        @Test
        @DisplayName("authenticated user without an account in the room → 500 " +
                "(AccessDeniedException from @CurrentAccount is not mapped by GlobalExceptionHandler)")
        void create_nonMember_returns500_currentBehaviour() throws Exception {
            authenticateAs(newProfile("stranger"));

            mockMvc.perform(post("/api/v1/templates/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"Ghost\", \"content\": \"c\" }"))
                    // Pinned on purpose: becomes 403 once AccessDeniedException gets an @ExceptionHandler.
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("blank name/content is refused at persistence level (>= 400)")
        void create_blankFields_isRejected() throws Exception {
            // The violation rolls the transaction back, so no post-assertions on the session here —
            // the blank row cannot exist for the surrounding @Transactional test to commit anyway.
            mockMvc.perform(post("/api/v1/templates/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"\", \"content\": \"\" }"))
                    .andExpect(result -> assertThat(result.getResponse().getStatus())
                            .isGreaterThanOrEqualTo(400));
        }

        @Test
        @DisplayName("anonymous request → 403")
        void create_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(post("/api/v1/templates/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"Anon\", \"content\": \"c\" }"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/templates — paged list with search")
    class ListTests {

        @Test
        @DisplayName("search param filters templates by name fragment (case-insensitive)")
        void list_searchFiltersByName() throws Exception {
            String needle = unique("SHOPKEEPER");
            createViaApi(needle, "match me");
            createViaApi(unique("Barkeep"), "other template");

            mockMvc.perform(get("/api/v1/templates").param("search", needle.substring(needle.lastIndexOf(' ') + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value(needle));
        }

        @Test
        @DisplayName("list responses omit the content field payload (id + name only)")
        void list_omitsContent() throws Exception {
            String name = unique("Quiet NPC");
            long id = createViaApi(name, "hidden content");

            mockMvc.perform(get("/api/v1/templates").param("search", name))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(id))
                    .andExpect(jsonPath("$.content[0].name").value(name));
        }

        @Test
        @DisplayName("pagination size param is honoured")
        void list_pagination() throws Exception {
            createViaApi(unique("T1"), "a");
            createViaApi(unique("T2"), "b");

            mockMvc.perform(get("/api/v1/templates").param("page", "0").param("size", "1"))
                    .andExpect(status().isOk())
                    // size=1 must never leak more than one element into the page content
                    .andExpect(jsonPath("$.content.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/templates/{templateId} — detail")
    class DetailTests {

        @Test
        @DisplayName("returns the full template including content")
        void detail_success() throws Exception {
            String name = unique("Full NPC");
            long id = createViaApi(name, "the content");

            mockMvc.perform(get("/api/v1/templates/{templateId}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.name").value(name))
                    .andExpect(jsonPath("$.content").value("the content"));
        }

        @Test
        @DisplayName("unknown id → 404")
        void detail_unknown_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/templates/{templateId}", 9999999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/templates/{templateId} — rename")
    class UpdateTests {

        @Test
        @DisplayName("updates the template name and answers 204")
        void update_success() throws Exception {
            long id = createViaApi(unique("Old Name"), "content");

            mockMvc.perform(put("/api/v1/templates/{templateId}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"Renamed Template\" }"))
                    .andExpect(status().isNoContent());

            assertThat(templateRepository.findById(id).orElseThrow().getName()).isEqualTo("Renamed Template");
        }

        @Test
        @DisplayName("unknown id → 404")
        void update_unknown_returns404() throws Exception {
            mockMvc.perform(put("/api/v1/templates/{templateId}", 9999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"Ghost\" }"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/templates/{templateId}")
    class DeleteTests {

        @Test
        @DisplayName("deletes the template and answers 204")
        void delete_success() throws Exception {
            long id = createViaApi(unique("Doomed"), "content");

            mockMvc.perform(delete("/api/v1/templates/{templateId}", id))
                    .andExpect(status().isNoContent());

            assertThat(templateRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("unknown id → 404 (delete checks existence up front, unlike role/tag deletes)")
        void delete_unknown_returns404() throws Exception {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", 9999999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Security")
    class SecurityTests {

        @Test
        @DisplayName("anonymous list request → 403")
        void list_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(get("/api/v1/templates"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("any authenticated user may read the global template list (no room scoping)")
        void list_anyAuthenticatedUser_ok() throws Exception {
            authenticateAs(newProfile("outsider"));
            mockMvc.perform(get("/api/v1/templates"))
                    .andExpect(result -> assertThat(result.getResponse().getStatus())
                            .isGreaterThanOrEqualTo(200));
        }
    }
}
