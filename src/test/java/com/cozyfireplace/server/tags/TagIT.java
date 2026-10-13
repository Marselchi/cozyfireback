package com.cozyfireplace.server.tags;

import com.cozyfireplace.server.BaseIntegrationTest;
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
 * Integration tests for {@link TagController} — {@code /api/v1/tags}.
 *
 * <p>Tags categorise lore entries inside a room and mirror the role semantics:
 * names are unique per room (409 on duplicates, allowed across rooms), unknown
 * rooms/tags answer 404. Like roles, the endpoints perform no membership check
 * and the request DTOs ({@code TagCreateRequest}/{@code TagUpdateRequest}) carry
 * no bean validation — this class documents the behaviour as implemented.</p>
 */
@DisplayName("TagIT — /api/v1/tags")
class TagIT extends BaseIntegrationTest {

    Profile dm;
    Room room;

    @BeforeEach
    void seedRoom() {
        dm = newProfile("dm");
        room = newRoomOwnedBy(dm, "Tag Room");
        authenticateAs(dm);
    }

    private static String tagBody(String name) {
        return "{ \"name\": \"" + name + "\" }";
    }

    @Nested
    @DisplayName("POST /api/v1/tags/{roomId} — create")
    class CreateTests {

        @Test
        @DisplayName("creates a tag in the room and answers 204")
        void create_success() throws Exception {
            mockMvc.perform(post("/api/v1/tags/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tagBody("Locations")))
                    .andExpect(status().isNoContent());

            assertThat(tagRepository.findAll().stream()
                    .filter(t -> t.getName().equals("Locations") && t.getRoom().getId().equals(room.getId())))
                    .hasSize(1);
        }

        @Test
        @DisplayName("duplicate name inside the same room → 409")
        void create_duplicateInSameRoom_returns409() throws Exception {
            newTag(room, "Factions");

            mockMvc.perform(post("/api/v1/tags/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tagBody("Factions")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Factions")));
        }

        @Test
        @DisplayName("same name in a different room is allowed (uniqueness is per room)")
        void create_sameNameOtherRoom_isAllowed() throws Exception {
            newTag(room, "Factions");
            Room otherRoom = newRoom("Other Tag Room");

            mockMvc.perform(post("/api/v1/tags/{roomId}", otherRoom.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tagBody("Factions")))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("unknown room id → 404")
        void create_unknownRoom_returns404() throws Exception {
            mockMvc.perform(post("/api/v1/tags/{roomId}", 9999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tagBody("Nobody's tag")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("anonymous request → 403")
        void create_anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(post("/api/v1/tags/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tagBody("Anon")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tags/{roomId}/{tagId} — update")
    class UpdateTests {

        @Test
        @DisplayName("renames the tag and answers 204")
        void update_success() throws Exception {
            Tag tag = newTag(room, "Locations");

            mockMvc.perform(put("/api/v1/tags/{roomId}/{tagId}", room.getId(), tag.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tagBody("Landmarks")))
                    .andExpect(status().isNoContent());

            assertThat(tagRepository.findById(tag.getId()).orElseThrow().getName()).isEqualTo("Landmarks");
        }

        @Test
        @DisplayName("unknown tag id → 404")
        void update_unknownTag_returns404() throws Exception {
            mockMvc.perform(put("/api/v1/tags/{roomId}/{tagId}", room.getId(), 9999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tagBody("Ghost")))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/tags/{roomId}/{tagId}")
    class DeleteTests {

        @Test
        @DisplayName("deletes the tag and answers 204")
        void delete_success() throws Exception {
            Tag tag = newTag(room, "Doomed tag");

            mockMvc.perform(delete("/api/v1/tags/{roomId}/{tagId}", room.getId(), tag.getId()))
                    .andExpect(status().isNoContent());

            assertThat(tagRepository.findById(tag.getId())).isEmpty();
        }

        @Test
        @DisplayName("unknown tag id → 204 (deleteById is lenient, service skips the existence check)")
        void delete_unknownId_isSilentlyIgnored() throws Exception {
            mockMvc.perform(delete("/api/v1/tags/{roomId}/{tagId}", room.getId(), 9999999L))
                    // Pinned current behaviour: TagService#deleteTag never verifies existence.
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET endpoints")
    class ReadTests {

        @Test
        @DisplayName("single tag returns id + name")
        void get_success() throws Exception {
            Tag tag = newTag(room, "Locations");

            mockMvc.perform(get("/api/v1/tags/{roomId}/{tagId}", room.getId(), tag.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(tag.getId()))
                    .andExpect(jsonPath("$.name").value("Locations"));
        }

        @Test
        @DisplayName("unknown tag id → 404")
        void get_unknownTag_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/tags/{roomId}/{tagId}", room.getId(), 9999999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("all tags of the room are listed")
        void getAll_success() throws Exception {
            newTag(room, "Locations");
            newTag(room, "Factions");

            mockMvc.perform(get("/api/v1/tags/{roomId}/all", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", hasItems("Locations", "Factions")));
        }
    }
}
