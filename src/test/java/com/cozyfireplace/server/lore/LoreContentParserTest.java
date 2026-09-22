package com.cozyfireplace.server.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LoreContentParser}.
 * <p>
 * The parser is a stateless utility with static methods, so no mocking is required.
 * It covers:
 * <ul>
 *   <li>{@code parseRestrictedContent} - removal of restricted blocks the viewer has no access to;</li>
 *   <li>{@code extractAllExcerpts} - extraction of header sections (excerpts) with access filtering;</li>
 *   <li>{@code textToSlug} / {@code encodeHeaderSlug} - anchor slug helpers.</li>
 * </ul>
 */
@DisplayName("LoreContentParser")
class LoreContentParserTest {

    /** Builds a restricted block that is only visible to the given role ids. */
    private static String block(String roleJson, String body) {
        return "::: restrictedBlock " + roleJson + "\n" + body + "\n:::";
    }

    @Nested
    @DisplayName("parseRestrictedContent")
    class ParseRestrictedContent {

        @Test
        @DisplayName("null content is returned unchanged")
        void nullContent() {
            assertNull(LoreContentParser.parseRestrictedContent(null, false, Set.of()));
        }

        @Test
        @DisplayName("empty content is returned unchanged")
        void emptyContent() {
            assertEquals("", LoreContentParser.parseRestrictedContent("", false, Set.of()));
        }

        @Test
        @DisplayName("canSeeAll=true keeps everything untouched")
        void canSeeAllKeepsEverything() {
            String content = "before\n" + block("{\"roles\":[7]}", "secret") + "\nafter";
            assertEquals(content, LoreContentParser.parseRestrictedContent(content, true, Set.of()));
        }

        @Test
        @DisplayName("block with a matching role id is kept")
        void allowedBlockKept() {
            String content = block("{\"roles\":[1,2]}", "secret body");
            String result = LoreContentParser.parseRestrictedContent(content, false, Set.of(2L));
            assertEquals(content, result);
        }

        @Test
        @DisplayName("block without a matching role id is fully removed (including markers)")
        void disallowedBlockRemoved() {
            String content = "visible\n" + block("{\"roles\":[5]}", "secret body") + "\ntail";
            String result = LoreContentParser.parseRestrictedContent(content, false, Set.of(9L));
            assertEquals("visible\n\ntail", result);
        }

        @Test
        @DisplayName("viewer with no roles sees no restricted blocks")
        void viewerWithoutRolesSeesNothing() {
            String content = block("{\"roles\":[5]}", "secret");
            assertEquals("", LoreContentParser.parseRestrictedContent(content, false, Set.of()));
            assertEquals("", LoreContentParser.parseRestrictedContent(content, false, null));
        }

        @Test
        @DisplayName("block without metadata json is removed for non-creators")
        void blockWithoutMetadataRemoved() {
            String content = "::: restrictedBlock\nbody\n:::";
            assertEquals("", LoreContentParser.parseRestrictedContent(content, false, Set.of(1L)));
        }

        @Test
        @DisplayName("block with malformed metadata json is treated as inaccessible")
        void blockWithBrokenMetadataRemoved() {
            // the metadata group only matches a braced one-line payload; once matched,
            // unparseable json yields no role ids -> the block is cut for everyone
            String content = block("{not-json}", "body");
            assertEquals("", LoreContentParser.parseRestrictedContent(content, false, Set.of(1L)));
        }

        @Test
        @DisplayName("unbraced text after the marker is not a block at all and stays intact")
        void brokenMarkerSyntaxStaysUntouched() {
            String content = block("{not-json", "body");
            assertEquals(content, LoreContentParser.parseRestrictedContent(content, false, Set.of(1L)));
        }

        @Test
        @DisplayName("block with non-array roles is treated as inaccessible")
        void blockWithNonArrayRolesRemoved() {
            String content = block("{\"roles\":\"oops\"}", "body");
            assertEquals("", LoreContentParser.parseRestrictedContent(content, false, Set.of(1L)));
        }

        @Test
        @DisplayName("multiple blocks: only allowed ones survive")
        void multipleBlocksFilteredIndividually() {
            String allowed = block("{\"roles\":[1]}", "a");
            String denied = block("{\"roles\":[2]}", "b");
            String content = allowed + "\n---\n" + denied;
            String result = LoreContentParser.parseRestrictedContent(content, false, Set.of(1L));
            assertEquals(allowed + "\n---\n", result);
        }
    }

    @Nested
    @DisplayName("extractAllExcerpts")
    class ExtractAllExcerpts {

        @Test
        @DisplayName("null or empty content yields empty map")
        void emptyContent() {
            assertEquals(Map.of(), LoreContentParser.extractAllExcerpts(null, List.of("h"), true, Set.of()));
            assertEquals(Map.of(), LoreContentParser.extractAllExcerpts("", List.of("h"), true, Set.of()));
        }

        @Test
        @DisplayName("empty header list yields empty map")
        void emptyHeaderList() {
            assertEquals(Map.of(), LoreContentParser.extractAllExcerpts("# Title", List.of(), true, Set.of()));
            assertEquals(Map.of(), LoreContentParser.extractAllExcerpts("# Title", null, true, Set.of()));
        }

        @Test
        @DisplayName("only requested headers are extracted")
        void onlyRequestedHeaders() {
            String content = "# First\nbody1\n\n# Second\nbody2\n";
            Map<String, String> result =
                    LoreContentParser.extractAllExcerpts(content, List.of("second"), true, Set.of());
            assertEquals(Set.of("second"), result.keySet());
            assertEquals("# Second\nbody2", result.get("second"));
        }

        @Test
        @DisplayName("section ends only at the next heading of the same or higher level")
        void sectionBoundedByNextHeading() {
            String content = "# Alpha\none\n## Beta\ntwo\n# Gamma\nthree\n";
            Map<String, String> result =
                    LoreContentParser.extractAllExcerpts(content, List.of("Alpha"), true, Set.of());
            // '## Beta' is deeper, so it stays inside the Alpha section;
            // the section is cut at '# Gamma' (same level)
            assertEquals("# Alpha\none\n## Beta\ntwo", result.get("alpha"));
        }

        @Test
        @DisplayName("nested sub-headings stay inside the excerpt of their parent")
        void subHeadingsIncluded() {
            String content = "# Parent\np-text\n## Child\nc-text\n";
            Map<String, String> result =
                    LoreContentParser.extractAllExcerpts(content, List.of("parent"), true, Set.of());
            assertEquals("# Parent\np-text\n## Child\nc-text", result.get("parent"));
        }

        @Test
        @DisplayName("excerpt longer than 500 chars is truncated with ellipsis")
        void longExcerptTruncated() {
            String longBody = "x".repeat(600);
            String content = "# Big\n" + longBody;
            Map<String, String> result =
                    LoreContentParser.extractAllExcerpts(content, List.of("Big"), true, Set.of());
            String excerpt = result.get("big");
            assertTrue(excerpt.endsWith("..."), "excerpt must end with '...'");
            assertEquals(500 + "...".length(), excerpt.length());
        }

        @Test
        @DisplayName("restricted content is cleaned before excerpts are taken")
        void restrictedContentRespected() {
            String hidden = block("{\"roles\":[42]}", "# Secret\nhidden text");
            String content = "# Public\nvisible\n" + hidden;
            Map<String, String> result =
                    LoreContentParser.extractAllExcerpts(content, List.of("public", "secret"), false, Set.of(1L));
            assertTrue(result.containsKey("public"));
            assertFalse(result.containsKey("secret"),
                    "excerpt from a block the viewer cannot see must not be extracted");
        }

        @Test
        @DisplayName("requested header names are matched case/slug-insensitively")
        void headersMatchedBySlug() {
            String content = "# The Dark  Tower\ntext\n";
            Map<String, String> result =
                    LoreContentParser.extractAllExcerpts(content, List.of("the-dark-tower"), true, Set.of());
            assertTrue(result.containsKey("the-dark-tower"));
        }
    }

    @Nested
    @DisplayName("textToSlug")
    class TextToSlug {

        @Test
        @DisplayName("null and empty produce empty string")
        void nullAndEmpty() {
            assertEquals("", LoreContentParser.textToSlug(null));
            assertEquals("", LoreContentParser.textToSlug(""));
        }

        @Test
        @DisplayName("latin text is lowercased and spaces become single dashes")
        void latinText() {
            assertEquals("the-broken-compass", LoreContentParser.textToSlug("The Broken Compass"));
        }

        @Test
        @DisplayName("special characters are stripped")
        void specialCharactersRemoved() {
            assertEquals("hello-world", LoreContentParser.textToSlug("Hello!!! World..."));
        }

        @Test
        @DisplayName("cyrillic letters are preserved; 'ё' is stripped (outside the а-я range)")
        void cyrillicPreserved() {
            assertEquals("тмный-лес", LoreContentParser.textToSlug("Тёмный Лес"));
            assertEquals("лес", LoreContentParser.textToSlug("Лес"));
        }

        @Test
        @DisplayName("leading and trailing dashes are trimmed, repeats collapsed")
        void dashesNormalized() {
            assertEquals("a-b", LoreContentParser.textToSlug("  ---a   b---  "));
        }
    }

    @Nested
    @DisplayName("encodeHeaderSlug")
    class EncodeHeaderSlug {

        @Test
        @DisplayName("null and empty produce empty string")
        void nullAndEmpty() {
            assertEquals("", LoreContentParser.encodeHeaderSlug(null));
            assertEquals("", LoreContentParser.encodeHeaderSlug(""));
        }

        @Test
        @DisplayName("spaces are URL-encoded")
        void spacesEncoded() {
            assertEquals("The+Broken+Compass", LoreContentParser.encodeHeaderSlug("The Broken Compass"));
        }

        @Test
        @DisplayName("cyrillic text is percent-encoded in UTF-8")
        void cyrillicEncoded() {
            assertEquals("%D0%9B%D0%B5%D1%81", LoreContentParser.encodeHeaderSlug("Лес"));
        }
    }
}
