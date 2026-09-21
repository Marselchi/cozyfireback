package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.blocks.BlockService;
import com.cozyfireplace.server.lore.dto.*;
import com.cozyfireplace.server.loreViews.LoreViewService;
import com.cozyfireplace.server.notifications.event.LoreEvent;
import com.cozyfireplace.server.notifications.event.OperationType;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleService;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
import com.cozyfireplace.server.tags.TagService;
import com.cozyfireplace.server.util.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class LoreService {

    private final LoreRepository loreRepository;
    private final RoomRepository roomRepository;
    private final RoleService roleService;
    private final RoomSecurityService roomSecurityService;
    private final TagService tagService;
    private final LoreMapper loreMapper;
    private final LoreViewService loreViewService;
    private final LoreAnchorExtractor loreAnchorExtractor;
    private final QuestionRepository questionRepository;
    private final BlockService blockService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createLore(Long roomId, Account account, LoreRequest request) {
        RoomTextResponse room = roomRepository.findRoomUrlById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        Lore lore = loreMapper.toEntity(request, account);

        boolean isCreator = roomSecurityService.isCreator(account);

        lore.setRoles(roleService.resolveRoles(
                request.roleIds() == null ? List.of() : List.copyOf(request.roleIds())
        ));
        lore.setTags(tagService.resolveTags(
                request.tagIds() == null ? List.of() : List.copyOf(request.tagIds())
        ));

        // Сначала сохраняем Lore, чтобы получить ID
        Lore saved = loreRepository.save(lore);

        // Обработка блоков через BlockService (теперь lore имеет ID)
        if (request.blockChanges() != null) {
            String updatedContent = blockService.processBlockChanges(request.blockChanges(), saved);
            saved.setContent(updatedContent);
            saved = loreRepository.save(saved);
        }

        loreViewService.setViewed(account.getId(), saved.getId());
        eventPublisher.publishEvent(
                LoreEvent.builder()
                        .type(OperationType.CREATE)
                        .roomId(roomId)
                        .roomName(room.getRoomName())
                        .roomUrl(room.getRoomUrl())
                        .loreId(saved.getId())
                        .roleIds(request.roleIds())
                        .title(request.title())
                        .authorAccountId(account.getId())
                        .byDm(isCreator)
                        .build()
        );

        return saved.getId();
    }

    @Transactional
    public void updateLore(Long loreId, LoreRequest request, Account account, Long roomId) {
        Lore lore = loreRepository.findById(loreId)
                .orElseThrow(() -> new NotFoundException("Lore", loreId));
        RoomTextResponse room = roomRepository.findRoomUrlById(roomId)
                .orElseThrow(() -> new NotFoundException("Room", roomId));

        lore = roleService.updateRolesLore(lore, request.roleIds());
        lore = tagService.updateTagsLore(lore, request.tagIds());
        loreMapper.updateEntity(request, lore);

        // Обработка блоков через BlockService
        if (request.blockChanges() != null) {
            String updatedContent = blockService.processBlockChanges(request.blockChanges(), lore);
            lore.setContent(updatedContent);

        }
        loreRepository.save(lore);

        loreViewService.loreUpdated(loreId);
        eventPublisher.publishEvent(
                LoreEvent.builder()
                        .type(OperationType.UPDATE)
                        .authorAccountId(account.getId())
                        .roomName(room.getRoomName())
                        .roomUrl(room.getRoomUrl())
                        .roomId(roomId)
                        .loreId(loreId)
                        .roleIds(request.roleIds())
                        .title(request.title())
                        .build()
        );
    }

    @Transactional
    public void deleteLore(Long loreId) {
        loreRepository.deleteById(loreId);
    }

    // ====== LIST (pagination + filters) ======
    @Transactional(readOnly = true)
    public Page<LoreListResponse> getLoreList(Long roomId,
                                              Account account,
                                              LoreListFilter filter,
                                              Pageable pageable) {

        boolean canSeeAll = roomSecurityService.isCreator(account);

        Set<Long> viewerRoleIds = getAccountRoleIds(account);

        long[] roleArr = viewerRoleIds == null ? new long[0] : viewerRoleIds.stream().mapToLong(Long::longValue).toArray();
        boolean viewerHasRoles = roleArr.length > 0;

        Set<Long> tagIds = Set.of();
        if (filter != null && filter.tagNames() != null && !filter.tagNames().isEmpty()) {
            tagIds = tagService.resolveTagIdsByNames(roomId, filter.tagNames());
        }

        long[] tagArr = tagIds.stream().mapToLong(Long::longValue).toArray();
        int tagCount = tagArr.length;

        String title = (filter == null || filter.title() == null || filter.title().isBlank())
                ? null
                : filter.title();

        Boolean createdByCreator = (filter == null) ? null : filter.createdByRoomCreator();

        String status = (filter == null || filter.status() == null) ? null : filter.status().name();

        Page<Long> idPage = loreRepository.findLoreIdsFiltered(
                roomId,
                canSeeAll,
                viewerHasRoles,
                roleArr,
                createdByCreator,
                title,
                tagCount,
                tagArr,
                status,
                account.getId(),
                pageable
        );

        List<Long> ids = idPage.getContent();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }

        // summaries for ids
        Map<Long, LoreSummaryRow> summaryById = loreRepository.findSummariesByIds(ids, account.getId()).stream()
                .collect(Collectors.toMap(LoreSummaryRow::getId, s -> s));

        // tags for ids
        Map<Long, List<IdName>> tagsByLoreId = loreRepository.findTagsByLoreIds(ids).stream()
                .collect(Collectors.groupingBy(
                        LoreRepository.LoreTagRow::getLoreId,
                        Collectors.mapping(t -> new IdName(t.getTagId(), t.getTagName()), Collectors.toList())
                ));

        // preserve order of ids (важно для сортировки/страниц)
        List<LoreListResponse> content = ids.stream()
                .map(id -> {
                    LoreSummaryRow s = summaryById.get(id);
                    if (s == null) return null;
                    return loreMapper.toListResponse(
                            s,
                            !canSeeAll && s.isNonPublic(),
                            tagsByLoreId.getOrDefault(id, List.of())
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, idPage.getTotalElements());
    }

    // ====== ONE: edit ======
    @Transactional(readOnly = true)
    public LoreResponse getLoreForEdit(Long roomId, Long loreId, Account account) {
        assertCanReadLore(account, roomId, loreId, getAccountRoleIds(account));

        LoreSummaryRow s = loreRepository.findSummaryByIdAndRoomId(roomId, loreId)
                .orElseThrow(() -> new NotFoundException("Lore", loreId));

        List<Long> ids = List.of(s.getId());

        Map<Long, List<IdName>> rolesByLoreId = loreRepository.findRolesByLoreIds(ids).stream()
                .collect(Collectors.groupingBy(
                        LoreRepository.LoreRoleRow::getLoreId,
                        Collectors.mapping(r -> new IdName(r.getRoleId(), r.getRoleName()), Collectors.toList())
                ));

        Map<Long, List<IdName>> tagsByLoreId = loreRepository.findTagsByLoreIds(ids).stream()
                .collect(Collectors.groupingBy(
                        LoreRepository.LoreTagRow::getLoreId,
                        Collectors.mapping(t -> new IdName(t.getTagId(), t.getTagName()), Collectors.toList())
                ));

        String expandedContent = blockService.expandBlocks(s.getContent(), account);

        return loreMapper.toEditResponse(
                s,
                expandedContent,
                rolesByLoreId.getOrDefault(s.getId(), List.of()),
                tagsByLoreId.getOrDefault(s.getId(), List.of())
        );
    }

    // ====== ONE: user view ======
    @Transactional(readOnly = true)
    public LoreUserResponse getLoreForUser(Long roomId, Long loreId, Account account) {
        boolean canSeeAll = roomSecurityService.isCreator(account);

        Set<Long> roleIds = getAccountRoleIds(account);
        if (!canSeeAll) {
            assertCanReadLore(account, roomId, loreId, roleIds);
        }

        LoreSummaryRow s = loreRepository.findSummaryByIdAndRoomId(roomId, loreId)
                .orElseThrow(() -> new NotFoundException("Lore", loreId));

        boolean isAuthor = Objects.equals(s.getAccountId(), account.getId());

        // tags как было
        List<Long> ids = List.of(s.getId());
        Map<Long, List<IdName>> tagsByLoreId = loreRepository.findTagsByLoreIds(ids).stream()
                .collect(Collectors.groupingBy(
                        LoreRepository.LoreTagRow::getLoreId,
                        Collectors.mapping(t -> new IdName(t.getTagId(), t.getTagName()), Collectors.toList())
                ));

        // Разворачиваем блоки через BlockService
        String expandedContent = blockService.expandBlocks(s.getContent(), account);

        return loreMapper.toUserResponse(
                s,
                expandedContent,
                isAuthor,
                !canSeeAll && s.isNonPublic(),
                tagsByLoreId.getOrDefault(s.getId(), List.of())
        );
    }


    private Map<Long, ContentTitleRow> getInlineContent(Long roomId, List<Long> loreIds, boolean canSeeAll, Set<Long> roleIds) {
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

    private void assertCanReadLore(Account account, Long roomId, Long loreId, Set<Long> viewerRoleIds) {
        boolean canSeeAll = roomSecurityService.isCreator(account);

        long[] roleArr = viewerRoleIds == null ? new long[0] : viewerRoleIds.stream().mapToLong(Long::longValue).toArray();
        boolean viewerHasRoles = roleArr.length > 0;

        boolean ok = loreRepository.existsVisibleLore(roomId, loreId, canSeeAll, viewerHasRoles, roleArr);
        if (!ok) {
            throw new NotFoundException("Lore", loreId);
        }
    }

    public Set<Long> getAccountRoleIds(Account account) {
        return account.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
    }
}