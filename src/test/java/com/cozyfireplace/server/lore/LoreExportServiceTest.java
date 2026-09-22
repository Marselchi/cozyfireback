package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.rooms.dto.LoreExportRequest;
import com.cozyfireplace.server.util.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LoreExportService}.
 * <p>
 * The service renders lore rows into an in-memory ZIP of markdown files. The
 * tests unzip the produced byte array and assert file names, rendered
 * front-matter lines and wiki-link conversion. Repositories are mocked; the
 * {@code appDomain} property is injected via reflection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoreExportService")
class LoreExportServiceTest {

    private static final Long ROOM_ID = 1L;

    @Mock
    private LoreRepository loreRepository;
    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private LoreExportService exportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exportService, "appDomain", "https://cozyfireplace.ru");
    }

    // ================= helpers =================

    private Account account(long id) {
        return Account.builder().id(id).build();
    }

    private Room room(Account creator) {
        return Room.builder().id(ROOM_ID).name("R").url("room-1").creator(creator).build();
    }

    private Lore lore(long id, String title, String content, Account author) {
        return Lore.builder()
                .id(id).title(title).content(content).account(author)
                .build();
    }

    private LoreRepository.LoreTagRow tagRow(long tagId, String tagName) {
        return new LoreRepository.LoreTagRow() {
            @Override
            public Long getLoreId() {
                return 1L;
            }

            @Override
            public Long getTagId() {
                return tagId;
            }

            @Override
            public String getTagName() {
                return tagName;
            }
        };
    }

    private static Map<String, String> unzip(byte[] zipBytes) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zis.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    private static LoreExportRequest filters(boolean saveTags, boolean dmOnly) {
        return new LoreExportRequest(saveTags, dmOnly, true);
    }

    // ================= error paths =================

    @Test
    @DisplayName("throws NotFoundException when the room does not exist")
    void roomNotFound() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> exportService.exportLore(ROOM_ID, filters(false, false), account(7)));
    }

    // ================= happy paths =================

    @Nested
    @DisplayName("ZIP content")
    class ZipContent {

        @Test
        @DisplayName("renders one entry per lore with sanitized file name and raw content")
        void singleLore() throws IOException {
            Account author = account(7);
            Lore l = lore(1L, "The Broken Compass", "It spins.", author);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(author)));
            when(loreRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(l));
            when(loreRepository.findTagsByLoreIds(List.of(1L))).thenReturn(List.of());

            Map<String, String> entries = unzip(
                    exportService.exportLore(ROOM_ID, filters(false, false), author));

            assertEquals(List.of("The Broken Compass.md"), List.copyOf(entries.keySet()));
            // an empty separator line is always emitted between front-matter and body
            assertEquals("\nIt spins.", entries.get("The Broken Compass.md"));
        }

        @Test
        @DisplayName("description, date and tag lines are rendered above the body")
        void frontMatterLines() throws IOException {
            Account author = account(7);
            Lore l = Lore.builder().id(1L).title("T").content("body")
                    .description("a relic").date("Year 12").account(author).build();
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(author)));
            when(loreRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(l));
            when(loreRepository.findTagsByLoreIds(List.of(1L)))
                    .thenReturn(List.of(tagRow(2L, "artifact"), tagRow(3L, "north")));

            Map<String, String> entries = unzip(
                    exportService.exportLore(ROOM_ID, filters(true, false), author));

            String text = entries.get("T.md");
            assertTrue(text.startsWith("#artifact #north \nОписание: a relic\nДата: Year 12\n\nbody"), text);
        }

        @Test
        @DisplayName("tag lines are skipped when saveTags=false")
        void tagsSkipped() throws IOException {
            Account author = account(7);
            Lore l = lore(1L, "T", "body", author);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(author)));
            when(loreRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(l));
            when(loreRepository.findTagsByLoreIds(List.of(1L))).thenReturn(List.of());

            Map<String, String> entries = unzip(
                    exportService.exportLore(ROOM_ID, filters(false, false), author));

            assertEquals("\nbody", entries.get("T.md"));
            verify(loreRepository).findTagsByLoreIds(List.of(1L));
        }

        @Test
        @DisplayName("illegal file-name characters are replaced with underscores")
        void fileNameSanitized() throws IOException {
            Account author = account(7);
            Lore l = lore(1L, "a/b:c*d?e", "x", author);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(author)));
            when(loreRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(l));
            when(loreRepository.findTagsByLoreIds(List.of(1L))).thenReturn(List.of());

            Map<String, String> entries = unzip(
                    exportService.exportLore(ROOM_ID, filters(false, false), author));

            assertTrue(entries.containsKey("a_b_c_d_e.md"), entries.keySet().toString());
        }

        @Test
        @DisplayName("duplicate titles get numbered suffixes")
        void duplicateTitles() throws IOException {
            Account author = account(7);
            Lore first = lore(1L, "Same", "one", author);
            Lore second = lore(2L, "Same", "two", author);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(author)));
            when(loreRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(first, second));
            when(loreRepository.findTagsByLoreIds(List.of(1L, 2L))).thenReturn(List.of());

            Map<String, String> entries = unzip(
                    exportService.exportLore(ROOM_ID, filters(false, false), author));

            assertTrue(entries.containsKey("Same.md"));
            assertTrue(entries.containsKey("Same (1).md"));
        }
    }

    // ================= dmOnly filter =================

    @Nested
    @DisplayName("dmOnly filter")
    class DmOnly {

        @Test
        @DisplayName("keeps only lores authored by the room creator")
        void onlyCreatorLores() throws IOException {
            Account creator = account(1L);
            Account player = account(2L);
            Lore mine = lore(1L, "Mine", "m", creator);
            Lore theirs = lore(2L, "Theirs", "t", player);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(creator)));
            when(loreRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(mine, theirs));
            when(loreRepository.findTagsByLoreIds(List.of(1L))).thenReturn(List.of());

            Map<String, String> entries = unzip(
                    exportService.exportLore(ROOM_ID, filters(false, true), player));

            assertEquals(List.of("Mine.md"), List.copyOf(entries.keySet()));
        }

        @Test
        @DisplayName("dmOnly=false exports everything without consulting the creator")
        void allLores() throws IOException {
            Account creator = account(1L);
            Account player = account(2L);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(creator)));
            when(loreRepository.findAllByRoomId(ROOM_ID))
                    .thenReturn(List.of(lore(1L, "A", "a", creator), lore(2L, "B", "b", player)));
            when(loreRepository.findTagsByLoreIds(List.of(1L, 2L))).thenReturn(List.of());

            Map<String, String> entries = unzip(
                    exportService.exportLore(ROOM_ID, filters(false, false), player));

            assertEquals(2, entries.size());
        }
    }

    // ================= link conversion =================

    @Nested
    @DisplayName("markdown-to-wiki link conversion")
    class LinkConversion {

        @Test
        @DisplayName("lore link with a header anchor becomes [[Title#header|display]]")
        void linkWithHeader() throws IOException {
            Account author = account(7);
            Lore source = lore(1L, "Source",
                    "go [here](https://cozyfireplace.ru/rooms/r1/lore/2#chapter-3)!", author);
            Lore target = lore(2L, "Target", "t", author);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(author)));
            when(loreRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(source, target));
            when(loreRepository.findTagsByLoreIds(List.of(1L, 2L))).thenReturn(List.of());

            Map<String, String> entries = unzip(
                    exportService.exportLore(ROOM_ID, filters(false, false), author));

            assertEquals("\ngo [[Target#chapter-3|here]]!", entries.get("Source.md"));
            assertEquals("\nt", entries.get("Target.md"));
        }

        @Test
        @DisplayName("lore link without a header becomes [[Title|display]]")
        void linkWithoutHeader() throws IOException {
            Account author = account(7);
            Lore source = lore(1L, "Source",
                    "see [thing](https://cozyfireplace.ru/rooms/r1/lore/2)", author);
            Lore target = lore(2L, "Target", "t", author);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(author)));
            when(loreRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(source, target));
            when(loreRepository.findTagsByLoreIds(List.of(1L, 2L))).thenReturn(List.of());

            Map<String, String> entries = unzip(
                    exportService.exportLore(ROOM_ID, filters(false, false), author));

            assertEquals("\nsee [[Target|thing]]", entries.get("Source.md"));
        }

        @Test
        @DisplayName("link to a lore outside the export degrades to its display text")
        void danglingLinkBecomesText() throws IOException {
            Account author = account(7);
            Lore source = lore(1L, "Source",
                    "lost [sight](https://cozyfireplace.ru/rooms/r1/lore/999)", author);
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(author)));
            when(loreRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(source));
            when(loreRepository.findTagsByLoreIds(List.of(1L))).thenReturn(List.of());

            Map<String, String> entries = unzip(
                    exportService.exportLore(ROOM_ID, filters(false, false), author));

            assertEquals("\nlost sight", entries.get("Source.md"));
        }
    }
}
