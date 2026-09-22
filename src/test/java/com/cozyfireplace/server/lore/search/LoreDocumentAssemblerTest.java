package com.cozyfireplace.server.lore.search;

import com.cozyfireplace.server.lore.blocks.Block;
import com.cozyfireplace.server.lore.search.LoreDocumentAssembler.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LoreDocumentAssembler} - the pure component that cuts
 * raw lore content into segments (public text / visible block body /
 * placeholder for invisible blocks) and stitches the highlighted segments back
 * into a single searchable document with recorded block regions.
 */
@DisplayName("LoreDocumentAssembler")
class LoreDocumentAssemblerTest {

    private LoreDocumentAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new LoreDocumentAssembler(new ObjectMapper());
    }

    private static Block visibleBlock(long id, String content) {
        return Block.builder().id(id).content(content).roles(Set.of()).build();
    }

    @Nested
    @DisplayName("split")
    class Split {

        @Test
        @DisplayName("null or empty content produces no segments")
        void emptyContent() {
            assertEquals(List.of(), assembler.split(null, Map.of()));
            assertEquals(List.of(), assembler.split("", Map.of()));
        }

        @Test
        @DisplayName("content without markers is one public segment")
        void plainContent() {
            List<Segment> segments = assembler.split("just text", Map.of());
            assertEquals(List.of(new PublicText("just text")), segments);
        }

        @Test
        @DisplayName("text around a marker stays public, invisible marker becomes a non-highlightable placeholder")
        void invisibleMarkerBecomesPlaceholder() {
            String content = "before {restrictedBlock id=\"42\"} after";
            List<Segment> segments = assembler.split(content, Map.of());

            assertEquals(3, segments.size());
            assertEquals(new PublicText("before "), segments.get(0));
            assertEquals(new Placeholder(LoreDocumentAssembler.BLOCK_PLACEHOLDER), segments.get(1));
            assertFalse(segments.get(1).highlightable());
            assertEquals(new PublicText(" after"), segments.get(2));
        }

        @Test
        @DisplayName("marker of a visible block is replaced by the block body")
        void visibleMarkerBecomesBlockText() {
            Block block = visibleBlock(42, "block body");
            String content = "a{restrictedBlock id=\"42\"}b";
            List<Segment> segments = assembler.split(content, Map.of(42L, block));

            assertEquals(3, segments.size());
            assertEquals(new BlockText(block, "block body"), segments.get(1));
            assertTrue(segments.get(1).highlightable());
        }

        @Test
        @DisplayName("block with null body yields an empty BlockText segment")
        void visibleBlockWithNullContent() {
            Block block = visibleBlock(42, null);
            List<Segment> segments = assembler.split(
                    "{restrictedBlock id=\"42\"}", Map.of(42L, block));

            assertEquals(new BlockText(block, ""), segments.getFirst());
        }

        @Test
        @DisplayName("marker without a numeric id attribute is a placeholder")
        void markerWithoutId() {
            List<Segment> segments = assembler.split(
                    "{restrictedBlock localId=\"tmp\"}", Map.of(1L, visibleBlock(1, "x")));

            assertEquals(List.of(new Placeholder(LoreDocumentAssembler.BLOCK_PLACEHOLDER)), segments);
        }

        @Test
        @DisplayName("multiple markers are processed in order with public gaps")
        void multipleMarkers() {
            Block one = visibleBlock(1, "A");
            Block two = visibleBlock(2, "B");
            String content = "s{restrictedBlock id=\"1\"}m{restrictedBlock id=\"2\"}e";
            List<Segment> segments = assembler.split(content, Map.of(1L, one, 2L, two));

            assertEquals(List.of(
                    new PublicText("s"),
                    new BlockText(one, "A"),
                    new PublicText("m"),
                    new BlockText(two, "B"),
                    new PublicText("e")
            ), segments);
        }
    }

    @Nested
    @DisplayName("assemble")
    class Assemble {

        @Test
        @DisplayName("public segments are concatenated verbatim")
        void publicOnly() {
            AssembledDocument doc = assembler.assemble(
                    List.of(new PublicText("ab"), new PublicText("cd")),
                    List.of("ab", "cd"));

            assertEquals("abcd", doc.text());
            assertTrue(doc.regions().isEmpty());
        }

        @Test
        @DisplayName("block segments are wrapped with header and closing marker; region offsets are recorded")
        void blockRegionOffsets() {
            Block block = visibleBlock(7, "ignored-body");
            List<Segment> segments = List.of(new PublicText("pre\n"), new BlockText(block, "HL-body"));
            AssembledDocument doc = assembler.assemble(segments, List.of("pre\n", "HL-body"));

            String header = "::: restrictedBlock {\"id\":\"7\",\"spoilerType\":\"normal\",\"roles\":[]}";
            assertEquals("pre\n" + header + "\nHL-body\n:::", doc.text());

            assertEquals(1, doc.regions().size());
            BlockRegion region = doc.regions().getFirst();
            assertEquals(7L, region.blockId());
            assertEquals(4, region.start());
            assertEquals(4 + header.length() + 1, region.bodyStart());
            assertEquals(doc.text().length(), region.end());
            assertEquals(header, region.header());
        }

        @Test
        @DisplayName("placeholders are appended as-is without a region")
        void placeholderAppended() {
            AssembledDocument doc = assembler.assemble(
                    List.of(new Placeholder(LoreDocumentAssembler.BLOCK_PLACEHOLDER)),
                    List.of(LoreDocumentAssembler.BLOCK_PLACEHOLDER));

            assertEquals(LoreDocumentAssembler.BLOCK_PLACEHOLDER, doc.text());
            assertTrue(doc.regions().isEmpty());
        }
    }

    @Nested
    @DisplayName("buildBlockHeader")
    class BuildBlockHeader {

        @Test
        @DisplayName("normal block header carries id, spoilerType and role ids as strings")
        void normalHeader() {
            Block block = Block.builder()
                    .id(5L).content("x")
                    .roles(Set.of())
                    .build();

            assertEquals("::: restrictedBlock {\"id\":\"5\",\"spoilerType\":\"normal\",\"roles\":[]}",
                    assembler.buildBlockHeader(block));
        }

        @Test
        @DisplayName("chance block header embeds skill and threshold")
        void chanceHeader() {
            com.cozyfireplace.server.accounts.stats.Skill skill =
                    com.cozyfireplace.server.accounts.stats.Skill.builder()
                            .key("perception").label("Perception").build();
            com.cozyfireplace.server.lore.blocks.ChanceConfig config =
                    com.cozyfireplace.server.lore.blocks.ChanceConfig.builder()
                            .id(1L).threshold(15).skill(skill).build();
            Block block = Block.builder()
                    .id(7L).content("x").chanceConfig(config).roles(Set.of()).build();

            assertEquals("""
                    ::: restrictedBlock {"id":"7","spoilerType":"chance","roles":[],"chance":{"skill":"perception","threshold":15}}""",
                    assembler.buildBlockHeader(block));
        }
    }
}
