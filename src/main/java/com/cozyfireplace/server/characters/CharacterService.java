package com.cozyfireplace.server.characters;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.characters.dto.*;
import com.cozyfireplace.server.lore.LoreAnchorExtractor;
import com.cozyfireplace.server.lore.LoreContentParser;
import com.cozyfireplace.server.lore.LoreRepository;
import com.cozyfireplace.server.lore.dto.ContentTitleRow;
import com.cozyfireplace.server.lore.dto.ExcerptResponse;
import com.cozyfireplace.server.lore.dto.IdName;
import com.cozyfireplace.server.notifications.event.CharacterEvent;
import com.cozyfireplace.server.notifications.event.LoreEvent;
import com.cozyfireplace.server.notifications.event.OperationType;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleService;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final RoomRepository roomRepository;
    private final RoleService roleService;
    private final RoomSecurityService roomSecurityService;
    private final CharacterMapper characterMapper;
    private final LoreAnchorExtractor loreAnchorExtractor;
    private final LoreRepository loreRepository;
    private final QuestionRepository questionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createCharacter(Long roomId, Account account, CharacterRequest request) {
        RoomTextResponse room = roomRepository.findRoomUrlById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        Character character = characterMapper.toEntity(request, account);
        boolean isCreator = roomSecurityService.isCreator(account);

        character.setRoles(roleService.resolveRoles(
                request.roleIds() == null ? List.of() : List.copyOf(request.roleIds())
        ));
        Character saved = characterRepository.save(character);
        eventPublisher.publishEvent(
                CharacterEvent.builder()
                        .type(OperationType.CREATE)
                        .roomId(roomId)
                        .roomUrl(room.getRoomUrl())
                        .roomName(room.getRoomName())
                        .characterId(saved.getId())
                        .roleIds(request.roleIds())
                        .name(request.name())
                        .authorAccountId(account.getId())
                        .byDm(isCreator)
                        .build()
        );

        return saved.getId();
    }

    @Transactional
    public void updateCharacter(Long characterId, CharacterRequest request,Account account, Long roomId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NotFoundException("Character", characterId));

        RoomTextResponse room = roomRepository.findRoomUrlById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        character = roleService.updateRolesCharacter(character, request.roleIds());
        characterMapper.updateEntity(request, character);
        characterRepository.save(character);

        eventPublisher.publishEvent(
                CharacterEvent.builder()
                        .type(OperationType.UPDATE)
                        .roomId(roomId)
                        .roomUrl(room.getRoomUrl())
                        .roomName(room.getRoomName())
                        .characterId(characterId)
                        .roleIds(request.roleIds())
                        .name(request.name())
                        .authorAccountId(account.getId())
                        .build()
        );
    }

    @Transactional
    public void deleteCharacter(Long characterId) {
        characterRepository.deleteById(characterId);
    }

    // ====== LIST (pagination + filters) ======
    @Transactional(readOnly = true)
    public Page<CharacterListResponse> getCharacterList(Long roomId,
                                                        Account account,
                                                        @Nullable CharacterListFilter filter,
                                                        Pageable pageable) {

        boolean canSeeAll = roomSecurityService.isCreator(account);

        Set<Long> viewerRoleIds = getAccountRoleIds(account);

        long[] roleArr = viewerRoleIds == null ? new long[0] : viewerRoleIds.stream().mapToLong(Long::longValue).toArray();
        boolean viewerHasRoles = roleArr.length > 0;

        Set<Long> roleIds = filter != null ? filter.roleIds() : null;
        long[] roleFilterArr = roleIds == null ? new long[0] : roleIds.stream().mapToLong(Long::longValue).toArray();
        int roleCount = roleFilterArr.length;

        String name = (filter == null || filter.name() == null || filter.name().isBlank())
                ? null
                : filter.name();

        Boolean createdByCreator = (filter == null) ? null : filter.createdByRoomCreator();

        Page<Long> idPage = characterRepository.findCharacterIdsFiltered(
                roomId,
                canSeeAll,
                viewerHasRoles,
                roleArr,
                createdByCreator,
                name,
                roleCount,
                roleFilterArr,
                pageable
        );

        List<Long> ids = idPage.getContent();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }

        Map<Long, CharacterSummaryRow> summaryById = characterRepository.findSummariesByIds(ids).stream()
                .collect(Collectors.toMap(CharacterSummaryRow::getId, s -> s));

        List<CharacterListResponse> content = ids.stream()
                .map(id -> {
                    CharacterSummaryRow s = summaryById.get(id);
                    if (s == null) return null;
                    return characterMapper.toListResponse(s);
                })
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, idPage.getTotalElements());
    }

    // ====== ONE: edit ======
    @Transactional(readOnly = true)
    public CharacterResponse getCharacterForEdit(Long roomId, Long characterId, Account account) {
        assertCanReadCharacter(account, roomId, characterId, getAccountRoleIds(account));

        CharacterSummaryRow s = characterRepository.findSummaryByIdAndRoomId(roomId, characterId)
                .orElseThrow(() -> new NotFoundException("Character", characterId));

        List<Long> ids = List.of(s.getId());

        Map<Long, List<IdName>> rolesByCharacterId = characterRepository.findRolesByCharacterIds(ids).stream()
                .collect(Collectors.groupingBy(
                        CharacterRepository.CharacterRoleRow::getCharacterId,
                        Collectors.mapping(r -> new IdName(r.getRoleId(), r.getRoleName()), Collectors.toList())
                ));

        return characterMapper.toEditResponse(
                s,
                rolesByCharacterId.getOrDefault(s.getId(), List.of())
        );
    }

    // ====== ONE: user view inline ======
    @Transactional(readOnly = true)
    public CharacterUserResponse getCharacterForUserInline(Long roomId, Long characterId, Account account) {
        boolean canSeeAll = roomSecurityService.isCreator(account);

        Set<Long> roleIds = getAccountRoleIds(account);
        if (!canSeeAll) {
            assertCanReadCharacter(account, roomId, characterId, roleIds);
        }

        CharacterSummaryRow s = characterRepository.findSummaryByIdAndRoomId(roomId, characterId)
                .orElseThrow(() -> new NotFoundException("Character", characterId));

        boolean isAuthor = Objects.equals(s.getAccountId(), account.getId());

        List<Long> ids = List.of(s.getId());
        Map<Long, List<IdName>> rolesByCharacterId = characterRepository.findRolesByCharacterIds(ids).stream()
                .collect(Collectors.groupingBy(
                        CharacterRepository.CharacterRoleRow::getCharacterId,
                        Collectors.mapping(r -> new IdName(r.getRoleId(), r.getRoleName()), Collectors.toList())
                ));

        String parsedContent = LoreContentParser.parseRestrictedContent(s.getContent(), canSeeAll, roleIds);

        List<LoreAnchorExtractor.AnchorLink> anchorLinks = loreAnchorExtractor.extractAnchorLinks(parsedContent);

        Map<Long, ContentTitleRow> loreById = getLoreContent(
                roomId,
                anchorLinks.stream().map(LoreAnchorExtractor.AnchorLink::loreId).toList(),
                canSeeAll,
                roleIds
        );

        anchorLinks.removeIf(link -> !loreById.containsKey(link.loreId()));
        Set<String> anchorsToCheck = anchorLinks.stream()
                .map(l -> l.loreId() + ":" + l.header())
                .collect(Collectors.toSet());

        String clearedContent = loreAnchorExtractor.removeInvalidLinks(parsedContent, anchorsToCheck);

        List<ExcerptResponse> excerpts = extractExcerpts(anchorLinks, loreById, canSeeAll, roleIds);
        long questionCount = questionRepository.countUnansweredByCharacterId(characterId);

        return characterMapper.toUserResponse(
                s,
                clearedContent,
                isAuthor,
                rolesByCharacterId.getOrDefault(s.getId(), List.of()),
                excerpts,
                questionCount
        );
    }

    private List<ExcerptResponse> extractExcerpts(List<LoreAnchorExtractor.AnchorLink> anchorLinks,
                                                  Map<Long, ContentTitleRow> loreById,
                                                  boolean canSeeAll,
                                                  Set<Long> viewerRoleIds) {
        if (anchorLinks == null || anchorLinks.isEmpty()) return List.of();

        Map<Long, List<LoreAnchorExtractor.AnchorLink>> linksByLoreId = anchorLinks.stream()
                .collect(Collectors.groupingBy(LoreAnchorExtractor.AnchorLink::loreId));

        List<ExcerptResponse> excerpts = new ArrayList<>();

        for (var entry : linksByLoreId.entrySet()) {
            Long loreId = entry.getKey();
            List<LoreAnchorExtractor.AnchorLink> links = entry.getValue();

            ContentTitleRow lore = loreById.get(loreId);
            if (lore == null) continue;

            List<String> headersToExtract = links.stream()
                    .map(LoreAnchorExtractor.AnchorLink::header)
                    .toList();

            Map<String, String> excerptsByHeader = LoreContentParser.extractAllExcerpts(
                    lore.getContent(),
                    headersToExtract,
                    canSeeAll,
                    viewerRoleIds
            );

            for (var link : links) {
                String headerSlug = LoreContentParser.textToSlug(link.header());
                String headerUrl = LoreContentParser.encodeHeaderSlug(link.header());
                String headerContent = excerptsByHeader.get(headerSlug);

                if (headerContent != null && !headerContent.isEmpty()) {
                    ExcerptResponse response = new ExcerptResponse(
                            loreId,
                            lore.getTitle(),
                            headerContent,
                            loreId + "#" + headerUrl
                    );
                    excerpts.add(response);
                }
            }
        }

        return excerpts;
    }

    private Map<Long, ContentTitleRow> getLoreContent(Long roomId, List<Long> loreIds, boolean canSeeAll, Set<Long> roleIds) {
        long[] roleArr = roleIds == null ? new long[0] : roleIds.stream().mapToLong(Long::longValue).toArray();
        List<ContentTitleRow> rows = loreRepository.findContentAndTitleByIdsAndRoomId(
                roomId,
                loreIds.stream().mapToLong(Long::longValue).toArray(),
                canSeeAll,
                roleArr.length > 0,
                roleArr);
        return rows.stream()
                .collect(Collectors.toMap(ContentTitleRow::getId, r -> r));
    }

    private void assertCanReadCharacter(Account account, Long roomId, Long characterId, Set<Long> viewerRoleIds) {
        boolean canSeeAll = roomSecurityService.isCreator(account);

        long[] roleArr = viewerRoleIds == null ? new long[0] : viewerRoleIds.stream().mapToLong(Long::longValue).toArray();
        boolean viewerHasRoles = roleArr.length > 0;

        boolean ok = characterRepository.existsVisibleCharacter(roomId, characterId, canSeeAll, viewerHasRoles, roleArr);
        if (!ok) {
            throw new NotFoundException("Character", characterId);
        }
    }

    private Set<Long> getAccountRoleIds(Account account) {
        return account.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
    }

}
