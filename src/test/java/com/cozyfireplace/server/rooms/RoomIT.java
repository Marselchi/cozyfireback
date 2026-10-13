package com.cozyfireplace.server.rooms;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.lore.LoreExportService;
import com.cozyfireplace.server.lore.LoreImportService;
import com.cozyfireplace.server.questions.Question;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.rooms.roomDetails.RoomDetails;
import com.cozyfireplace.server.rooms.roomDetails.RoomDetailsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the rooms module ({@code /api/v1/rooms/**}).
 *
 * <p>Verifies room creation (creator account + {@code RoomDetails} row are created together),
 * membership listings, the room-context endpoint guarded by {@code @CurrentAccount} resolution,
 * details read/update, slug lookup (the only {@code permitAll} room route) and the
 * lore import/export wiring (external lore services mocked — they have their own IT/unittests).</p>
 */
@DisplayName("Rooms module IT: /api/v1/rooms")
class RoomIT extends BaseIntegrationTest {

    @Autowired
    private RoomDetailsRepository roomDetailsRepository;

    @Autowired
    private QuestionRepository questionRepository;

    /** Lore import/export do heavy file processing covered by dedicated unittests. */
    @MockitoBean
    private LoreImportService loreImportService;
    @MockitoBean
    private LoreExportService loreExportService;

    // ====== POST /api/v1/rooms ======
    @Nested
    @DisplayName("POST /api/v1/rooms")
    class CreateRoomTests {

        @Test
        @DisplayName("should create room with creator account and default room details")
        void createRoom_success() throws Exception {
            Profile dm = newProfile("dm");
            authenticateAs(dm);

            mockMvc.perform(post("/api/v1/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Emberhold","description":"A cozy campaign","url":"emberhold"}
                                    """))
                    .andExpect(status().isCreated());

            Room room = roomRepository.findByUrl("emberhold").orElseThrow();
            assertThat(room.getName(), equalTo("Emberhold"));
            assertThat(room.getCreator().getProfile().getUsername(), equalTo("dm1"));
            // Creating a room must also seed its RoomDetails row with defaults
            RoomDetails details = roomDetailsRepository.findByRoomId(room.getId()).orElseThrow();
            assertThat(details.getSituation(), notNullValue());
        }

        @Test
        @DisplayName("should return 400 when room name is too short")
        void createRoom_validationError() throws Exception {
            authenticateAs(newProfile("shorty"));

            mockMvc.perform(post("/api/v1/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"ab"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.name", notNullValue()));
        }

        @Test
        @DisplayName("should return 403 when request is anonymous")
        void createRoom_unauthenticated() throws Exception {
            clearAuthentication();

            mockMvc.perform(post("/api/v1/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Nope Room"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    // ====== GET /api/v1/rooms/my ======
    @Nested
    @DisplayName("GET /api/v1/rooms/my")
    class MyRoomsTests {

        @Test
        @DisplayName("should list rooms where current profile holds an account, flagging creator flag")
        void getUserRooms_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "My Room");
            authenticateAs(dm);

            mockMvc.perform(get("/api/v1/rooms/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", equalTo(room.getId().intValue())))
                    .andExpect(jsonPath("$[0].name", equalTo("My Room")))
                    .andExpect(jsonPath("$[0].currentUserCreator", is(true)));
        }

        @Test
        @DisplayName("should return empty list for profile without accounts")
        void getUserRooms_empty() throws Exception {
            authenticateAs(newProfile("lone"));

            mockMvc.perform(get("/api/v1/rooms/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ====== GET /api/v1/rooms/{roomId}/context ======
    @Nested
    @DisplayName("GET /api/v1/rooms/{roomId}/context")
    class RoomContextTests {

        @Test
        @DisplayName("should return context for a room member")
        void getRoomContext_member() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Context Room");
            newRoom("unrelated"); // noise: another room the user is not in
            authenticateAs(dm);

            mockMvc.perform(get("/api/v1/rooms/{roomId}/context", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", equalTo(room.getId().intValue())))
                    .andExpect(jsonPath("$.name", equalTo("Context Room")))
                    .andExpect(jsonPath("$.url", equalTo(room.getUrl())))
                    .andExpect(jsonPath("$.creator.id", equalTo(room.getCreator().getId().intValue())))
                    .andExpect(jsonPath("$.creator.username", equalTo("dm1")))
                    .andExpect(jsonPath("$.currentUserCreator", is(true)))
                    .andExpect(jsonPath("$.memberCount", equalTo(1)))
                    .andExpect(jsonPath("$.questionCount", equalTo(0)));
        }

        @Test
        @DisplayName("questionCount counts only unanswered questions authored by room members")
        void getRoomContext_questionCount() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Context Room");
            Account author = room.getCreator();
            questionRepository.save(Question.builder()
                    .title("open").body("b").author(author).isAnswered(false).build());
            questionRepository.save(Question.builder()
                    .title("open2").body("b").author(author).isAnswered(false).build());
            // Answered question must be excluded by the isAnswered=false filter of countUnansweredByRoomId.
            questionRepository.save(Question.builder()
                    .title("closed").body("b").author(author).isAnswered(true).build());
            authenticateAs(dm);

            mockMvc.perform(get("/api/v1/rooms/{roomId}/context", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionCount", equalTo(2)));
        }

        @Test
        @DisplayName("should not expose context to a profile without an account in the room")
        void getRoomContext_notAMember() throws Exception {
            Room room = newRoom("Locked Room");
            Profile stranger = newProfile("stranger");
            newRoomOwnedBy(stranger, "Other Room"); // stranger has an account, but in a different room
            authenticateAs(stranger);

            // CurrentAccountResolver throws AccessDeniedException, which (as of today) is
            // not mapped to 403 by GlobalExceptionHandler and falls through to the generic
            // handler -> 500. This test pins current behaviour; adjust when a dedicated
            // security exception mapping is added.
            mockMvc.perform(get("/api/v1/rooms/{roomId}/context", room.getId()))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ====== GET/PUT /api/v1/rooms/{roomId}/details ======
    @Nested
    @DisplayName("GET/PUT /api/v1/rooms/{roomId}/details")
    class RoomDetailsTests {

        @Test
        @DisplayName("GET should return details with default situation/date")
        void getRoomDetails_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Details Room");
            roomDetailsRepository.save(RoomDetails.builder().room(room).build());
            authenticateAs(dm);

            mockMvc.perform(get("/api/v1/rooms/{roomId}/details", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentSituation", equalTo("Мастер пока не установил")))
                    .andExpect(jsonPath("$.currentDate", equalTo("Мастер пока не установил")));
        }

        @Test
        @DisplayName("GET should return 404 when room has no details row")
        void getRoomDetails_notFound() throws Exception {
            Room room = newRoom("Bare Room");
            authenticateAs(newProfile("viewer"));

            mockMvc.perform(get("/api/v1/rooms/{roomId}/details", room.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT should update details and return 202")
        void updateRoomDetails_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Details Room");
            roomDetailsRepository.save(RoomDetails.builder().room(room).build());
            authenticateAs(dm);

            mockMvc.perform(put("/api/v1/rooms/{roomId}/details", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"situation":"Siege of the gate","date":"14th of Ember","lastSession":"Retreat"}
                                    """))
                    .andExpect(status().isAccepted());

            RoomDetails updated = roomDetailsRepository.findByRoomId(room.getId()).orElseThrow();
            assertThat(updated.getSituation(), equalTo("Siege of the gate"));
            assertThat(updated.getLastSession(), equalTo("Retreat"));
        }

        @Test
        @DisplayName("PUT should return 404 for unknown room")
        void updateRoomDetails_notFound() throws Exception {
            authenticateAs(newProfile("dm"));

            mockMvc.perform(put("/api/v1/rooms/{roomId}/details", 999_999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"situation":"x","date":"y","lastSession":"z"}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    // ====== PUT /api/v1/rooms/{roomId}/name ======
    @Nested
    @DisplayName("PUT /api/v1/rooms/{roomId}/name")
    class UpdateRoomTests {

        @Test
        @DisplayName("should update name and description and return 202")
        void updateRoom_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Old Name");
            authenticateAs(dm);

            mockMvc.perform(put("/api/v1/rooms/{roomId}/name", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"New Name","description":"Now with extra lore"}
                                    """))
                    .andExpect(status().isAccepted());

            Room saved = roomRepository.findById(room.getId()).orElseThrow();
            assertThat(saved.getName(), equalTo("New Name"));
            assertThat(saved.getDescription(), equalTo("Now with extra lore"));
        }

        @Test
        @DisplayName("should return 404 for unknown room")
        void updateRoom_notFound() throws Exception {
            authenticateAs(newProfile("dm"));

            mockMvc.perform(put("/api/v1/rooms/{roomId}/name", 999_999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Ghost"}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    // ====== GET /api/v1/rooms/{roomUrl} (permitAll) ======
    @Nested
    @DisplayName("GET /api/v1/rooms/{roomUrl}")
    class ResolveSlugTests {

        @Test
        @DisplayName("should resolve slug to numeric room id without authentication")
        void getRoomByUrl_success() throws Exception {
            Room room = newRoom("Public Slug");
            clearAuthentication(); // endpoint is permitAll in SecurityConfig

            mockMvc.perform(get("/api/v1/rooms/{roomUrl}", room.getUrl()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(room.getId().toString()));
        }

        @Test
        @DisplayName("should return 404 for unknown slug")
        void getRoomByUrl_notFound() throws Exception {
            clearAuthentication();

            mockMvc.perform(get("/api/v1/rooms/{roomUrl}", "no-such-room"))
                    .andExpect(status().isNotFound());
        }
    }

    // ====== Lore import / export ======
    @Nested
    @DisplayName("POST /{roomId}/lore/import and GET /{roomId}/export")
    class LoreTransferTests {

        @Test
        @DisplayName("should delegate md import to LoreImportService and return 202")
        void importLore_md() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Import Room");
            authenticateAs(dm);

            var file = new MockMultipartFile("file", "lore.md", "text/markdown",
                    "# Header".getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/rooms/{roomId}/lore/import", room.getId())
                            .file(file)
                            .param("filetype", "md")
                            .param("autolink", "true")
                            .param("roomReset", "false")
                            .param("replaceOnConflict", "true"))
                    .andExpect(status().isAccepted());

            verify(loreImportService).importMdFile(eq(room.getId()), any(), any(), any());
        }

        @Test
        @DisplayName("should reject unsupported filetype")
        void importLore_unsupportedFiletype() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Import Room");
            authenticateAs(dm);

            var file = new MockMultipartFile("file", "lore.pdf", "application/pdf", new byte[]{1});

            // IllegalArgumentException is not mapped explicitly; generic advice answers 5xx.
            // Contract: the import service must never be invoked for unsupported types.
            mockMvc.perform(multipart("/api/v1/rooms/{roomId}/lore/import", room.getId())
                    .file(file)
                    .param("filetype", "pdf"));

            verify(loreImportService, never()).importMdFile(anyLong(), any(), any(), any());
            verify(loreImportService, never()).importZipFile(anyLong(), any(), any(), any());
        }

        @Test
        @DisplayName("should stream exported zip bytes back to the client")
        void exportLore_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Export Room");
            authenticateAs(dm);

            when(loreExportService.exportLore(eq(room.getId()), any(), any()))
                    .thenReturn("ZIPDATA".getBytes(StandardCharsets.UTF_8));

            // LoreExportRequest is a record bound via constructor — all filter params are required
            mockMvc.perform(get("/api/v1/rooms/{roomId}/export", room.getId())
                            .param("saveTags", "true")
                            .param("dmOnly", "false")
                            .param("saveSpoilers", "false"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                    .andExpect(content().bytes("ZIPDATA".getBytes(StandardCharsets.UTF_8)));
        }
    }
}
