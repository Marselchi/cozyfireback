package com.cozyfireplace.server.tags;

import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.tags.dto.TagCreateRequest;
import com.cozyfireplace.server.tags.dto.TagMapper;
import com.cozyfireplace.server.tags.dto.TagResponse;
import com.cozyfireplace.server.tags.dto.TagUpdateRequest;
import com.cozyfireplace.server.util.exception.AlreadyExistsException;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure Mockito unit tests for {@link TagService}.
 * <p>
 * The repositories and the MapStruct mapper are mocked so the tests target the
 * service's own logic: room/duplicate checks, the update-and-save flow, and in
 * particular {@code resolveTagIdsByNames} where names are trimmed, blanks and
 * nulls dropped, the resolved set validated against missing names, and ids
 * returned. The {@code updateTagsLore}/{@code resolveTags} id-collection
 * validation (missing-tag detection) is covered too.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TagService")
class TagServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long TAG_ID = 4L;

    @Mock
    private TagRepository tagRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagService tagService;

    // ================= helpers =================

    private Room room() {
        return Room.builder().id(ROOM_ID).name("Room " + ROOM_ID).build();
    }

    private Tag tag(long id, String name) {
        return Tag.builder().id(id).name(name).room(room()).build();
    }

    private static TagRepository.IdNameRow row(long id, String name) {
        return new TagRepository.IdNameRow() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    // ================= createTag =================

    @Nested
    @DisplayName("createTag")
    class CreateTag {

        @Test
        @DisplayName("throws NotFoundException when the room does not exist")
        void roomNotFound() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> tagService.createTag(ROOM_ID, new TagCreateRequest("Locations")));

            verify(tagRepository, never()).existsByNameAndRoom(anyString(), any());
            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws AlreadyExistsException when a same-named tag exists in the room")
        void duplicateName() {
            Room room = room();
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(tagRepository.existsByNameAndRoom("Locations", room)).thenReturn(true);

            assertThrows(AlreadyExistsException.class,
                    () -> tagService.createTag(ROOM_ID, new TagCreateRequest("Locations")));

            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("persists a new tag bound to the room and the requested name")
        void validSaves() {
            Room room = room();
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(tagRepository.existsByNameAndRoom("Locations", room)).thenReturn(false);

            tagService.createTag(ROOM_ID, new TagCreateRequest("Locations"));

            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Locations");
            assertThat(captor.getValue().getRoom()).isSameAs(room);
        }
    }

    // ================= updateTag =================

    @Nested
    @DisplayName("updateTag")
    class UpdateTag {

        @Test
        @DisplayName("throws NotFoundException when the tag does not exist")
        void tagNotFound() {
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> tagService.updateTag(TAG_ID, new TagUpdateRequest("Landmarks")));

            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("applies the request via the mapper and saves the tag")
        void validUpdatesAndSaves() {
            Tag tag = tag(TAG_ID, "Locations");
            TagUpdateRequest request = new TagUpdateRequest("Landmarks");
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));

            tagService.updateTag(TAG_ID, request);

            verify(tagMapper).updateEntity(request, tag);
            verify(tagRepository).save(tag);
        }
    }

    // ================= deleteTag / getTag =================

    @Test
    @DisplayName("deleteTag delegates straight to the repository")
    void deleteTag() {
        tagService.deleteTag(TAG_ID);
        verify(tagRepository).deleteById(TAG_ID);
    }

    @Nested
    @DisplayName("getTag")
    class GetTag {

        @Test
        @DisplayName("throws NotFoundException when the tag does not exist")
        void tagNotFound() {
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> tagService.getTag(TAG_ID));
        }

        @Test
        @DisplayName("returns the mapped response for an existing tag")
        void returnsMapped() {
            Tag tag = tag(TAG_ID, "Locations");
            TagResponse mapped = TagResponse.builder().id(TAG_ID).name(tag.getName()).build();
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
            when(tagMapper.toResponse(tag)).thenReturn(mapped);

            assertThat(tagService.getTag(TAG_ID)).isSameAs(mapped);
        }
    }

    // ================= getAllRoomTags =================

    @Nested
    @DisplayName("getAllRoomTags")
    class GetAllRoomTags {

        @Test
        @DisplayName("maps every room tag to a response")
        void mapsAll() {
            Tag a = tag(1L, "a");
            Tag b = tag(2L, "b");
            when(tagRepository.findByRoomId(ROOM_ID)).thenReturn(List.of(a, b));
            when(tagMapper.toResponse(a)).thenReturn(TagResponse.builder().id(1L).name("a").build());
            when(tagMapper.toResponse(b)).thenReturn(TagResponse.builder().id(2L).name("b").build());

            List<TagResponse> result = tagService.getAllRoomTags(ROOM_ID);

            assertThat(result).extracting(TagResponse::getId).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("returns an empty list and never calls the mapper when there are no tags")
        void empty() {
            when(tagRepository.findByRoomId(ROOM_ID)).thenReturn(List.of());

            assertThat(tagService.getAllRoomTags(ROOM_ID)).isEmpty();
            verifyNoInteractions(tagMapper);
        }
    }

    // ================= resolveTagIdsByNames =================

    @Nested
    @DisplayName("resolveTagIdsByNames")
    class ResolveTagIdsByNames {

        @Test
        @DisplayName("null or empty name set returns an empty set without touching the repository")
        void nullOrEmpty() {
            assertThat(tagService.resolveTagIdsByNames(ROOM_ID, null)).isEmpty();
            assertThat(tagService.resolveTagIdsByNames(ROOM_ID, Set.of())).isEmpty();
            verifyNoInteractions(tagRepository);
        }

        @Test
        @DisplayName("a set of only blanks/nulls normalises to empty and short-circuits")
        void allBlankNormalizedToEmpty() {
            Set<String> names = new HashSet<>(Arrays.asList("   ", "", null));

            assertThat(tagService.resolveTagIdsByNames(ROOM_ID, names)).isEmpty();
            verifyNoInteractions(tagRepository);
        }

        @Test
        @DisplayName("names are trimmed and de-duplicated before being looked up")
        @SuppressWarnings("unchecked")
        void normalizationBeforeLookup() {
            Set<String> names = new LinkedHashSet<>(Arrays.asList(" npc ", "npc", null, ""));
            when(tagRepository.findIdNameByRoomIdAndNameIn(eq(ROOM_ID), anyCollection()))
                    .thenReturn(List.of(row(7L, "npc")));

            Set<Long> result = tagService.resolveTagIdsByNames(ROOM_ID, names);

            assertThat(result).containsExactly(7L);
            ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
            verify(tagRepository).findIdNameByRoomIdAndNameIn(eq(ROOM_ID), captor.capture());
            assertThat(captor.getValue()).containsExactly("npc");
        }

        @Test
        @DisplayName("resolves every present name to its id")
        void resolvesAll() {
            Set<String> names = new LinkedHashSet<>(List.of("npc", "north"));
            when(tagRepository.findIdNameByRoomIdAndNameIn(ROOM_ID, names))
                    .thenReturn(List.of(row(7L, "npc"), row(8L, "north")));

            Set<Long> result = tagService.resolveTagIdsByNames(ROOM_ID, names);

            assertThat(result).containsExactlyInAnyOrder(7L, 8L);
        }

        @Test
        @DisplayName("throws NotFoundException listing the names that could not be resolved")
        void missingNamesThrow() {
            Set<String> names = new LinkedHashSet<>(List.of("npc", "ghost"));
            when(tagRepository.findIdNameByRoomIdAndNameIn(eq(ROOM_ID), anyCollection()))
                    .thenReturn(List.of(row(7L, "npc")));

            NotFoundException ex = assertThrows(NotFoundException.class,
                    () -> tagService.resolveTagIdsByNames(ROOM_ID, names));
            assertThat(ex).hasMessageContaining("ghost");
        }
    }

    // ================= updateTagsLore =================

    @Nested
    @DisplayName("updateTagsLore")
    class UpdateTagsLore {

        @Test
        @DisplayName("throws NotFoundException when an id is missing")
        void missingIdThrows() {
            Lore lore = Lore.builder().tags(new HashSet<>()).build();
            when(tagRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(tag(1L, "a")));

            assertThrows(NotFoundException.class, () -> tagService.updateTagsLore(lore, Set.of(1L, 2L)));
        }

        @Test
        @DisplayName("replaces the lore's tags with the resolved set")
        void validReplacesTags() {
            Tag stale = tag(99L, "stale");
            Lore lore = Lore.builder().tags(new HashSet<>(Set.of(stale))).build();
            Tag a = tag(1L, "a");
            Tag b = tag(2L, "b");
            when(tagRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(a, b));

            Lore result = tagService.updateTagsLore(lore, Set.of(1L, 2L));

            assertThat(result).isSameAs(lore);
            assertThat(lore.getTags()).containsExactlyInAnyOrder(a, b);
        }
    }

    // ================= resolveTags =================

    @Nested
    @DisplayName("resolveTags")
    class ResolveTags {

        @Test
        @DisplayName("null or empty id list returns an empty set without touching the repository")
        void nullOrEmpty() {
            assertThat(tagService.resolveTags(null)).isEmpty();
            assertThat(tagService.resolveTags(List.of())).isEmpty();
            verifyNoInteractions(tagRepository);
        }

        @Test
        @DisplayName("throws NotFoundException when an id cannot be resolved")
        void missingIdThrows() {
            when(tagRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(tag(1L, "a")));

            assertThrows(NotFoundException.class, () -> tagService.resolveTags(List.of(1L, 2L)));
        }

        @Test
        @DisplayName("returns a de-duplicated set of the resolved tags")
        void validReturnsSet() {
            Tag a = tag(1L, "a");
            Tag b = tag(2L, "b");
            when(tagRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(a, b, a));

            Set<Tag> result = tagService.resolveTags(List.of(1L, 2L));

            assertThat(result).containsExactlyInAnyOrder(a, b);
        }
    }
}
