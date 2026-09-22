package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.blocks.BlockService;
import com.cozyfireplace.server.lore.blocks.dto.BlockChangesRequest;
import com.cozyfireplace.server.lore.dto.*;
import com.cozyfireplace.server.loreViews.LoreViewService;
import com.cozyfireplace.server.notifications.event.LoreEvent;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleService;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
import com.cozyfireplace.server.tags.TagService;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure Mockito unit tests for {@link LoreService}.
 * <p>
 * All collaborators (repositories, mappers, security, block/tag/role services)
 * are mocked; only the service's own branching, entity plumbing and event
 * publishing are under test. Integration behaviour (SQL queries, real
 * visibility rules) is deliberately out of scope here.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoreService")
class LoreServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long LORE_ID = 100L;
    private static final Long ACCOUNT_ID = 7L;

    @Mock
    private LoreRepository loreRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private RoomSecurityService roomSecurityService;
    @Mock
    private TagService tagService;
    @Mock
    private LoreMapper loreMapper;
    @Mock
    private LoreViewService loreViewService;
    @Mock
    @SuppressWarnings("unused")
    private LoreAnchorExtractor loreAnchorExtractor;
    @Mock
    @SuppressWarnings("unused")
    private QuestionRepository questionRepository;
    @Mock
    private BlockService blockService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LoreService loreService;

    // ================= helpers =================

    private Account accountWithRoles(Role... roles) {
        Account account = mock(Account.class);
        lenient().when(account.getId()).thenReturn(ACCOUNT_ID);
        lenient().when(account.getRoles()).thenReturn(Set.of(roles));
        return account;
    }

    private Role role(long id) {
        Role role = mock(Role.class);
        lenient().when(role.getId()).thenReturn(id);
        return role;
    }

    private RoomTextResponse roomResponse() {
        RoomTextResponse room = mock(RoomTextResponse.class);
        lenient().when(room.getRoomName()).thenReturn("Test Room");
        lenient().when(room.getRoomUrl()).thenReturn("/test-room");
        return room;
    }

    private LoreRequest request(BlockChangesRequest blockChanges) {
        return new LoreRequest("Title", "Desc", "Date", "Content",
                blockChanges, Set.of(2L), Set.of(3L));
    }

    private LoreRequest request() {
        return request(null);
    }

    /** Mocks the repository save so the persisted Lore receives a generated id. */
    private void stubSaveAssignsId() {
        when(loreRepository.save(any(Lore.class))).thenAnswer(inv -> {
            Lore l = inv.getArgument(0);
            l.setId(LORE_ID);
            return l;
        });
    }

    private LoreRepository.LoreTagRow tagRow(String tagName) {
        return new LoreRepository.LoreTagRow() {
            @Override
            public Long getLoreId() {
                return LORE_ID;
            }

            @Override
            public Long getTagId() {
                return 5L;
            }

            @Override
            public String getTagName() {
                return tagName;
            }
        };
    }

    private LoreRepository.LoreRoleRow roleRow() {
        return new LoreRepository.LoreRoleRow() {
            @Override
            public Long getLoreId() {
                return LORE_ID;
            }

            @Override
            public Long getRoleId() {
                return 4L;
            }

            @Override
            public String getRoleName() {
                return "Paladin";
            }
        };
    }

    // ================= createLore =================

    @Nested
    @DisplayName("createLore")
    class CreateLore {

        @Test
        @DisplayName("persists the lore, resolves roles/tags, marks it viewed and publishes a CREATE event")
        void success() {
            Account account = accountWithRoles();
            Lore lore = Lore.builder().title("Title").build();
            Role r2 = role(2);
            Optional<RoomTextResponse> roomOpt = Optional.of(roomResponse());
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(roomOpt);
            when(loreMapper.toEntity(any(LoreRequest.class), eq(account))).thenReturn(lore);
            when(roleService.resolveRoles(List.of(2L))).thenReturn(Set.of(r2));
            when(tagService.resolveTags(List.of(3L))).thenReturn(Set.of());
            when(roomSecurityService.isCreator(account)).thenReturn(true);
            stubSaveAssignsId();

            Long id = loreService.createLore(ROOM_ID, account, request());

            assertEquals(LORE_ID, id);
            assertEquals(Set.of(r2), lore.getRoles());
            verify(loreRepository).save(lore);
            verify(loreViewService).setViewed(ACCOUNT_ID, LORE_ID);

            ArgumentCaptor<LoreEvent> captor = ArgumentCaptor.forClass(LoreEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            LoreEvent event = captor.getValue();
            assertEquals(LORE_ID, event.loreId());
            assertEquals(ROOM_ID, event.roomId());
            assertEquals("Test Room", event.roomName());
            assertEquals("/test-room", event.roomUrl());
            assertEquals("Title", event.title());
            assertEquals(Set.of(2L), event.roleIds());
            assertEquals(ACCOUNT_ID, event.authorAccountId());
            assertTrue(event.byDm());
        }

        @Test
        @DisplayName("processes block changes and re-saves the lore with the produced content")
        void withBlockChanges() {
            Account account = accountWithRoles();
            Lore lore = Lore.builder().title("Title").content("old").build();
            BlockChangesRequest changes = new BlockChangesRequest();
            Optional<RoomTextResponse> roomOpt = Optional.of(roomResponse());
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(roomOpt);
            when(loreMapper.toEntity(any(LoreRequest.class), eq(account))).thenReturn(lore);
            when(roleService.resolveRoles(anyList())).thenReturn(Set.of());
            when(tagService.resolveTags(anyList())).thenReturn(Set.of());
            stubSaveAssignsId();
            when(blockService.processBlockChanges(changes, lore)).thenReturn("{restrictedBlock id=\"5\"}");

            Long id = loreService.createLore(ROOM_ID, account, request(changes));

            assertEquals(LORE_ID, id);
            assertEquals("{restrictedBlock id=\"5\"}", lore.getContent());
            verify(loreRepository, times(2)).save(lore);
        }

        @Test
        @DisplayName("null role/tag id sets are resolved as empty lists")
        void nullRoleAndTagIds() {
            Account account = accountWithRoles();
            LoreRequest req = new LoreRequest("T", null, null, "C", null, null, null);
            Optional<RoomTextResponse> roomOpt = Optional.of(roomResponse());
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(roomOpt);
            when(loreMapper.toEntity(any(LoreRequest.class), eq(account))).thenReturn(Lore.builder().build());
            when(roleService.resolveRoles(List.of())).thenReturn(Set.of());
            when(tagService.resolveTags(List.of())).thenReturn(Set.of());
            stubSaveAssignsId();

            assertNotNull(loreService.createLore(ROOM_ID, account, req));
            verify(roleService).resolveRoles(List.of());
            verify(tagService).resolveTags(List.of());
        }

        @Test
        @DisplayName("byDm is false when the account is not the room creator")
        void nonCreatorAuthor() {
            Account account = accountWithRoles();
            Optional<RoomTextResponse> roomOpt = Optional.of(roomResponse());
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(roomOpt);
            when(loreMapper.toEntity(any(LoreRequest.class), eq(account))).thenReturn(Lore.builder().build());
            when(roleService.resolveRoles(anyList())).thenReturn(Set.of());
            when(tagService.resolveTags(anyList())).thenReturn(Set.of());
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            stubSaveAssignsId();

            loreService.createLore(ROOM_ID, account, request());

            ArgumentCaptor<LoreEvent> captor = ArgumentCaptor.forClass(LoreEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertFalse(captor.getValue().byDm());
        }

        @Test
        @DisplayName("throws NotFoundException when the room does not exist")
        void roomNotFound() {
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> loreService.createLore(ROOM_ID, accountWithRoles(), request()));
            verify(loreRepository, never()).save(any());
        }
    }

    // ================= updateLore =================

    @Nested
    @DisplayName("updateLore")
    class UpdateLore {

        @Test
        @DisplayName("updates roles/tags/entity fields, saves, resets views cache and publishes an UPDATE event")
        void success() {
            Account account = accountWithRoles();
            Lore lore = Lore.builder().id(LORE_ID).content("old").build();
            LoreRequest req = request();
            when(loreRepository.findById(LORE_ID)).thenReturn(Optional.of(lore));
            Optional<RoomTextResponse> roomOpt = Optional.of(roomResponse());
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(roomOpt);
            when(roleService.updateRolesLore(lore, req.roleIds())).thenReturn(lore);
            when(tagService.updateTagsLore(lore, req.tagIds())).thenReturn(lore);

            loreService.updateLore(LORE_ID, req, account, ROOM_ID);

            verify(loreMapper).updateEntity(req, lore);
            verify(loreRepository).save(lore);
            verify(loreViewService).loreUpdated(LORE_ID);

            ArgumentCaptor<LoreEvent> captor = ArgumentCaptor.forClass(LoreEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            LoreEvent event = captor.getValue();
            assertEquals(LORE_ID, event.loreId());
            assertEquals(ROOM_ID, event.roomId());
            assertEquals("Title", event.title());
            assertEquals(ACCOUNT_ID, event.authorAccountId());
        }

        @Test
        @DisplayName("applies block changes to the content before saving")
        void withBlockChanges() {
            Account account = accountWithRoles();
            Lore lore = Lore.builder().id(LORE_ID).content("old").build();
            BlockChangesRequest changes = new BlockChangesRequest();
            LoreRequest req = request(changes);
            when(loreRepository.findById(LORE_ID)).thenReturn(Optional.of(lore));
            Optional<RoomTextResponse> roomOpt = Optional.of(roomResponse());
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(roomOpt);
            when(roleService.updateRolesLore(eq(lore), any())).thenReturn(lore);
            when(tagService.updateTagsLore(eq(lore), any())).thenReturn(lore);
            when(blockService.processBlockChanges(changes, lore)).thenReturn("new content");

            loreService.updateLore(LORE_ID, req, account, ROOM_ID);

            assertEquals("new content", lore.getContent());
            verify(loreRepository).save(lore);
        }

        @Test
        @DisplayName("throws NotFoundException when the lore does not exist")
        void loreNotFound() {
            when(loreRepository.findById(LORE_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> loreService.updateLore(LORE_ID, request(), accountWithRoles(), ROOM_ID));
        }

        @Test
        @DisplayName("throws NotFoundException when the room does not exist")
        void roomNotFound() {
            when(loreRepository.findById(LORE_ID)).thenReturn(Optional.of(Lore.builder().build()));
            when(roomRepository.findRoomUrlById(ROOM_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> loreService.updateLore(LORE_ID, request(), accountWithRoles(), ROOM_ID));
        }
    }

    // ================= deleteLore =================

    @Nested
    @DisplayName("deleteLore")
    class DeleteLore {

        @Test
        @DisplayName("delegates to the repository")
        void success() {
            loreService.deleteLore(LORE_ID);
            verify(loreRepository).deleteById(LORE_ID);
        }
    }

    // ================= getLoreList =================

    @Nested
    @DisplayName("getLoreList")
    class GetLoreList {

        @Test
        @DisplayName("returns an empty page without hitting the summary queries when no ids match")
        void emptyPage() {
            Account account = accountWithRoles();
            Pageable pageable = PageRequest.of(0, 10);
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(loreRepository.findLoreIdsFiltered(
                    eq(ROOM_ID), anyBoolean(), anyBoolean(), any(long[].class),
                    any(), any(), anyInt(), any(long[].class), any(), eq(ACCOUNT_ID), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<LoreListResponse> result = loreService.getLoreList(ROOM_ID, account, null, pageable);

            assertTrue(result.isEmpty());
            assertEquals(0L, result.getTotalElements());
            verify(loreRepository, never()).findSummariesByIds(anyList(), anyLong());
        }

        @Test
        @DisplayName("maps summaries with tags and preserves page metadata")
        void populatedPage() {
            Account account = accountWithRoles();
            Pageable pageable = PageRequest.of(0, 10);
            LoreSummaryRow summary = mock(LoreSummaryRow.class);
            when(summary.getId()).thenReturn(LORE_ID);
            // isNonPublic is not consulted: the creator short-circuits '!canSeeAll &&'
            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(loreRepository.findLoreIdsFiltered(
                    eq(ROOM_ID), anyBoolean(), anyBoolean(), any(long[].class),
                    any(), any(), anyInt(), any(long[].class), any(), eq(ACCOUNT_ID), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(LORE_ID), pageable, 1));
            when(loreRepository.findSummariesByIds(List.of(LORE_ID), ACCOUNT_ID))
                    .thenReturn(List.of(summary));
            when(loreRepository.findTagsByLoreIds(List.of(LORE_ID)))
                    .thenReturn(List.of(tagRow("npc")));
            LoreListResponse response = mock(LoreListResponse.class);
            when(loreMapper.toListResponse(eq(summary), eq(false), eq(List.of(new IdName(5L, "npc")))))
                    .thenReturn(response);

            Page<LoreListResponse> result = loreService.getLoreList(ROOM_ID, account, null, pageable);

            assertEquals(List.of(response), result.getContent());
            assertEquals(1L, result.getTotalElements());
        }

        @Test
        @DisplayName("non-creator viewing a non-public summary gets the nonPublic flag set")
        void nonPublicFlagForRegularUser() {
            Account account = accountWithRoles();
            Pageable pageable = PageRequest.of(0, 10);
            LoreSummaryRow summary = mock(LoreSummaryRow.class);
            when(summary.getId()).thenReturn(LORE_ID);
            when(summary.isNonPublic()).thenReturn(true);
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(loreRepository.findLoreIdsFiltered(
                    eq(ROOM_ID), anyBoolean(), anyBoolean(), any(long[].class),
                    any(), any(), anyInt(), any(long[].class), any(), eq(ACCOUNT_ID), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(LORE_ID), pageable, 1));
            when(loreRepository.findSummariesByIds(List.of(LORE_ID), ACCOUNT_ID))
                    .thenReturn(List.of(summary));
            when(loreRepository.findTagsByLoreIds(List.of(LORE_ID))).thenReturn(List.of());
            when(loreMapper.toListResponse(eq(summary), eq(true), eq(List.of())))
                    .thenReturn(mock(LoreListResponse.class));

            assertEquals(1, loreService.getLoreList(ROOM_ID, account, null, pageable).getContent().size());
        }

        @Test
        @DisplayName("rows whose summary disappeared between queries are skipped")
        void missingSummarySkipped() {
            Account account = accountWithRoles();
            Pageable pageable = PageRequest.of(0, 10);
            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(loreRepository.findLoreIdsFiltered(
                    eq(ROOM_ID), anyBoolean(), anyBoolean(), any(long[].class),
                    any(), any(), anyInt(), any(long[].class), any(), eq(ACCOUNT_ID), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(LORE_ID), pageable, 1));
            when(loreRepository.findSummariesByIds(List.of(LORE_ID), ACCOUNT_ID)).thenReturn(List.of());
            when(loreRepository.findTagsByLoreIds(List.of(LORE_ID))).thenReturn(List.of());

            Page<LoreListResponse> result = loreService.getLoreList(ROOM_ID, account, null, pageable);

            assertTrue(result.getContent().isEmpty());
            verify(loreMapper, never()).toListResponse(any(), anyBoolean(), anyList());
        }

        @Test
        @DisplayName("filter values are translated into repository arguments")
        void filterTranslation() {
            Account account = accountWithRoles(role(11));
            Pageable pageable = PageRequest.of(0, 10);
            LoreListFilter filter = new LoreListFilter(true, Set.of("npc"), "  Some title  ", LoreStatus.viewed);
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(tagService.resolveTagIdsByNames(ROOM_ID, Set.of("npc"))).thenReturn(Set.of(21L));
            when(loreRepository.findLoreIdsFiltered(
                    any(), anyBoolean(), anyBoolean(), any(long[].class),
                    any(), any(), anyInt(), any(long[].class), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            loreService.getLoreList(ROOM_ID, account, filter, pageable);

            ArgumentCaptor<Long> roomIdCap = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Boolean> canSeeAllCap = ArgumentCaptor.forClass(Boolean.class);
            ArgumentCaptor<Boolean> hasRolesCap = ArgumentCaptor.forClass(Boolean.class);
            ArgumentCaptor<long[]> rolesCap = ArgumentCaptor.forClass(long[].class);
            ArgumentCaptor<Boolean> byCreatorCap = ArgumentCaptor.forClass(Boolean.class);
            ArgumentCaptor<String> titleCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Integer> tagCountCap = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<long[]> tagIdsCap = ArgumentCaptor.forClass(long[].class);
            ArgumentCaptor<String> statusCap = ArgumentCaptor.forClass(String.class);
            verify(loreRepository).findLoreIdsFiltered(
                    roomIdCap.capture(), canSeeAllCap.capture(), hasRolesCap.capture(), rolesCap.capture(),
                    byCreatorCap.capture(), titleCap.capture(), tagCountCap.capture(), tagIdsCap.capture(),
                    statusCap.capture(), eq(ACCOUNT_ID), eq(pageable));

            assertEquals(ROOM_ID, roomIdCap.getValue());
            assertFalse(canSeeAllCap.getValue());
            assertTrue(hasRolesCap.getValue());
            assertArrayEquals(new long[]{11L}, rolesCap.getValue());
            assertTrue(byCreatorCap.getValue());
            assertEquals("  Some title  ", titleCap.getValue());
            assertEquals(1, tagCountCap.getValue());
            assertArrayEquals(new long[]{21L}, tagIdsCap.getValue());
            assertEquals("viewed", statusCap.getValue());
        }

        @Test
        @DisplayName("blank title in the filter is passed as null")
        void blankTitleBecomesNull() {
            Account account = accountWithRoles();
            Pageable pageable = PageRequest.of(0, 10);
            LoreListFilter filter = new LoreListFilter(null, Set.of(), "   ", null);
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(loreRepository.findLoreIdsFiltered(
                    any(), anyBoolean(), anyBoolean(), any(long[].class),
                    any(), any(), anyInt(), any(long[].class), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            loreService.getLoreList(ROOM_ID, account, filter, pageable);

            verify(loreRepository).findLoreIdsFiltered(
                    eq(ROOM_ID), eq(false), eq(false), any(long[].class),
                    isNull(), isNull(), eq(0), any(long[].class), isNull(), eq(ACCOUNT_ID), eq(pageable));
            verify(tagService, never()).resolveTagIdsByNames(anyLong(), anySet());
        }
    }

    // ================= getLoreForEdit =================

    @Nested
    @DisplayName("getLoreForEdit")
    class GetLoreForEdit {

        @Test
        @DisplayName("returns the edit response with expanded blocks, roles and tags")
        void success() {
            Account account = accountWithRoles(role(11));
            LoreSummaryRow summary = mock(LoreSummaryRow.class);
            when(summary.getId()).thenReturn(LORE_ID);
            when(summary.getContent()).thenReturn("raw content");
            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(loreRepository.existsVisibleLore(eq(ROOM_ID), eq(LORE_ID), eq(true), eq(true), any(long[].class)))
                    .thenReturn(true);
            when(loreRepository.findSummaryByIdAndRoomId(ROOM_ID, LORE_ID)).thenReturn(Optional.of(summary));
            when(loreRepository.findRolesByLoreIds(List.of(LORE_ID)))
                    .thenReturn(List.of(roleRow()));
            when(loreRepository.findTagsByLoreIds(List.of(LORE_ID)))
                    .thenReturn(List.of(tagRow("npc")));
            when(blockService.expandBlocks("raw content", account)).thenReturn("expanded");
            LoreResponse response = mock(LoreResponse.class);
            when(loreMapper.toEditResponse(summary, "expanded",
                    List.of(new IdName(4L, "Paladin")), List.of(new IdName(5L, "npc"))))
                    .thenReturn(response);

            LoreResponse result = loreService.getLoreForEdit(ROOM_ID, LORE_ID, account);

            assertSame(response, result);
        }

        @Test
        @DisplayName("throws NotFoundException when the lore is not visible to the account")
        void notVisible() {
            Account account = accountWithRoles();
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(loreRepository.existsVisibleLore(eq(ROOM_ID), eq(LORE_ID), eq(false), eq(false), any(long[].class)))
                    .thenReturn(false);

            assertThrows(NotFoundException.class,
                    () -> loreService.getLoreForEdit(ROOM_ID, LORE_ID, account));
            verify(loreRepository, never()).findSummaryByIdAndRoomId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("throws NotFoundException when the summary row is missing")
        void summaryMissing() {
            Account account = accountWithRoles();
            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(loreRepository.existsVisibleLore(eq(ROOM_ID), eq(LORE_ID), eq(true), eq(false), any(long[].class)))
                    .thenReturn(true);
            when(loreRepository.findSummaryByIdAndRoomId(ROOM_ID, LORE_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> loreService.getLoreForEdit(ROOM_ID, LORE_ID, account));
        }
    }

    // ================= getLoreForUser =================

    @Nested
    @DisplayName("getLoreForUser")
    class GetLoreForUser {

        @Test
        @DisplayName("creator bypasses the visibility check and sees the expanded content")
        void creatorSeesEverything() {
            Account account = accountWithRoles();
            LoreSummaryRow summary = mock(LoreSummaryRow.class);
            when(summary.getId()).thenReturn(LORE_ID);
            when(summary.getAccountId()).thenReturn(ACCOUNT_ID);
            when(summary.getContent()).thenReturn("raw");
            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(loreRepository.findSummaryByIdAndRoomId(ROOM_ID, LORE_ID)).thenReturn(Optional.of(summary));
            when(loreRepository.findTagsByLoreIds(List.of(LORE_ID))).thenReturn(List.of());
            when(blockService.expandBlocks("raw", account)).thenReturn("expanded");
            LoreUserResponse response = mock(LoreUserResponse.class);
            when(loreMapper.toUserResponse(summary, "expanded", true, false, List.of()))
                    .thenReturn(response);

            LoreUserResponse result = loreService.getLoreForUser(ROOM_ID, LORE_ID, account);

            assertSame(response, result);
            verify(loreRepository, never()).existsVisibleLore(anyLong(), anyLong(), anyBoolean(), anyBoolean(), any());
        }

        @Test
        @DisplayName("non-creator on someone else's non-public lore gets nonPublic=true and isAuthor=false")
        void regularUserOnForeignLore() {
            Account account = accountWithRoles(role(11));
            LoreSummaryRow summary = mock(LoreSummaryRow.class);
            when(summary.getId()).thenReturn(LORE_ID);
            when(summary.getAccountId()).thenReturn(999L);
            when(summary.isNonPublic()).thenReturn(true);
            when(summary.getContent()).thenReturn("raw");
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(loreRepository.existsVisibleLore(eq(ROOM_ID), eq(LORE_ID), eq(false), eq(true), any(long[].class)))
                    .thenReturn(true);
            when(loreRepository.findSummaryByIdAndRoomId(ROOM_ID, LORE_ID)).thenReturn(Optional.of(summary));
            when(loreRepository.findTagsByLoreIds(List.of(LORE_ID)))
                    .thenReturn(List.of(tagRow("location")));
            when(blockService.expandBlocks("raw", account)).thenReturn("clean");
            LoreUserResponse response = mock(LoreUserResponse.class);
            when(loreMapper.toUserResponse(summary, "clean", false, true, List.of(new IdName(5L, "location"))))
                    .thenReturn(response);

            LoreUserResponse result = loreService.getLoreForUser(ROOM_ID, LORE_ID, account);

            assertSame(response, result);
        }

        @Test
        @DisplayName("non-creator without read access gets NotFoundException")
        void regularUserDenied() {
            Account account = accountWithRoles();
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(loreRepository.existsVisibleLore(eq(ROOM_ID), eq(LORE_ID), eq(false), eq(false), any(long[].class)))
                    .thenReturn(false);

            assertThrows(NotFoundException.class,
                    () -> loreService.getLoreForUser(ROOM_ID, LORE_ID, account));
        }

        @Test
        @DisplayName("throws NotFoundException when the summary row is missing")
        void summaryMissing() {
            Account account = accountWithRoles();
            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(loreRepository.findSummaryByIdAndRoomId(ROOM_ID, LORE_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> loreService.getLoreForUser(ROOM_ID, LORE_ID, account));
        }
    }

    // ================= getAccountRoleIds =================

    @Nested
    @DisplayName("getAccountRoleIds")
    class GetAccountRoleIds {

        @Test
        @DisplayName("collects the ids of all account roles")
        void mapsRoleIds() {
            Account account = accountWithRoles(role(1), role(2));
            assertEquals(Set.of(1L, 2L), loreService.getAccountRoleIds(account));
        }

        @Test
        @DisplayName("account without roles yields an empty set")
        void noRoles() {
            assertEquals(Set.of(), loreService.getAccountRoleIds(accountWithRoles()));
        }
    }
}
