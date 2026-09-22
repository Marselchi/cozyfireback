package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.dto.LoreListFilter;
import com.cozyfireplace.server.lore.dto.LoreListResponse;
import com.cozyfireplace.server.lore.dto.LoreRequest;
import com.cozyfireplace.server.lore.dto.LoreResponse;
import com.cozyfireplace.server.lore.dto.LoreStatus;
import com.cozyfireplace.server.lore.dto.LoreUserResponse;
import com.cozyfireplace.server.lore.search.LoreSearchPageResponse;
import com.cozyfireplace.server.lore.search.LoreSearchRequest;
import com.cozyfireplace.server.lore.search.LoreSearchService;
import com.cozyfireplace.server.loreViews.LoreViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LoreController}.
 * <p>
 * The controller is a thin HTTP facade, so each test pins down its contract:
 * status code, body passthrough and - most importantly - how path variables,
 * query parameters and the authenticated account are mapped onto service calls
 * (e.g. the {@code byDm} request param becoming
 * {@link LoreSearchRequest#getCreatedByCreator()}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoreController")
class LoreControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long LORE_ID = 23L;
    private static final Long ACCOUNT_ID = 7L;

    @Mock
    private LoreService loreService;
    @Mock
    private LoreSearchService loreSearchService;
    @Mock
    private LoreViewService loreViewService;

    @InjectMocks
    private LoreController controller;

    // ================= helpers =================

    private Account account() {
        Account account = mock(Account.class);
        lenient().when(account.getId()).thenReturn(ACCOUNT_ID);
        return account;
    }

    private static LoreRequest request() {
        return new LoreRequest("title", "desc", "date", "content", null, Set.of(3L), Set.of(1L));
    }

    // ================= create =================

    @Nested
    @DisplayName("POST /{roomId}")
    class Create {

        @Test
        @DisplayName("returns 201 with the id produced by the service")
        void returnsCreatedId() {
            Account account = account();
            LoreRequest request = request();
            when(loreService.createLore(ROOM_ID, account, request)).thenReturn(LORE_ID);

            ResponseEntity<Long> response = controller.create(ROOM_ID, request, account);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertEquals(LORE_ID, response.getBody());
            verify(loreService).createLore(ROOM_ID, account, request);
        }
    }

    // ================= update =================

    @Nested
    @DisplayName("PUT /{roomId}/{loreId}")
    class Update {

        @Test
        @DisplayName("returns 204, updates the lore and flags it as viewed-updated")
        void updatesAndNotifiesViews() {
            Account account = account();
            LoreRequest request = request();

            ResponseEntity<Void> response = controller.update(ROOM_ID, LORE_ID, request, account);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            verify(loreService).updateLore(LORE_ID, request, account, ROOM_ID);
            verify(loreViewService).loreUpdated(LORE_ID);
        }
    }

    // ================= delete =================

    @Nested
    @DisplayName("DELETE /{loreId}")
    class Delete {

        @Test
        @DisplayName("returns 204 and delegates the deletion by lore id")
        void deletes() {
            ResponseEntity<Void> response = controller.delete(LORE_ID);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            verify(loreService).deleteLore(LORE_ID);
        }
    }

    // ================= list =================

    @Nested
    @DisplayName("GET /{roomId}/all")
    class ListLore {

        @Test
        @DisplayName("returns 200 with the exact page provided by the service")
        @SuppressWarnings("unchecked")
        void passesPageThrough() {
            Account account = account();
            LoreListFilter filter = new LoreListFilter(true, Set.of("tag"), "ti", LoreStatus.viewed);
            Pageable pageable = PageRequest.of(0, 20);
            Page<LoreListResponse> page = mock(Page.class);
            when(loreService.getLoreList(ROOM_ID, account, filter, pageable)).thenReturn(page);

            ResponseEntity<Page<LoreListResponse>> response = controller.list(ROOM_ID, filter, pageable, account);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(page, response.getBody());
        }
    }

    // ================= getForEdit =================

    @Nested
    @DisplayName("GET /{roomId}/{loreId}/edit")
    class GetForEdit {

        @Test
        @DisplayName("returns 200 with the edit view from the service")
        void returnsEditView() {
            Account account = account();
            LoreResponse body = mock(LoreResponse.class);
            when(loreService.getLoreForEdit(ROOM_ID, LORE_ID, account)).thenReturn(body);

            ResponseEntity<LoreResponse> response = controller.getForEdit(ROOM_ID, LORE_ID, account);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(body, response.getBody());
        }
    }

    // ================= getForUser =================

    @Nested
    @DisplayName("GET /{roomId}/{loreId}/view")
    class GetForUser {

        @Test
        @DisplayName("returns 200 and records the view for the current account")
        void returnsViewAndRecordsIt() {
            Account account = account();
            LoreUserResponse body = mock(LoreUserResponse.class);
            when(loreService.getLoreForUser(ROOM_ID, LORE_ID, account)).thenReturn(body);

            ResponseEntity<LoreUserResponse> response = controller.getForUser(ROOM_ID, LORE_ID, account);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(body, response.getBody());
            verify(loreViewService).setViewed(ACCOUNT_ID, LORE_ID);
        }

        @Test
        @DisplayName("the view is NOT recorded when the service rejects the request")
        void noViewRecordedOnFailure() {
            Account account = account();
            when(loreService.getLoreForUser(ROOM_ID, LORE_ID, account))
                    .thenThrow(new RuntimeException("no access"));

            assertThrows(RuntimeException.class, () -> controller.getForUser(ROOM_ID, LORE_ID, account));
            verify(loreViewService, never()).setViewed(anyLong(), anyLong());
        }
    }

    // ================= search =================

    @Nested
    @DisplayName("GET /{roomId}/full-search")
    class Search {

        @Test
        @DisplayName("maps every query param onto the search request, byDm becoming createdByCreator")
        void buildsSearchRequest() {
            Account account = account();
            LoreSearchPageResponse pageResponse = mock(LoreSearchPageResponse.class);
            when(loreSearchService.search(any(LoreSearchRequest.class), eq(account))).thenReturn(pageResponse);

            LoreSearchPageResponse result = controller.search(
                    "compass", ROOM_ID, 20, 40, "Broken", true, List.of(1L, 4L), "viewed", account);

            assertSame(pageResponse, result);
            ArgumentCaptor<LoreSearchRequest> captor = ArgumentCaptor.forClass(LoreSearchRequest.class);
            verify(loreSearchService).search(captor.capture(), eq(account));
            LoreSearchRequest request = captor.getValue();
            assertEquals("compass", request.getQuery());
            assertEquals(ROOM_ID, request.getRoomId());
            assertEquals(20, request.getSize());
            assertEquals(40, request.getOffset());
            assertEquals("Broken", request.getTitle());
            assertEquals(Boolean.TRUE, request.getCreatedByCreator());
            assertEquals(List.of(1L, 4L), request.getTagIds());
            assertEquals("viewed", request.getStatus());
        }

        @Test
        @DisplayName("optional filters stay null when the client omits them")
        void optionalFiltersStayNull() {
            Account account = account();
            when(loreSearchService.search(any(LoreSearchRequest.class), eq(account)))
                    .thenReturn(mock(LoreSearchPageResponse.class));

            controller.search("q", ROOM_ID, 10, 0, null, null, null, null, account);

            ArgumentCaptor<LoreSearchRequest> captor = ArgumentCaptor.forClass(LoreSearchRequest.class);
            verify(loreSearchService).search(captor.capture(), eq(account));
            LoreSearchRequest request = captor.getValue();
            assertNull(request.getTitle());
            assertNull(request.getCreatedByCreator());
            assertNull(request.getTagIds());
            assertNull(request.getStatus());
        }
    }
}
