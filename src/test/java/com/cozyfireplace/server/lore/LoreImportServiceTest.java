package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.dto.LoreRequest;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.dto.LoreImportRequest;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LoreImportService}.
 * <p>
 * Files are supplied as {@link MockMultipartFile}s (single markdown and real
 * in-memory ZIP archives). The persistence side is mocked: {@code loreService}
 * captures the {@link LoreRequest}s the importer would create/update, so the
 * tests focus on charset handling, wiki-link rewriting and conflict strategy.
 * The {@code appDomain} property used to build links is injected by reflection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoreImportService")
class LoreImportServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final String ROOM_URL = "test-room";

    @Mock
    private LoreRepository loreRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private LoreService loreService;

    @InjectMocks
    private LoreImportService importService;

    private Account account;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(importService, "appDomain", "https://cozyfireplace.ru");
        account = mock(Account.class);
        lenient().when(account.getId()).thenReturn(7L);
    }

    // ================= helpers =================

    private Room room() {
        return Room.builder().id(ROOM_ID).url(ROOM_URL).build();
    }

    private static LoreImportRequest filters(boolean autolink, boolean roomReset, boolean replaceOnConflict) {
        return new LoreImportRequest("zip", autolink, roomReset, replaceOnConflict);
    }

    private static MockMultipartFile mdFile(String name, String content) {
        return new MockMultipartFile("file", name, "text/markdown",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile zipFile(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return new MockMultipartFile("file", "a.zip", "application/zip", bos.toByteArray());
    }

    private static Lore existingLore(long id, String title, String content) {
        return Lore.builder()
                .id(id).title(title).content(content)
                .description("d").date("dd")
                .roles(Set.of()).tags(Set.of())
                .build();
    }

    // ================= importMdFile =================

    @Nested
    @DisplayName("importMdFile")
    class ImportMdFile {

        @Test
        @DisplayName("throws NotFoundException when the room does not exist")
        void roomNotFound() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> importService.importMdFile(ROOM_ID, mdFile("a.md", "x"), filters(false, false, false), account));
        }

        @Test
        @DisplayName("rejects files that are not markdown")
        void rejectsNonMarkdown() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            MockMultipartFile txt = mdFile("notes.txt", "x");

            assertThrows(IllegalArgumentException.class,
                    () -> importService.importMdFile(ROOM_ID, txt, filters(false, false, false), account));
        }

        @Test
        @DisplayName("creates a new lore named after the file with the raw content")
        void createsFreshLore() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findByTitleAndRoomId("My File", ROOM_ID)).thenReturn(Optional.empty());
            when(loreService.createLore(eq(ROOM_ID), eq(account), any(LoreRequest.class))).thenReturn(10L);

            importService.importMdFile(ROOM_ID, mdFile("My File.md", "# H\nbody"),
                    filters(false, false, false), account);

            ArgumentCaptor<LoreRequest> captor = ArgumentCaptor.forClass(LoreRequest.class);
            verify(loreService).createLore(eq(ROOM_ID), eq(account), captor.capture());
            LoreRequest req = captor.getValue();
            assertEquals("My File", req.title());
            assertTrue(req.content().startsWith("# H\nbody\n"), req.content());
            assertEquals(Set.of(), req.roleIds());
        }

        @Test
        @DisplayName("an existing lore with the same title is NOT overwritten by single-file import")
        void conflictKept() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findByTitleAndRoomId("Dup", ROOM_ID))
                    .thenReturn(Optional.of(existingLore(5L, "Dup", "old")));

            importService.importMdFile(ROOM_ID, mdFile("Dup.md", "new"),
                    filters(false, false, false), account);

            verify(loreService, never()).createLore(anyLong(), any(), any());
            verify(loreService, never()).updateLore(anyLong(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("autolink=false: wiki links degrade to their display text")
        void wikiLinksDegradeWithoutAutolink() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findByTitleAndRoomId("Source", ROOM_ID)).thenReturn(Optional.empty());
            when(loreService.createLore(eq(ROOM_ID), eq(account), any(LoreRequest.class))).thenReturn(1L);

            importService.importMdFile(ROOM_ID, mdFile("Source.md", "see [[Target|the target]]!"),
                    filters(false, false, false), account);

            ArgumentCaptor<LoreRequest> captor = ArgumentCaptor.forClass(LoreRequest.class);
            verify(loreService).createLore(eq(ROOM_ID), eq(account), captor.capture());
            assertTrue(captor.getValue().content().contains("see the target!"),
                    captor.getValue().content());
        }

        @Test
        @DisplayName("autolink=true: wiki link to an existing lore becomes a full frontend URL")
        void wikiLinksResolvedWithAutolink() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findByTitleAndRoomId("Source", ROOM_ID)).thenReturn(Optional.empty());
            when(loreRepository.findByTitleAndRoomId("Target", ROOM_ID))
                    .thenReturn(Optional.of(existingLore(55L, "Target", "t")));
            when(loreService.createLore(eq(ROOM_ID), eq(account), any(LoreRequest.class))).thenReturn(1L);

            importService.importMdFile(ROOM_ID, mdFile("Source.md", "[[Target|go there]]"),
                    filters(true, false, false), account);

            ArgumentCaptor<LoreRequest> captor = ArgumentCaptor.forClass(LoreRequest.class);
            verify(loreService).createLore(eq(ROOM_ID), eq(account), captor.capture());
            assertTrue(captor.getValue().content().contains(
                    "[go there](https://cozyfireplace.ru/rooms/" + ROOM_URL + "/lore/55)"),
                    captor.getValue().content());
        }

        @Test
        @DisplayName("autolink=true with a header anchor appends the URL-encoded header")
        void wikiLinkWithHeader() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findByTitleAndRoomId("Source", ROOM_ID)).thenReturn(Optional.empty());
            when(loreRepository.findByTitleAndRoomId("Target", ROOM_ID))
                    .thenReturn(Optional.of(existingLore(55L, "Target", "t")));
            when(loreService.createLore(eq(ROOM_ID), eq(account), any(LoreRequest.class))).thenReturn(1L);

            importService.importMdFile(ROOM_ID, mdFile("Source.md", "[[Target#Some Header|jump]]"),
                    filters(true, false, false), account);

            ArgumentCaptor<LoreRequest> captor = ArgumentCaptor.forClass(LoreRequest.class);
            verify(loreService).createLore(eq(ROOM_ID), eq(account), captor.capture());
            assertTrue(captor.getValue().content().contains(
                    "https://cozyfireplace.ru/rooms/" + ROOM_URL + "/lore/55#Some+Header"),
                    captor.getValue().content());
        }
    }

    // ================= importZipFile =================

    @Nested
    @DisplayName("importZipFile")
    class ImportZipFile {

        @Test
        @DisplayName("throws NotFoundException when the room does not exist")
        void roomNotFound() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> importService.importZipFile(ROOM_ID, zipFile(Map.of("x.md", "y")),
                            filters(false, false, false), account));
        }

        @Test
        @DisplayName("an archive without markdown entries is rejected")
        void noMarkdownEntries() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));

            assertThrows(IllegalArgumentException.class,
                    () -> importService.importZipFile(ROOM_ID, zipFile(Map.of("readme.txt", "hi")),
                            filters(false, false, false), account));
        }

        @Test
        @DisplayName("roomReset=true wipes all existing lore first")
        void roomResetWipesLore() throws Exception {
            List<Lore> existing = List.of(existingLore(1L, "Old", "o"));
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findAllByRoomId(ROOM_ID)).thenReturn(existing);
            when(loreRepository.findByTitleAndRoomId("A", ROOM_ID)).thenReturn(Optional.empty());
            when(loreService.createLore(eq(ROOM_ID), eq(account), any(LoreRequest.class))).thenReturn(1L);

            importService.importZipFile(ROOM_ID, zipFile(Map.of("A.md", "aaa")),
                    filters(false, true, false), account);

            verify(loreRepository).deleteAll(existing);
        }

        @Test
        @DisplayName("creates one lore per markdown entry, titles taken from file names")
        void importsAllEntries() throws Exception {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findByTitleAndRoomId(anyString(), eq(ROOM_ID))).thenReturn(Optional.empty());
            when(loreService.createLore(eq(ROOM_ID), eq(account), any(LoreRequest.class))).thenReturn(1L, 2L);

            importService.importZipFile(ROOM_ID,
                    zipFile(Map.of("Alpha.md", "first", "nested/Beta.markdown", "second")),
                    filters(false, false, false), account);

            ArgumentCaptor<LoreRequest> captor = ArgumentCaptor.forClass(LoreRequest.class);
            verify(loreService, times(2)).createLore(eq(ROOM_ID), eq(account), captor.capture());
            List<String> titles = captor.getAllValues().stream().map(LoreRequest::title).sorted().toList();
            assertEquals(List.of("Alpha", "Beta"), titles);
        }

        @Test
        @DisplayName("wiki link to a file inside the same archive resolves once that file was imported")
        void crossLinksInsideArchive() throws Exception {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findByTitleAndRoomId(anyString(), eq(ROOM_ID))).thenReturn(Optional.empty());
            when(loreService.createLore(eq(ROOM_ID), eq(account), any(LoreRequest.class)))
                    .thenAnswer(inv -> ((LoreRequest) inv.getArgument(2)).title().equals("Alpha") ? 11L : 22L);

            importService.importZipFile(ROOM_ID,
                    zipFile(Map.of("Alpha.md", "A text [[Beta|see beta]]",
                            "Beta.md", "B text [[Alpha|see alpha]]")),
                    filters(true, false, false), account);

            ArgumentCaptor<LoreRequest> captor = ArgumentCaptor.forClass(LoreRequest.class);
            verify(loreService, times(2)).createLore(eq(ROOM_ID), eq(account), captor.capture());

            LoreRequest alpha = captor.getAllValues().stream()
                    .filter(r -> r.title().equals("Alpha")).findFirst().orElseThrow();
            LoreRequest beta = captor.getAllValues().stream()
                    .filter(r -> r.title().equals("Beta")).findFirst().orElseThrow();
            // exactly one of the two was processed first and kept the plain display text;
            // the second must carry a link to the first one's reserved id
            boolean alphaLinked = alpha.content().contains("/lore/22");
            boolean betaLinked = beta.content().contains("/lore/11");
            assertTrue(alphaLinked || betaLinked,
                    "the later file must link to the earlier one; alpha=" + alpha.content()
                            + " beta=" + beta.content());
        }

        @Test
        @DisplayName("replaceOnConflict=true updates an existing lore keeping its roles/tags ids")
        void replaceOnConflict() throws Exception {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findByTitleAndRoomId("Alpha", ROOM_ID))
                    .thenReturn(Optional.of(existingLore(5L, "Alpha", "old content")));

            importService.importZipFile(ROOM_ID, zipFile(Map.of("Alpha.md", "fresh")),
                    filters(false, false, true), account);

            ArgumentCaptor<LoreRequest> captor = ArgumentCaptor.forClass(LoreRequest.class);
            verify(loreService).updateLore(eq(5L), captor.capture(), eq(account), eq(ROOM_ID));
            verify(loreService, never()).createLore(anyLong(), any(), any());
            LoreRequest req = captor.getValue();
            assertEquals("Alpha", req.title());
            assertEquals("d", req.description());
            assertEquals("dd", req.date());
            assertEquals(Set.of(), req.roleIds());
            assertEquals(Set.of(), req.tagIds());
            assertTrue(req.content().startsWith("fresh"), req.content());
        }

        @Test
        @DisplayName("replaceOnConflict=false leaves the existing lore untouched")
        void keepOnConflict() throws Exception {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findByTitleAndRoomId("Alpha", ROOM_ID))
                    .thenReturn(Optional.of(existingLore(5L, "Alpha", "old")));

            importService.importZipFile(ROOM_ID, zipFile(Map.of("Alpha.md", "fresh")),
                    filters(false, false, false), account);

            verify(loreService, never()).createLore(anyLong(), any(), any());
            verify(loreService, never()).updateLore(anyLong(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("a .markdown entry inside the archive is imported as well")
        void markdownExtension() throws Exception {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(loreRepository.findByTitleAndRoomId("Doc", ROOM_ID)).thenReturn(Optional.empty());
            when(loreService.createLore(eq(ROOM_ID), eq(account), any(LoreRequest.class))).thenReturn(9L);

            importService.importZipFile(ROOM_ID, zipFile(Map.of("Doc.markdown", "body")),
                    filters(false, false, false), account);

            verify(loreService).createLore(eq(ROOM_ID), eq(account), any(LoreRequest.class));
        }
    }
}
