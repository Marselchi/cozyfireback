package com.cozyfireplace.server.characters;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.characters.dto.*;
import com.cozyfireplace.server.lore.blocks.BlockService;
import com.cozyfireplace.server.lore.dto.IdName;
import com.cozyfireplace.server.notifications.event.CharacterEvent;
import com.cozyfireplace.server.notifications.event.OperationType;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleService;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
import com.cozyfireplace.server.util.exception.ForbiddenException;
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
    private final QuestionRepository questionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BlockService blockService;

    @Transactional
    public Long createCharacter(Long roomId, Account account, CharacterRequest request) {
        RoomTextResponse room = roomRepository.findRoomUrlById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        Character character = characterMapper.toEntity(request, account);
        boolean isCreator = roomSecurityService.isCreator(account);

        character.setRoles(roleService.resolveRoles(
                request.roleIds() == null ? List.of() : List.copyOf(request.roleIds()), roomId
        ));
        Character saved = characterRepository.save(character);

        if (request.blockChanges() != null) {
            String updatedContent = blockService.processBlockChanges(request.blockChanges(), saved);
            saved.setContent(updatedContent);
            saved = characterRepository.save(saved);
        }

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
    public void updateCharacter(Long roomId, Long characterId, CharacterRequest request, Account account) {
        Character character = characterRepository.findByRoomIdAndId(roomId, characterId)
                .orElseThrow(() -> new NotFoundException("Character", characterId));

        RoomTextResponse room = roomRepository.findRoomUrlById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        boolean isCreator = roomSecurityService.isCreator(account);
        boolean isAuthor = character.getAccount() != null && Objects.equals(character.getAccount().getId(), account.getId());
        if (!isCreator && !isAuthor) {
            throw new ForbiddenException("Only author or room creator can update character");
        }

        character = roleService.updateRolesCharacter(character, request.roleIds(), roomId);
        characterMapper.updateEntity(request, character);

        if (request.blockChanges() != null) {
            String updatedContent = blockService.processBlockChanges(request.blockChanges(), character);
            character.setContent(updatedContent);
        }

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
    public void deleteCharacter(Long roomId, Long characterId, Account account) {
        Character character = characterRepository.findByRoomIdAndId(roomId, characterId)
                .orElseThrow(() -> new NotFoundException("Character", characterId));
        checkCharacterEditPermission(character, account);
        characterRepository.deleteById(characterId);
    }

    /**
     * Комната уже проверена запросом ({@code findByRoomIdAndId}), а аккаунт гарантирован
     * {@code @CurrentAccount}, поэтому остаётся проверить только авторство либо права создателя комнаты.
     */
    private void checkCharacterEditPermission(Character character, Account account) {
        boolean isCreator = roomSecurityService.isCreator(account);
        boolean isAuthor = character.getAccount() != null && Objects.equals(character.getAccount().getId(), account.getId());
        if (!isCreator && !isAuthor) {
            throw new ForbiddenException("Only author or room creator can delete character");
        }
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

        boolean isCreator = roomSecurityService.isCreator(account);
        boolean isAuthor = Objects.equals(s.getAccountId(), account.getId());
        if (!isCreator && !isAuthor) {
            throw new ForbiddenException("Only author or room creator can edit character");
        }

        List<Long> ids = List.of(s.getId());

        Map<Long, List<IdName>> rolesByCharacterId = characterRepository.findRolesByCharacterIds(ids).stream()
                .collect(Collectors.groupingBy(
                        CharacterRepository.CharacterRoleRow::getCharacterId,
                        Collectors.mapping(r -> new IdName(r.getRoleId(), r.getRoleName()), Collectors.toList())
                ));

        String expandedContent = blockService.expandBlocks(s.getContent(), account);

        return characterMapper.toEditResponse(
                s,
                expandedContent,
                rolesByCharacterId.getOrDefault(s.getId(), List.of())
        );
    }

    // ====== ONE: user view ======
    @Transactional(readOnly = true)
    public CharacterUserResponse getCharacterForUser(Long roomId, Long characterId, Account account) {
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

        // Разворачиваем блоки через BlockService (фильтрует restrictedBlock'и по правам viewer'а)
        String expandedContent = blockService.expandBlocks(s.getContent(), account);

        long questionCount = questionRepository.countUnansweredByCharacterId(s.getId());

        return characterMapper.toUserResponse(
                s,
                expandedContent,
                isAuthor,
                rolesByCharacterId.getOrDefault(s.getId(), List.of()),
                questionCount
        );
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
