package com.cozyfireplace.server.lore.search;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountChar;
import com.cozyfireplace.server.lore.LoreService;
import com.cozyfireplace.server.lore.blocks.Block;
import com.cozyfireplace.server.lore.blocks.BlockRepository;
import com.cozyfireplace.server.lore.search.LoreDocumentAssembler.AssembledDocument;
import com.cozyfireplace.server.lore.search.LoreDocumentAssembler.PublicText;
import com.cozyfireplace.server.lore.search.LoreDocumentAssembler.Segment;
import com.cozyfireplace.server.lore.search.LoreSnippetBuilder.Chunk;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LoreSearchService} - orchestration only: the native
 * repository, the document assembler and the snippet builder are mocked, so
 * these tests pin down the service's branching (empty query, empty page,
 * pagination cursor, match truncation, viewer character resolution).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoreSearchService")
class LoreSearchServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long ACCOUNT_ID = 7L;

    @Mock
    private LoreSearchRepository repository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private LoreService loreService;
    @Mock
    private RoomSecurityService roomSecurityService;
    @Mock
    private LoreDocumentAssembler assembler;
    @Mock
    private LoreSnippetBuilder snippetBuilder;
    @Spy
    private LoreSearchProperties properties = new LoreSearchProperties();

    @InjectMocks
    private LoreSearchService service;

    // ================= helpers =================

    /** Matcher for long[] arguments compared order-insensitively (source is a HashSet stream). */
    private static long[] arrEqSorted(long[] expected) {
        return ArgumentMatchers.argThat(actual -> {
            if (actual == null) return false;
            long[] a = actual.clone();
            long[] e = expected.clone();
            Arrays.sort(a);
            Arrays.sort(e);
            return Arrays.equals(a, e);
        });
    }

    private Account viewer(Long charId) {
        Account account = mock(Account.class);
        lenient().when(account.getId()).thenReturn(ACCOUNT_ID);
        if (charId != null) {
            AccountChar ch = mock(AccountChar.class);
            lenient().when(ch.getId()).thenReturn(charId);
            lenient().when(account.getAccountChar()).thenReturn(ch);
        }
        return account;
    }

    private LoreSearchRequest request(String query, int size, int offset) {
        return LoreSearchRequest.builder()
                .query(query).roomId(ROOM_ID).size(size).offset(offset)
                .build();
    }

    private LoreSearchProjection row(long id, String title, String content) {
        return new LoreSearchProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public Boolean getByAdmin() {
                return true;
            }

            @Override
            public Boolean getSecret() {
                return false;
            }

            @Override
            public String getContent() {
                return content;
            }
        };
    }

    private HighlightProjection highlight() {
        return new HighlightProjection() {
            @Override
            public Long getOrd() {
                return 0L;
            }

            @Override
            public String getHeadline() {
                return null;
            }
        };
    }

    private static Chunk chunk(String text) {
        Chunk c = new Chunk(0, text.length(), 1);
        c.text = text;
        return c;
    }

    /** Stubs the whole pipeline for a single-row page (lenient: not every test reaches every stage). */
    private void stubPipeline(String content, List<Segment> segments, String assembledText,
                              List<int[]> occurrences, List<Chunk> chunks) {
        lenient().when(assembler.split(eq(content), anyMap())).thenReturn(segments);
        lenient().when(assembler.assemble(anyList(), anyList()))
                .thenReturn(new AssembledDocument(assembledText, List.of()));
        lenient().when(snippetBuilder.findOccurrences(assembledText)).thenReturn(occurrences);
        lenient().when(snippetBuilder.buildChunks(eq(assembledText), eq(occurrences), anyList()))
                .thenReturn(chunks);
    }

    // ================= tests =================

    @Test
    @DisplayName("blank query short-circuits to an empty page without touching the database")
    void blankQuery() {
        Account account = viewer(500L);
        when(roomSecurityService.isCreator(account)).thenReturn(false);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of());

        LoreSearchPageResponse response = service.search(request("   ", 10, 0), account);

        assertTrue(response.getContent().isEmpty());
        assertNull(response.getNextOffset());
        assertEquals(0L, response.getTotalElements());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("no matching rows returns an empty page but keeps the DB total")
    void noRows() {
        Account account = viewer(500L);
        when(roomSecurityService.isCreator(account)).thenReturn(true);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of(3L));
        when(repository.search(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(repository.countSearch(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any()))
                .thenReturn(4L);

        LoreSearchPageResponse response = service.search(request("compass", 10, 0), account);

        assertTrue(response.getContent().isEmpty());
        assertEquals(4L, response.getTotalElements());
        assertNull(response.getNextOffset());
    }

    @Test
    @DisplayName("a matching row is assembled, highlighted and turned into a response DTO")
    void happyPath() {
        Account account = viewer(500L);
        when(roomSecurityService.isCreator(account)).thenReturn(false);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of(3L));
        when(repository.search(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(row(10L, "Title", "hello world")));
        when(repository.countSearch(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any()))
                .thenReturn(1L);
        when(repository.findVisibleBlockIds(any(long[].class), any(), anyBoolean(), anyBoolean(), any(long[].class)))
                .thenReturn(List.of());
        stubPipeline("hello world", List.of(new PublicText("hello world")),
                "==hello== world", List.of(new int[]{0, 9}), List.of(chunk("==hello== world")));

        LoreSearchPageResponse response = service.search(request("hello", 10, 0), account);

        assertEquals(1, response.getContent().size());
        LoreSearchResponse dto = response.getContent().getFirst();
        assertEquals(10L, dto.getId());
        assertEquals("Title", dto.getTitle());
        assertTrue(dto.isByAdmin());
        assertFalse(dto.isSecret());
        assertEquals(1, dto.getTotalOccurrences());
        assertFalse(dto.isNotAll());
        assertEquals(1, dto.getMatches().size());
        assertEquals("==hello== world", dto.getMatches().getFirst().getMatchContent());
        assertEquals(1, dto.getMatches().getFirst().getOccurrenceCount());
        assertNull(response.getNextOffset(), "no more pages -> cursor must be null");
    }

    @Test
    @DisplayName("viewer char id and role array are forwarded to the native query")
    void viewerContextPropagation() {
        Account account = viewer(500L);
        when(roomSecurityService.isCreator(account)).thenReturn(false);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of(3L, 4L));
        when(repository.search(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(repository.countSearch(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any()))
                .thenReturn(0L);

        service.search(request("q", 20, 40), account);

        verify(repository).search(eq("q"), eq(ROOM_ID), eq(ACCOUNT_ID), eq(500L),
                eq(false), eq(true), arrEqSorted(new long[]{3L, 4L}), isNull(), isNull(), isNull(),
                eq(20), eq(40));
    }

    @Test
    @DisplayName("account without a character is queried with a null char id (no chance block visible)")
    void noCharacter() {
        Account account = viewer(null);
        when(roomSecurityService.isCreator(account)).thenReturn(false);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of());
        when(repository.search(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(repository.countSearch(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any()))
                .thenReturn(0L);

        service.search(request("q", 10, 0), account);

        verify(repository).search(eq("q"), eq(ROOM_ID), eq(ACCOUNT_ID), isNull(),
                eq(false), eq(false), arrEqSorted(new long[0]), isNull(), isNull(), isNull(),
                eq(10), eq(0));
    }

    @Test
    @DisplayName("visible block ids are loaded and passed to the assembler")
    void visibleBlocksLoaded() {
        Account account = viewer(500L);
        Block block = Block.builder().id(9L).content("body").roles(Set.of()).build();
        when(roomSecurityService.isCreator(account)).thenReturn(true);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of());
        when(repository.search(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(row(1L, "T", "content")));
        when(repository.countSearch(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any()))
                .thenReturn(1L);
        when(repository.findVisibleBlockIds(any(long[].class), any(), anyBoolean(), anyBoolean(), any(long[].class)))
                .thenReturn(List.of(9L));
        when(blockRepository.findAllByIdIn(List.of(9L))).thenReturn(List.of(block));
        stubPipeline("content", List.of(new PublicText("content")), "content",
                List.of(), List.of());

        service.search(request("q", 10, 0), account);

        verify(assembler).split("content", Map.of(9L, block));
    }

    @Test
    @DisplayName("matches beyond maxMatches are truncated and flagged with notAll")
    void truncationToMaxMatches() {
        properties.setMaxMatches(1);
        Account account = viewer(500L);
        when(roomSecurityService.isCreator(account)).thenReturn(true);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of());
        when(repository.search(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(row(1L, "T", "text")));
        when(repository.countSearch(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any()))
                .thenReturn(1L);
        when(repository.findVisibleBlockIds(any(long[].class), any(), anyBoolean(), anyBoolean(), any(long[].class)))
                .thenReturn(List.of());
        stubPipeline("text", List.of(new PublicText("text")), "aa bb cc",
                List.of(new int[]{0, 2}, new int[]{3, 5}, new int[]{6, 8}),
                List.of(chunk("aa bb"), chunk("bb cc"), chunk("cc")));

        LoreSearchResponse dto = service.search(request("q", 10, 0), account)
                .getContent().getFirst();

        assertTrue(dto.isNotAll());
        assertEquals(1, dto.getMatches().size());
        assertEquals(3, dto.getTotalOccurrences());
    }

    @Test
    @DisplayName("SQL match with no highlight in the document yields zero matches (mismatch guard)")
    void noHighlightMismatch() {
        Account account = viewer(500L);
        when(roomSecurityService.isCreator(account)).thenReturn(true);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of());
        when(repository.search(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(row(1L, "T", "text")));
        when(repository.countSearch(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any()))
                .thenReturn(1L);
        when(repository.findVisibleBlockIds(any(long[].class), any(), anyBoolean(), anyBoolean(), any(long[].class)))
                .thenReturn(List.of());
        stubPipeline("text", List.of(new PublicText("text")), "plain text",
                List.of(), List.of());

        LoreSearchResponse dto = service.search(request("q", 10, 0), account)
                .getContent().getFirst();

        assertEquals(0, dto.getTotalOccurrences());
        assertFalse(dto.isNotAll());
        assertTrue(dto.getMatches().isEmpty());
    }

    @Test
    @DisplayName("segments whose ts_headline came back null fall back to the raw text")
    void nullHeadlineFallsBackToRawSegment() {
        Account account = viewer(500L);
        when(roomSecurityService.isCreator(account)).thenReturn(false);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of());
        when(repository.search(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(row(1L, "T", "abc")));
        when(repository.countSearch(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any()))
                .thenReturn(1L);
        when(repository.findVisibleBlockIds(any(long[].class), any(), anyBoolean(), anyBoolean(), any(long[].class)))
                .thenReturn(List.of());
        when(assembler.split(eq("abc"), anyMap()))
                .thenReturn(List.of(new PublicText("abc")));
        when(repository.highlightBatch(any(long[].class), any(String[].class), eq("q"), anyString()))
                .thenReturn(List.of(highlight()));
        when(assembler.assemble(anyList(), eq(List.of("abc"))))
                .thenReturn(new AssembledDocument("abc", List.of()));
        when(snippetBuilder.findOccurrences("abc")).thenReturn(List.of());

        service.search(request("q", 10, 0), account);

        verify(assembler).assemble(anyList(), eq(List.of("abc")));
    }

    @Test
    @DisplayName("nextOffset is returned while more pages remain")
    void nextOffsetWhenMorePages() {
        Account account = viewer(500L);
        when(roomSecurityService.isCreator(account)).thenReturn(true);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of());
        when(repository.search(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(row(1L, "T", "text")));
        when(repository.countSearch(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any()))
                .thenReturn(3L);
        when(repository.findVisibleBlockIds(any(long[].class), any(), anyBoolean(), anyBoolean(), any(long[].class)))
                .thenReturn(List.of());
        when(assembler.split(any(), anyMap())).thenReturn(List.of());
        when(assembler.assemble(anyList(), anyList()))
                .thenReturn(new AssembledDocument("doc", List.of()));
        when(snippetBuilder.findOccurrences("doc")).thenReturn(List.of());

        LoreSearchPageResponse response = service.search(request("q", 1, 0), account);

        assertEquals(1, response.getNextOffset());
        assertEquals(3L, response.getTotalElements());
    }

    @Test
    @DisplayName("optional filters are forwarded to the repository untouched")
    void optionalFiltersForwarded() {
        Account account = viewer(500L);
        when(roomSecurityService.isCreator(account)).thenReturn(false);
        when(loreService.getAccountRoleIds(account)).thenReturn(Set.of());
        when(repository.search(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(repository.countSearch(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(long[].class), any(), any(), any()))
                .thenReturn(0L);

        LoreSearchRequest req = LoreSearchRequest.builder()
                .query("q").roomId(ROOM_ID).size(10).offset(0)
                .title("Broken").createdByCreator(true).status("viewed")
                .build();
        service.search(req, account);

        verify(repository).search(eq("q"), eq(ROOM_ID), eq(ACCOUNT_ID), eq(500L),
                eq(false), eq(false), any(long[].class), eq("Broken"), eq(true), eq("viewed"),
                eq(10), eq(0));
    }
}
