package com.cozyfireplace.server.tags;

import com.cozyfireplace.server.tags.dto.TagCreateRequest;
import com.cozyfireplace.server.tags.dto.TagResponse;
import com.cozyfireplace.server.tags.dto.TagUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TagController}.
 * <p>
 * The controller is a thin HTTP facade over {@link TagService}; each test fixes
 * the status code, the body passthrough and the arguments forwarded to the
 * service (the room id path variable is accepted but ignored by the tag-scoped
 * operations, mirroring the source's {@code @SuppressWarnings}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TagController")
class TagControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long TAG_ID = 4L;

    @Mock
    private TagService tagService;

    @InjectMocks
    private TagController controller;

    @Nested
    @DisplayName("POST /{roomId}")
    class Create {

        @Test
        @DisplayName("returns 204 and forwards the room id and request body")
        void returnsNoContent() {
            TagCreateRequest request = new TagCreateRequest("Locations");

            ResponseEntity<Void> response = controller.create(ROOM_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(tagService).createTag(ROOM_ID, request);
        }
    }

    @Nested
    @DisplayName("PUT /{roomId}/{tagId}")
    class Update {

        @Test
        @DisplayName("returns 204 and updates by tag id (room id is ignored)")
        void returnsNoContent() {
            TagUpdateRequest request = new TagUpdateRequest("Landmarks");

            ResponseEntity<Void> response = controller.update(ROOM_ID, TAG_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(tagService).updateTag(TAG_ID, request);
            verifyNoMoreInteractions(tagService);
        }
    }

    @Nested
    @DisplayName("DELETE /{roomId}/{tagId}")
    class Delete {

        @Test
        @DisplayName("returns 204 and deletes by tag id (room id is ignored)")
        void returnsNoContent() {
            ResponseEntity<Void> response = controller.delete(ROOM_ID, TAG_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(tagService).deleteTag(TAG_ID);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/{tagId}")
    class GetTag {

        @Test
        @DisplayName("returns 200 with the tag fetched by id")
        void returnsBody() {
            TagResponse body = TagResponse.builder().id(TAG_ID).name("Locations").build();
            when(tagService.getTag(TAG_ID)).thenReturn(body);

            ResponseEntity<TagResponse> response = controller.getTag(ROOM_ID, TAG_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }

    @Nested
    @DisplayName("GET /{roomId}/all")
    class GetAll {

        @Test
        @DisplayName("returns 200 with the room tag list")
        void returnsList() {
            List<TagResponse> body = List.of(TagResponse.builder().id(1L).name("a").build());
            when(tagService.getAllRoomTags(ROOM_ID)).thenReturn(body);

            ResponseEntity<List<TagResponse>> response = controller.getAll(ROOM_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(body);
        }
    }
}
