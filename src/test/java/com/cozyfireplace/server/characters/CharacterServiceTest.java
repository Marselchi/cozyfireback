package com.cozyfireplace.server.characters;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.characters.dto.*;
import com.cozyfireplace.server.lore.LoreAnchorExtractor;
import com.cozyfireplace.server.lore.LoreRepository;
import com.cozyfireplace.server.lore.dto.ContentTitleRow;
import com.cozyfireplace.server.notifications.event.CharacterEvent;
import com.cozyfireplace.server.notifications.event.OperationType;
import com.cozyfireplace.server.questions.QuestionRepository;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleService;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import com.cozyfireplace.server.rooms.dto.RoomTextResponse;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterService tests")
class CharacterServiceTest {

    @Mock private CharacterRepository characterRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoleService roleService;
    @Mock private RoomSecurityService roomSecurityService;
    @Mock private CharacterMapper characterMapper;
    @Mock private LoreAnchorExtractor loreAnchorExtractor;
    @Mock private LoreRepository loreRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CharacterService characterService;

    // ====== HELPERS (lenient stubs to avoid UnnecessaryStubbing) ======
    private Account testAccount(long id, Set<Role> roles) {
        Account acc = mock(Account.class);
        lenient().when(acc.getId()).thenReturn(id);
        lenient().when(acc.getRoles()).thenReturn(roles);
        return acc;
    }

    private RoomTextResponse testRoom() {
        RoomTextResponse room = mock(RoomTextResponse.class);
        lenient().when(room.getRoomName()).thenReturn("Room");
        lenient().when(room.getRoomUrl()).thenReturn("/r/1");
        return room;
    }

    // ====== CREATE CHARACTER ======
    @Nested
    @DisplayName("createCharacter")
    class CreateCharacterTests {

        @Test
        @DisplayName("should create character and publish event")
        void createCharacter_success() {
            long roomId = 1L;
            long charId = 200L, accountId = 1L;
            Account account = testAccount(accountId, Set.of());
            RoomTextResponse roomResp = testRoom();
            Character character = mock(Character.class);
            CharacterRequest request = mock(CharacterRequest.class);

            when(roomRepository.findRoomUrlById(roomId)).thenReturn(Optional.of(roomResp));
            when(characterMapper.toEntity(request, account)).thenReturn(character);
            when(roleService.resolveRoles(anyList())).thenReturn(Set.of());
            when(characterRepository.save(any(Character.class))).thenAnswer(inv -> inv.getArgument(0));
            when(character.getId()).thenReturn(charId);
            when(request.roleIds()).thenReturn(null);
            when(request.name()).thenReturn("Hero");
            when(roomSecurityService.isCreator(account)).thenReturn(true);

            Long result = characterService.createCharacter(roomId, account, request);

            assertEquals(charId, result);
            ArgumentCaptor<CharacterEvent> captor = ArgumentCaptor.forClass(CharacterEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            CharacterEvent ev = captor.getValue();
            assertEquals(OperationType.CREATE, ev.type());
            assertTrue(ev.byDm());
        }

        @Test
        @DisplayName("should throw when room not found")
        void createCharacter_roomNotFound() {
            when(roomRepository.findRoomUrlById(1L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> characterService.createCharacter(1L, testAccount(1L, Set.of()), mock(CharacterRequest.class)));
        }

        @Test
        @DisplayName("should handle empty roleIds")
        void createCharacter_emptyRoles() {
            Long roomId = 1L, charId = 200L;
            Account account = testAccount(1L, Set.of());
            RoomTextResponse roomResp = testRoom();
            Character character = mock(Character.class);
            CharacterRequest request = mock(CharacterRequest.class);

            when(roomRepository.findRoomUrlById(roomId)).thenReturn(Optional.of(roomResp));
            when(characterMapper.toEntity(request, account)).thenReturn(character);
            when(roleService.resolveRoles(eq(List.of()))).thenReturn(Set.of());
            when(characterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(character.getId()).thenReturn(charId);
            when(request.roleIds()).thenReturn(Set.of());
            when(request.name()).thenReturn("Name");
            when(roomSecurityService.isCreator(account)).thenReturn(false);

            Long result = characterService.createCharacter(roomId, account, request);
            assertEquals(charId, result);
            verify(roleService).resolveRoles(eq(List.of()));
        }
    }

    // ====== UPDATE CHARACTER ======
    @Nested
    @DisplayName("updateCharacter")
    class UpdateCharacterTests {

        @Test
        @DisplayName("should update character and publish event")
        void updateCharacter_success() {
            long charId = 200L;
            long roomId = 1L, accountId = 1L;
            Account account = testAccount(accountId, Set.of());
            RoomTextResponse roomResp = testRoom();
            Character character = mock(Character.class);
            CharacterRequest request = mock(CharacterRequest.class);

            when(characterRepository.findById(charId)).thenReturn(Optional.of(character));
            when(roomRepository.findRoomUrlById(roomId)).thenReturn(Optional.of(roomResp));
            when(roleService.updateRolesCharacter(eq(character), any())).thenReturn(character);
            when(request.name()).thenReturn("Updated");
            when(request.roleIds()).thenReturn(Set.of(5L));

            characterService.updateCharacter(charId, request, account, roomId);

            verify(characterRepository).save(character);
        }

        @Test
        @DisplayName("should throw when character not found")
        void updateCharacter_notFound() {
            when(characterRepository.findById(200L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> characterService.updateCharacter(200L, mock(CharacterRequest.class),
                            testAccount(1L, Set.of()), 1L));
        }
    }

    // ====== DELETE CHARACTER ======
    @Nested
    @DisplayName("deleteCharacter")
    class DeleteCharacterTests {
        @Test
        void deleteCharacter_success() {
            characterService.deleteCharacter(200L);
            verify(characterRepository).deleteById(200L);
        }
    }

    // ====== GET CHARACTER LIST ======
    @Nested
    @DisplayName("getCharacterList")
    class GetCharacterListTests {

        @Test
        @DisplayName("should return empty page")
        void getCharacterList_empty() {
            Pageable pageable = PageRequest.of(0, 10);
            Account account = testAccount(1L, Set.of());
            Page<Long> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(characterRepository.findCharacterIdsFiltered(
                    eq(1L), anyBoolean(), anyBoolean(), any(long[].class), any(), any(),
                    anyInt(), any(long[].class), any()))
                    .thenReturn(emptyPage);

            Page<CharacterListResponse> result = characterService.getCharacterList(1L, account, null, pageable);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return populated page with filters")
        void getCharacterList_withData() {
            Pageable pageable = PageRequest.of(0, 10);
            long charId = 200L;
            long accountId = 1L;
            Account account = testAccount(accountId, Set.of());
            Page<Long> idPage = new PageImpl<>(List.of(charId), pageable, 1);
            CharacterSummaryRow summary = mock(CharacterSummaryRow.class);
            CharacterListFilter filter = mock(CharacterListFilter.class);

            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(characterRepository.findCharacterIdsFiltered(
                    eq(1L), anyBoolean(), anyBoolean(), any(long[].class), any(), any(),
                    anyInt(), any(long[].class), any()))
                    .thenReturn(idPage);
            when(summary.getId()).thenReturn(charId);
            when(characterRepository.findSummariesByIds(List.of(charId)))
                    .thenReturn(List.of(summary));
            when(characterMapper.toListResponse(eq(summary)))
                    .thenReturn(mock(CharacterListResponse.class));
            when(filter.roleIds()).thenReturn(null);
            when(filter.name()).thenReturn(null);
            when(filter.createdByRoomCreator()).thenReturn(null);

            Page<CharacterListResponse> result = characterService.getCharacterList(1L, account, filter, pageable);
            assertEquals(1, result.getContent().size());
        }
    }

    // ====== GET CHARACTER FOR EDIT ======
    @Nested
    @DisplayName("getCharacterForEdit")
    class GetCharacterForEditTests {

        @Test
        @DisplayName("should return character for edit")
        void getCharacterForEdit_success() {
            Long roomId = 1L, charId = 200L;
            Account account = testAccount(1L, Set.of());
            CharacterSummaryRow summary = mock(CharacterSummaryRow.class);

            when(roomSecurityService.isCreator(account)).thenReturn(true);
            // Stub access check inside assertCanReadCharacter
            when(characterRepository.existsVisibleCharacter(
                    eq(roomId), eq(charId), eq(true), eq(false), any(long[].class)))
                    .thenReturn(true);
            when(characterRepository.findSummaryByIdAndRoomId(roomId, charId))
                    .thenReturn(Optional.of(summary));
            when(summary.getId()).thenReturn(charId);
            when(characterRepository.findRolesByCharacterIds(List.of(charId))).thenReturn(List.of());
            when(characterMapper.toEditResponse(eq(summary), eq(List.of())))
                    .thenReturn(mock(CharacterResponse.class));

            CharacterResponse result = characterService.getCharacterForEdit(roomId, charId, account);
            assertNotNull(result);
        }

        @Test
        @DisplayName("should throw when not accessible")
        void getCharacterForEdit_forbidden() {
            Account account = testAccount(1L, Set.of());
            when(roomSecurityService.isCreator(account)).thenReturn(false);
            when(characterRepository.existsVisibleCharacter(
                    eq(1L), eq(200L), eq(false), eq(false), any(long[].class)))
                    .thenReturn(false);

            assertThrows(NotFoundException.class,
                    () -> characterService.getCharacterForEdit(1L, 200L, account));
        }
    }

    // ====== GET CHARACTER FOR USER INLINE ======
    @Nested
    @DisplayName("getCharacterForUserInline")
    class GetCharacterForUserInlineTests {

        @Test
        @DisplayName("should return inline response with excerpts")
        void getCharacterForUserInline_success() {
            long roomId = 1L;
            long charId = 200L, accountId = 1L;
            Account account = testAccount(accountId, Set.of());
            CharacterSummaryRow summary = mock(CharacterSummaryRow.class);
            LoreAnchorExtractor.AnchorLink anchor = mock(LoreAnchorExtractor.AnchorLink.class);
            ContentTitleRow loreRow = mock(ContentTitleRow.class);
            String content = "# Header\nText";

            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(characterRepository.findSummaryByIdAndRoomId(roomId, charId))
                    .thenReturn(Optional.of(summary));
            when(summary.getId()).thenReturn(charId);
            when(summary.getAccountId()).thenReturn(accountId);
            when(summary.getContent()).thenReturn(content);
            when(characterRepository.findRolesByCharacterIds(List.of(charId))).thenReturn(List.of());
            // Use ArrayList for mutable list (removeIf is called)
            when(loreAnchorExtractor.extractAnchorLinks(content))
                    .thenReturn(new ArrayList<>(List.of(anchor)));
            when(anchor.loreId()).thenReturn(300L);
            when(anchor.header()).thenReturn("header");
            when(loreRepository.findContentAndTitleByIdsAndRoomId(
                    eq(roomId), any(long[].class), eq(true), eq(false), any(long[].class)))
                    .thenReturn(List.of(loreRow));
            when(loreRow.getId()).thenReturn(300L);
            when(loreRow.getContent()).thenReturn(content);
            when(loreRow.getTitle()).thenReturn("Lore Title");
            when(questionRepository.countUnansweredByCharacterId(charId)).thenReturn(2L);
            // Use any() for nullable parsedContent and complex excerpts list
            when(characterMapper.toUserResponse(
                    any(), any(), anyBoolean(), anyList(), anyList(), eq(2L)))
                    .thenReturn(mock(CharacterUserResponse.class));

            CharacterUserResponse result = characterService.getCharacterForUserInline(roomId, charId, account);
            assertNotNull(result);
            verify(loreAnchorExtractor).removeInvalidLinks(eq(content), anySet());
        }

        @Test
        @DisplayName("should handle empty anchor links")
        void getCharacterForUserInline_noAnchors() {
            long roomId = 1L;
            long charId = 200L, accountId = 1L;
            Account account = testAccount(accountId, Set.of());
            CharacterSummaryRow summary = mock(CharacterSummaryRow.class);
            String content = "Plain text";

            when(roomSecurityService.isCreator(account)).thenReturn(true);
            when(characterRepository.findSummaryByIdAndRoomId(roomId, charId))
                    .thenReturn(Optional.of(summary));
            when(summary.getId()).thenReturn(charId);
            when(summary.getAccountId()).thenReturn(accountId);
            when(summary.getContent()).thenReturn(content);
            when(characterRepository.findRolesByCharacterIds(List.of(charId))).thenReturn(new ArrayList<>());
            when(loreAnchorExtractor.extractAnchorLinks(content)).thenReturn(new ArrayList<>());
            when(questionRepository.countUnansweredByCharacterId(charId)).thenReturn(0L);
            when(characterMapper.toUserResponse(any(), any(), anyBoolean(), anyList(), anyList(), eq(0L)))
                    .thenReturn(mock(CharacterUserResponse.class));

            CharacterUserResponse result = characterService.getCharacterForUserInline(roomId, charId, account);
            assertNotNull(result);
        }
    }

    // ====== EDGE CASES ======
    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle null roleIds in createCharacter")
        void createCharacter_nullRoles() {
            Long roomId = 1L, charId = 200L;
            Account account = testAccount(1L, Set.of());
            RoomTextResponse roomResp = testRoom();
            Character character = mock(Character.class);
            CharacterRequest request = mock(CharacterRequest.class);

            when(roomRepository.findRoomUrlById(roomId)).thenReturn(Optional.of(roomResp));
            when(characterMapper.toEntity(request, account)).thenReturn(character);
            when(roleService.resolveRoles(eq(List.of()))).thenReturn(Set.of());
            when(characterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(character.getId()).thenReturn(charId);
            when(request.roleIds()).thenReturn(null);
            when(request.name()).thenReturn("Name");
            when(roomSecurityService.isCreator(account)).thenReturn(false);

            Long result = characterService.createCharacter(roomId, account, request);
            assertEquals(charId, result);
            verify(roleService).resolveRoles(eq(List.of()));
        }
    }
}