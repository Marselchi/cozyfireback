package com.cozyfireplace.server.lore.search;

import com.cozyfireplace.server.lore.search.LoreDocumentAssembler.BlockRegion;
import com.cozyfireplace.server.lore.search.LoreSnippetBuilder.Chunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LoreSnippetBuilder} - pure logic that locates
 * {@code ==highlighted==} spans inside an assembled document and cuts
 * context windows (chunks) around them, merging overlapping windows and
 * repairing dangling markdown.
 */
@DisplayName("LoreSnippetBuilder")
class LoreSnippetBuilderTest {

    private LoreSearchProperties properties;
    private LoreSnippetBuilder builder;

    @BeforeEach
    void setUp() {
        properties = new LoreSearchProperties();
        properties.setContextChars(5);
        properties.setMaxChunkSize(100);
        properties.setMaxMatches(10);
        builder = new LoreSnippetBuilder(properties);
    }

    @Nested
    @DisplayName("findOccurrences")
    class FindOccurrences {

        @Test
        @DisplayName("null or empty input yields no occurrences")
        void emptyInput() {
            assertTrue(builder.findOccurrences(null).isEmpty());
            assertTrue(builder.findOccurrences("").isEmpty());
        }

        @Test
        @DisplayName("a single highlighted span is located including its markers")
        void singleOccurrence() {
            String text = "abc ==def== ghi";
            List<int[]> ranges = builder.findOccurrences(text);

            assertEquals(1, ranges.size());
            assertEquals(4, ranges.getFirst()[0]);
            assertEquals(11, ranges.getFirst()[1]);
            assertEquals("==def==", text.substring(ranges.getFirst()[0], ranges.getFirst()[1]));
        }

        @Test
        @DisplayName("multiple spans are located left to right")
        void multipleOccurrences() {
            List<int[]> ranges = builder.findOccurrences("==a== b ==c==");
            assertEquals(2, ranges.size());
            assertEquals(0, ranges.get(0)[0]);
            assertEquals(8, ranges.get(1)[0]);
        }

        @Test
        @DisplayName("an unterminated opening marker is ignored")
        void unterminatedMarker() {
            assertTrue(builder.findOccurrences("text ==oops").isEmpty());
        }
    }

    @Nested
    @DisplayName("buildChunks")
    class BuildChunks {

        @Test
        @DisplayName("one occurrence produces one chunk carrying its highlight and context")
        void singleChunk() {
            String content = "0123456789"; // 10 chars
            // highlight spans [2,8)
            List<Chunk> chunks = builder.buildChunks(content, List.of(new int[]{2, 8}), List.of());

            assertEquals(1, chunks.size());
            Chunk chunk = chunks.getFirst();
            assertEquals(1, chunk.occurrenceCount);
            assertEquals(0, chunk.start);
            assertEquals(10, chunk.end);
            assertEquals(content, chunk.text); // full content, no trimming needed
        }

        @Test
        @DisplayName("window smaller than the document gets '...' ellipses on both trimmed sides")
        void trimmedSides() {
            String content = "x".repeat(50);
            // place a highlight in the middle
            String doc = content.substring(0, 20) + "==hit==" + content.substring(25);

            List<Chunk> chunks = builder.buildChunks(doc, List.of(new int[]{20, 27}), List.of());

            assertEquals(1, chunks.size());
            String text = chunks.getFirst().text;
            assertTrue(text.startsWith("..."), text);
            assertTrue(text.endsWith("..."), text);
            assertTrue(text.contains("==hit=="));
        }

        @Test
        @DisplayName("overlapping windows are merged into one chunk with summed occurrences")
        void overlappingWindowsMerge() {
            // contextChars=5 -> first window [10..20+5], second starts at 21 <= window end
            String doc = "0123456789==one===two==56789";
            List<int[]> occurrences = List.of(new int[]{10, 17}, new int[]{17, 24});

            List<Chunk> chunks = builder.buildChunks(doc, occurrences, List.of());

            assertEquals(1, chunks.size());
            assertEquals(2, chunks.getFirst().occurrenceCount);
        }

        @Test
        @DisplayName("distant occurrences stay in separate chunks")
        void distantOccurrencesSplit() {
            String doc = "==one==" + "y".repeat(100) + "==two==";
            List<int[]> occurrences = List.of(new int[]{0, 7}, new int[]{107, 114});

            List<Chunk> chunks = builder.buildChunks(doc, occurrences, List.of());

            assertEquals(2, chunks.size());
            assertEquals(1, chunks.get(0).occurrenceCount);
            assertEquals(1, chunks.get(1).occurrenceCount);
        }

        @Test
        @DisplayName("window exceeding maxChunkSize is clamped symmetrically around the match")
        void oversizedWindowClamped() {
            properties.setMaxChunkSize(15);
            String doc = "x".repeat(20) + "==mid==" + "x".repeat(20);

            List<Chunk> chunks = builder.buildChunks(doc, List.of(new int[]{20, 27}), List.of());

            assertEquals(1, chunks.size());
            assertTrue(chunks.getFirst().text.contains("==mid=="));
        }

        @Test
        @DisplayName("an unclosed inline bold marker inside a chunk is closed at the end")
        void danglingBoldIsClosed() {
            String doc = "start **bold tail";
            // occurrence [6,13) is not a highlight pair - place a fake one to exercise chunk text
            List<Chunk> chunks = builder.buildChunks(doc, List.of(new int[]{6, 12}), List.of());

            String text = chunks.getFirst().text;
            // the segment never leaves the scanner unbalanced: trailing ** closes it
            assertTrue(text.endsWith("**"), text);
        }

        @Test
        @DisplayName("a chunk starting with a dangling closing marker gets it prepended back")
        void danglingClosingMarkerPrepended() {
            String doc = "abcdefghij end**";
            // window reaches the trailing '**', which reads as an unbalanced closing marker
            List<Chunk> chunks = builder.buildChunks(doc, List.of(new int[]{6, 12}), List.of());

            String text = chunks.getFirst().text;
            assertTrue(text.startsWith("..."), text);
            assertTrue(text.endsWith("**"), text);
            assertTrue(text.contains("...**"), "the dangling closer must be prepended: " + text);
        }

        @Test
        @DisplayName("a chunk starting inside a block region gets the block header restored")
        void blockHeaderRepaired() {
            String header = "::: restrictedBlock {\"id\":\"5\",\"spoilerType\":\"normal\",\"roles\":[]}";
            String doc = header + "\nblock body text here";
            int bodyStart = header.length() + 1;
            BlockRegion region = new BlockRegion(5L, 0, bodyStart, doc.length(), header);
            // occurrence inside the block body, window would start inside the region
            int matchStart = bodyStart + 6;
            List<Chunk> chunks = builder.buildChunks(
                    doc, List.of(new int[]{matchStart, matchStart + 4}), List.of(region));

            assertEquals(1, chunks.size());
            String text = chunks.getFirst().text;
            assertTrue(text.startsWith("...\n" + header),
                    "repaired chunk must re-open with '...' then the block header, got: " + text);
            assertTrue(text.contains("\n:::"), "the opened block must be closed again: " + text);
        }
    }
}
