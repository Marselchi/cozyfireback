package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.dto.*;
import com.cozyfireplace.server.loreViews.LoreViewService;
import com.cozyfireplace.server.notifications.event.LoreEvent;
import com.cozyfireplace.server.notifications.event.OperationType;
import com.cozyfireplace.server.notifications.firebase.FirebaseConfig;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleService;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
import com.cozyfireplace.server.rooms.RoomSecurityService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoreService tests")
class LoreServiceTest {

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
    private LoreAnchorExtractor loreAnchorExtractor;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @MockitoBean
    private FirebaseConfig firebaseConfig;

    @InjectMocks
    private LoreService loreService;

    // ====== HELPERS ======
    private Account testAccount(long id, Set<Role> roles) {
        Account acc = mock(Account.class);
        lenient().when(acc.getId()).thenReturn(id);
        lenient().when(acc.getRoles()).thenReturn(roles);
        return acc;
    }

    private Role testRole(long id, String name) {
        Role role = mock(Role.class);
        when(role.getId()).thenReturn(id);
        // name не нужен в текущей логике, но можно добавить при необходимости
        return role;
    }

    private RoomTextResponse testRoom(long id, String name, String url) {
        RoomTextResponse room = mock(RoomTextResponse.class);
        when(room.getRoomName()).thenReturn(name);
        when(room.getRoomUrl()).thenReturn(url);
        return room;
    }

    // ====== CREATE LORE ======
    @Nested
    @DisplayName("createLore")
    class CreateLoreTests {

        @Test
        @DisplayName("should create lore successfully and publish event")
        void createLore_success() {
            Long roomId = 1L, loreId = 100L, accountId = 1L;
            Account account = testAccount(accountId, Set.of());
            RoomTextResponse roomResp = testRoom(roomId, "Test Room", "/rooms/1");
            Lore lore = mock(Lore.class);
            LoreRequest request = mock(LoreRequest.class);

            when(roomRepository.findRoomUrlById(roomId)).thenReturn(Optional.of(roomResp));
            when(loreMapper.toEntity(request, account)).thenReturn(lore);
            when(roleService.resolveRoles(anyList())).thenReturn(Set.of());
            when(tagService.resolveTags(anyList())).thenReturn(Set.of());
            when(loreRepository.save(any(Lore.class))).thenAnswer(inv -> inv.getArgument(0));
            when(lore.getId()).thenReturn(loreId);
            when(request.roleIds()).thenReturn(null);
            when(request.tagIds()).thenReturn(null);
            when(request.title()).thenReturn("New Lore");
            when(roomSecurityService.isCreator(account)).thenReturn(true);

            Long result = loreService.createLore(roomId, account, request);

            assertEquals(loreId, result);
            verify(loreViewService).setViewed(accountId, loreId);

            ArgumentCaptor<LoreEvent> captor = ArgumentCaptor.forClass(LoreEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            LoreEvent ev = captor.getValue();
            assertEquals(OperationType.CREATE, ev.type());
            assertEquals(roomId, ev.roomId());
            assertTrue(ev.byDm());
        }

        @Test
        @DisplayName("should throw NotFoundException when room not found")
        void createLore_roomNotFound() {
            when(roomRepository.findRoomUrlById(1L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> loreService.createLore(1L, testAccount(1L, Set.of()), mock(LoreRequest.class)));
        }
    }

    // ====== UPDATE LORE ======
    @Nested
    @DisplayName("updateLore")
    class UpdateLoreTests {

        @Test
        @DisplayName("should update lore and publish event")
        void updateLore_success() {
            Long loreId = 100L, roomId = 1L, accountId = 1L;
            Account account = testAccount(accountId, Set.of());
            RoomTextResponse roomResp = testRoom(roomId, "Room", "/r/1");
            Lore lore = mock(Lore.class);
            LoreRequest request = mock(LoreRequest.class);

            when(loreRepository.findById(loreId)).thenReturn(Optional.of(lore));
            when(roomRepository.findRoomUrlById(roomId)).thenReturn(Optional.of(roomResp));
            when(roleService.updateRolesLore(eq(lore), any())).thenReturn(lore);
            when(tagService.updateTagsLore(eq(lore), any())).thenReturn(lore);
            when(request.title()).thenReturn("Upd");
            when(request.roleIds()).thenReturn(Set.of(2L));
            when(request.tagIds()).thenReturn(Set.of(3L));

            loreService.updateLore(loreId, request, account, roomId);

            verify(loreRepository).save(lore);
            verify(loreViewService).loreUpdated(loreId);
        }

        @Test
        @DisplayName("should throw when lore not found")
        void updateLore_loreNotFound() {
            when(loreRepository.findById(100L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> loreService.updateLore(100L, mock(LoreRequest.class), testAccount(1L, Set.of()), 1L));
        }
    }

    // ====== DELETE LORE ======
    @Nested
    @DisplayName("deleteLore")
    class DeleteLoreTests {
        @Test
        void deleteLore_success() {
            loreService.deleteLore(100L);
            verify(loreRepository).deleteById(100L);
        }
    }

    // ====== GET LORE LIST ======
    @Nested
    @DisplayName("getLoreList")
    class GetLoreListTests {

        @Test
        @DisplayName("should return empty page")
        void getLoreList_empty() {
            Pageable pageable = PageRequest.of(0, 10);
            Account account = testAccount(1L, Set.of());
            Page<Long> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(loreRepository.findLoreIdsFiltered(
                    eq(1L), anyBoolean(), anyBoolean(), any(long[].class), any(), any(),
                    anyInt(), any(long[].class), any(), eq(1L), any()))
                    .thenReturn(emptyPage);

            Page<LoreListResponse> result = loreService.getLoreList(1L, account, null, pageable);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return populated page")
        void getLoreList_withData() {
            Pageable pageable = PageRequest.of(0, 10);
            Long loreId = 100L, accountId = 1L;
            Account account = testAccount(accountId, Set.of());
            Page<Long> idPage = new PageImpl<>(List.of(loreId), pageable, 1);
            LoreSummaryRow summary = mock(LoreSummaryRow.class);

            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(loreRepository.findLoreIdsFiltered(
                    eq(1L), anyBoolean(), anyBoolean(), any(long[].class), any(), any(),
                    anyInt(), any(long[].class), any(), eq(accountId), any()))
                    .thenReturn(idPage);
            when(summary.getId()).thenReturn(loreId);
            lenient().when(summary.isNonPublic()).thenReturn(false);
            when(loreRepository.findSummariesByIds(List.of(loreId), accountId))
                    .thenReturn(List.of(summary));
            when(loreRepository.findTagsByLoreIds(List.of(loreId))).thenReturn(List.of());
            when(loreMapper.toListResponse(eq(summary), eq(false), eq(List.of())))
                    .thenReturn(mock(LoreListResponse.class));

            Page<LoreListResponse> result = loreService.getLoreList(1L, account, null, pageable);
            assertEquals(1, result.getContent().size());
        }
    }

    // ====== GET LORE FOR EDIT ======
    @Nested
    @DisplayName("getLoreForEdit")
    class GetLoreForEditTests {

        @Test
        @DisplayName("should return lore for edit")
        void getLoreForEdit_success() {
            Long roomId = 1L, loreId = 100L;
            Account account = testAccount(1L, Set.of());
            LoreSummaryRow summary = mock(LoreSummaryRow.class);

            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(loreRepository.existsVisibleLore(eq(roomId), eq(loreId), eq(true), eq(false), any(long[].class)))
                    .thenReturn(true);
            when(loreRepository.findSummaryByIdAndRoomId(roomId, loreId))
                    .thenReturn(Optional.of(summary));
            when(summary.getId()).thenReturn(loreId);
            when(loreRepository.findRolesByLoreIds(List.of(loreId))).thenReturn(List.of());
            when(loreRepository.findTagsByLoreIds(List.of(loreId))).thenReturn(List.of());

            LoreResponse result = loreService.getLoreForEdit(roomId, loreId, account);
            assertNotNull(result);
        }

        @Test
        @DisplayName("should throw when not accessible")
        void getLoreForEdit_forbidden() {
            Account account = testAccount(1L, Set.of());
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(loreRepository.existsVisibleLore(
                    eq(1L), eq(100L), eq(false), eq(false), any(long[].class)))
                    .thenReturn(false);

            assertThrows(NotFoundException.class,
                    () -> loreService.getLoreForEdit(1L, 100L, account));
        }
    }

    // ====== GET LORE FOR USER ======
    @Nested
    @DisplayName("getLoreForUser")
    class GetLoreForUserTests {

        @Test
        @DisplayName("should return user response")
        void getLoreForUser_success() {
            Long roomId = 1L, loreId = 100L, accountId = 1L;
            Account account = testAccount(accountId, Set.of());
            LoreSummaryRow summary = mock(LoreSummaryRow.class);

            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(loreRepository.findSummaryByIdAndRoomId(roomId, loreId))
                    .thenReturn(Optional.of(summary));
            when(summary.getId()).thenReturn(loreId);
            when(summary.getAccountId()).thenReturn(accountId);
            when(summary.getContent()).thenReturn("Content");
            when(loreRepository.findTagsByLoreIds(List.of(loreId))).thenReturn(List.of());
            when(loreMapper.toUserResponse(any(), anyString(), eq(true), eq(false), anyList()))
                    .thenReturn(mock(LoreUserResponse.class));

            LoreUserResponse result = loreService.getLoreForUser(roomId, loreId, account);
            assertNotNull(result);
        }
    }

    // ====== GET LORE FOR USER INLINE ======
    @Nested
    @DisplayName("getLoreForUserInline")
    class GetLoreForUserInlineTests {

        @Test
        @DisplayName("should return inline response with excerpts")
        void getLoreForUserInline_success() {
            Long roomId = 1L, loreId = 100L, accountId = 1L;
            Account account = testAccount(accountId, Set.of());
            LoreSummaryRow summary = mock(LoreSummaryRow.class);
            LoreAnchorExtractor.AnchorLink anchor = mock(LoreAnchorExtractor.AnchorLink.class);
            ContentTitleRow contentRow = mock(ContentTitleRow.class);
            String content = "# Header\nText";

            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(loreRepository.findSummaryByIdAndRoomId(roomId, loreId))
                    .thenReturn(Optional.of(summary));
            when(summary.getId()).thenReturn(loreId);
            when(summary.getAccountId()).thenReturn(accountId);
            when(summary.getContent()).thenReturn(content);
            lenient().when(summary.getTitle()).thenReturn("Title");
            when(loreRepository.findTagsByLoreIds(List.of(loreId))).thenReturn(List.of());
            when(loreAnchorExtractor.extractAnchorLinks(content))
                    .thenReturn(new ArrayList<>(List.of(anchor)));
            when(anchor.loreId()).thenReturn(loreId);
            when(anchor.header()).thenReturn("header");
            when(loreRepository.findContentAndTitleByIdsAndRoomId(
                    eq(roomId), any(long[].class), eq(true), eq(false), any(long[].class)))
                    .thenReturn(List.of(contentRow));
            when(contentRow.getId()).thenReturn(loreId);
            when(contentRow.getContent()).thenReturn(content);
            when(contentRow.getTitle()).thenReturn("Title");
            when(questionRepository.countUnansweredByLoreId(loreId)).thenReturn(0L);
            when(loreMapper.toUserInlineResponse(
                    any(),           // LoreSummaryRow (мок)
                    any(),           // parsedContent: может быть null, anyString() не подойдёт!
                    anyBoolean(),    // isAuthor
                    anyBoolean(),    // isNonPublic
                    anyList(),       // tags
                    anyList(),       // excerpts
                    eq(0L)           // questionCount: примитив, оборачиваем в eq()
            )).thenReturn(mock(LoreUserInlineResponse.class));

            verify(loreAnchorExtractor).removeInvalidLinks(eq(content), anySet());
        }
    }
}