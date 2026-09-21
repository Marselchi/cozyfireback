package com.cozyfireplace.server.tags;


import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.LoreRepository;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
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
    private final TagMapper tagMapper;

    public void createTag(Long roomId, TagCreateRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

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

    public void updateTag(Long tagId, TagUpdateRequest request) {
        //TODO: check similar room from roomId and admin rights
        //Too lazy rn
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NotFoundException("Tag", tagId));
        tagMapper.updateEntity(request, tag);
        tagRepository.save(tag);
    }

    public void deleteTag(Long tagId) {

        //TODO: some checks mb and deleting of role in lore

        tagRepository.deleteById(tagId);
    }

    public TagResponse getTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NotFoundException("Tag", tagId));

        return tagMapper.toResponse(tag);

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


    public Lore updateTagsLore(Lore lore, Set<Long> tagIds) {
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            Set<Long> foundIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
            tagIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .ifPresent(missingId -> {
                        throw new NotFoundException("Tag", missingId);
                    });
        }

        lore.getTags().clear();
        lore.getTags().addAll(tags);
        return lore;
    }

    public Set<Tag> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return Set.of();

        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            Set<Long> foundIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
            tagIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .ifPresent(missingId -> { throw new NotFoundException("Tag", missingId); });
        }
        return new HashSet<>(tags);
    }
}
