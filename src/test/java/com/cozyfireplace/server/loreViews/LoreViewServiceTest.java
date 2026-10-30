package com.cozyfireplace.server.loreViews;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoreViewService}.
 * <p>
 * The service is a thin delegation layer over {@link LoreViewRepository}; these tests pin that
 * the view flag is upserted and that editing a lore entry resets its read markers. The composite
 * key's identity semantics (equals/hashCode) are asserted alongside, since they back the JPA
 * primary key.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoreViewService")
class LoreViewServiceTest {

    private static final Long ACCOUNT_ID = 1L;
    private static final Long LORE_ID = 2L;

    @Mock
    private LoreViewRepository loreViewRepository;

    @InjectMocks
    private LoreViewService service;

    @Test
    @DisplayName("setViewed upserts the (account, lore) read marker")
    void setViewed() {
        service.setViewed(ACCOUNT_ID, LORE_ID);

        verify(loreViewRepository).upsertViewed(ACCOUNT_ID, LORE_ID);
    }

    @Test
    @DisplayName("loreUpdated resets every read marker for the lore entry")
    void loreUpdated() {
        when(loreViewRepository.resetViewedByLoreId(LORE_ID)).thenReturn(3);

        service.loreUpdated(LORE_ID);

        verify(loreViewRepository).resetViewedByLoreId(LORE_ID);
    }

    @Nested
    @DisplayName("LoreViewId")
    class CompositeKey {

        @Test
        @DisplayName("is equal and hash-consistent when both parts match")
        void equalWhenSameParts() {
            assertThat(new LoreViewId(LORE_ID, ACCOUNT_ID))
                    .isEqualTo(new LoreViewId(LORE_ID, ACCOUNT_ID))
                    .hasSameHashCodeAs(new LoreViewId(LORE_ID, ACCOUNT_ID));
        }

        @Test
        @DisplayName("differs when either part differs")
        void differsOnAnyPart() {
            assertThat(new LoreViewId(LORE_ID, ACCOUNT_ID))
                    .isNotEqualTo(new LoreViewId(9L, ACCOUNT_ID))
                    .isNotEqualTo(new LoreViewId(LORE_ID, 9L));
        }
    }
}
