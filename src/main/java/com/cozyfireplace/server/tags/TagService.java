package com.cozyfireplace.server.tags;


import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.LoreRepository;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.tags.dto.TagCreateRequest;
import com.cozyfireplace.server.tags.dto.TagMapper;
import com.cozyfireplace.server.tags.dto.TagResponse;
import com.cozyfireplace.server.tags.dto.TagUpdateRequest;
import com.cozyfireplace.server.util.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final RoomRepository roomRepository;
    private final LoreRepository loreRepository;
    private final RoomSecurityService roomSecurityService;
    private final TagMapper tagMapper;

    public void createTag(Long roomId, TagCreateRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        assertCreator(roomId);

        if (tagRepository.existsByNameAndRoom(request.name(), room)) {
            throw new AlreadyExistsException(
                    "Tag '" + request.name() + "' already exists in room: " + room.getName()
            );
        }

        tagRepository.save(
                Tag.builder()
                        .name(request.name())
                        .room(room)
                        .build()
        );
    }

    public void updateTag(Long roomId, Long tagId, TagUpdateRequest request) {
        Tag tag = findInRoom(roomId, tagId);
        assertCreator(roomId);

        String newName = request.name();
        if (newName != null && !newName.equals(tag.getName())
                && tagRepository.existsByNameAndRoom(newName, tag.getRoom())) {
            throw new AlreadyExistsException(
                    "Tag '" + newName + "' already exists in room: " + tag.getRoom().getName()
            );
        }

        tagMapper.updateEntity(request, tag);
        tagRepository.save(tag);
    }

    public void deleteTag(Long roomId, Long tagId) {
        Tag tag = findInRoom(roomId, tagId);
        assertCreator(roomId);
        tagRepository.delete(tag);
    }

    public TagResponse getTag(Long roomId, Long tagId) {
        return tagMapper.toResponse(findInRoom(roomId, tagId));
    }

    /**
     * Room-scoped загрузку тега: в чужой комнате тега не видно (404 вместо тихого изменения чужого тега).
     */
    private Tag findInRoom(Long roomId, Long tagId) {
        return tagRepository.findByIdAndRoomId(tagId, roomId)
                .orElseThrow(() -> new NotFoundException("Tag", tagId));
    }

    private void assertCreator(Long roomId) {
        if (!roomSecurityService.isCreator(roomId)) {
            throw new ForbiddenException("Управление тегами", "только создатель комнаты может управлять тегами");
        }
    }


    //TODO: generally refactor where methods are
    public List<TagResponse> getAllRoomTags(Long roomId) {
        List<Tag> tags = tagRepository.findByRoomId(roomId);

        return tags.stream().map(tagMapper::toResponse).collect(Collectors.toList());

    }

    @Transactional(readOnly = true)
    public Set<Long> resolveTagIdsByNames(Long roomId, Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return Set.of();

        Set<String> normalized = tagNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalized.isEmpty()) return Set.of();

        List<TagRepository.IdNameRow> rows =
                tagRepository.findIdNameByRoomIdAndNameIn(roomId, normalized);

        Map<String, Long> idByName = rows.stream()
                .collect(Collectors.toMap(TagRepository.IdNameRow::getName,
                        TagRepository.IdNameRow::getId));

        // validate: all found
        List<String> missing = normalized.stream()
                .filter(n -> !idByName.containsKey(n))
                .toList();

        if (!missing.isEmpty()) {
            throw new NotFoundException("Tag(s)","name(s)", String.join(", ", missing));
        }

        return new LinkedHashSet<>(idByName.values());
    }


    public Lore updateTagsLore(Lore lore, Set<Long> tagIds, Long roomId) {
        List<Tag> tags = requireAllInRoom(tagIds, roomId);

        lore.getTags().clear();
        lore.getTags().addAll(tags);
        return lore;
    }

    /**
     * Разрешает теги для payload'а lore строго внутри комнаты:
     * чужой или несуществующий tagId отвечает 404 и не протаскивает тег из другой комнаты.
     */
    public Set<Tag> resolveTags(List<Long> tagIds, Long roomId) {
        if (tagIds == null || tagIds.isEmpty()) return Set.of();
        return new HashSet<>(requireAllInRoom(tagIds, roomId));
    }

    private List<Tag> requireAllInRoom(Collection<Long> tagIds, Long roomId) {
        if (tagIds == null || tagIds.isEmpty()) return List.of();

        List<Long> requested = List.copyOf(tagIds);
        List<Tag> found = tagRepository.findByIdInAndRoomId(requested, roomId);
        if (found.size() != requested.size()) {
            Set<Long> foundIds = found.stream().map(Tag::getId).collect(Collectors.toSet());
            requested.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .ifPresent(missingId -> {
                        throw new NotFoundException("Tag", missingId);
                    });
        }
        return found;
    }
}
