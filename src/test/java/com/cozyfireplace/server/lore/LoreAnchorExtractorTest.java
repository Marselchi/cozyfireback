package com.cozyfireplace.server.lore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LoreAnchorExtractor}.
 * <p>
 * The extractor reads its host from {@code @Value} fields, so the test injects
 * a fixed host ("cozyfireplace.ru") via reflection and verifies link pattern
 * building, anchor extraction and invalid-link removal.
 */
@DisplayName("LoreAnchorExtractor")
class LoreAnchorExtractorTest {

    private static final String HOST = "cozyfireplace.ru";

    private LoreAnchorExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new LoreAnchorExtractor();
        ReflectionTestUtils.setField(extractor, "protocolRegex", "http?");
        ReflectionTestUtils.setField(extractor, "host", HOST);
    }

    private static String loreLink(String linkText, long loreId, String header) {
        return "[" + linkText + "](https://" + HOST + "/rooms/room-1/lore/" + loreId + "#" + header + ")";
    }

    @Nested
    @DisplayName("buildLoreLinkPattern")
    class BuildLoreLinkPattern {

        @Test
        @DisplayName("matches a canonical lore link")
        void matchesCanonicalLink() {
            String link = loreLink("Secret", 42L, "hidden-header");
            assertTrue(extractor.buildLoreLinkPattern().matcher(link).find());
        }

        @Test
        @DisplayName("matches links with http scheme and www prefix and .ru suffix")
        void matchesVariants() {
            assertTrue(extractor.buildLoreLinkPattern()
                    .matcher("[t](http://" + HOST + "/rooms/r/lore/1#h)").find());
            assertTrue(extractor.buildLoreLinkPattern()
                    .matcher("[t](https://www." + HOST + "/rooms/r/lore/1#h)").find());
        }

        @Test
        @DisplayName("does not match links to another host or without an anchor")
        void rejectsForeignOrIncompleteLinks() {
            assertFalse(extractor.buildLoreLinkPattern()
                    .matcher("[t](https://evil.example.com/rooms/r/lore/1#h)").find());
            assertFalse(extractor.buildLoreLinkPattern()
                    .matcher("[t](https://" + HOST + "/rooms/r/lore/1)").find());
        }
    }

    @Nested
    @DisplayName("extractAnchorLinks")
    class ExtractAnchorLinks {

        @Test
        @DisplayName("null or empty content returns an empty list")
        void emptyContent() {
            assertEquals(List.of(), extractor.extractAnchorLinks(null));
            assertEquals(List.of(), extractor.extractAnchorLinks(""));
        }

        @Test
        @DisplayName("no lore links in content returns an empty list")
        void noLinks() {
            assertEquals(List.of(), extractor.extractAnchorLinks("Just some # text without links"));
        }

        @Test
        @DisplayName("extracts loreId, header and link text for each anchor")
        void extractsFields() {
            String content = "Intro " + loreLink("The Hook", 7L, "the-dark-past") + " outro "
                    + loreLink("Other", 8L, "part-two");
            List<LoreAnchorExtractor.AnchorLink> links = extractor.extractAnchorLinks(content);

            assertEquals(2, links.size());
            assertEquals(7L, links.getFirst().loreId());
            assertEquals("the-dark-past", links.getFirst().header());
            assertEquals("The Hook", links.getFirst().linkText());
            assertEquals(8L, links.get(1).loreId());
            assertEquals("part-two", links.get(1).header());
        }
    }

    @Nested
    @DisplayName("removeInvalidLinks")
    class RemoveInvalidLinks {

        @Test
        @DisplayName("null or empty content is returned unchanged")
        void emptyContent() {
            assertNull(extractor.removeInvalidLinks(null, Set.of()));
            assertEquals("", extractor.removeInvalidLinks("", Set.of()));
        }

        @Test
        @DisplayName("valid anchor keeps the full markdown link")
        void validAnchorKept() {
            String link = loreLink("Secret", 42L, "hidden-header");
            String result = extractor.removeInvalidLinks(link, Set.of("42:hidden-header"));
            assertEquals(link, result);
        }

        @Test
        @DisplayName("invalid anchor is replaced by its plain link text")
        void invalidAnchorStrippedToText() {
            String link = loreLink("Secret", 42L, "hidden-header");
            String result = extractor.removeInvalidLinks(link, Set.of("1:something-else"));
            assertEquals("Secret", result);
        }

        @Test
        @DisplayName("mixed content: valid links survive, invalid ones degrade to text")
        void mixedAnchors() {
            String valid = loreLink("Keeper", 1L, "head1");
            String invalid = loreLink("Whisper", 2L, "head2");
            String content = "A " + valid + " B " + invalid + " C";

            String result = extractor.removeInvalidLinks(content, Set.of("1:head1"));

            assertEquals("A " + valid + " B Whisper C", result);
        }

        @Test
        @DisplayName("links to non-lore URLs are left untouched")
        void externalLinksUntouched() {
            String external = "[site](https://example.com/pages/5#anchor)";
            assertEquals(external, extractor.removeInvalidLinks(external, Set.of()));
        }
    }
}
