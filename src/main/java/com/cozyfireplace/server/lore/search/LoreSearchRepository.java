package com.cozyfireplace.server.lore.search;

import com.cozyfireplace.server.lore.Lore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Нативные запросы полнотекстового поиска по Lore.
 * <p>
 * Три запроса, три раза один и тот же предикат видимости
 * (LoreSearchSql.BLOCK_VISIBLE) — править только там.
 * <p>
 * Тэги из поиска убраны: вместе с ними ушли LEFT JOIN lore_tags,
 * GROUP BY и HAVING. Проверка ролей переписана с LEFT JOIN lore_roles на
 * EXISTS — join размножал строки (по одной на каждую подходящую роль),
 * из-за чего планировщик неверно оценивал стоимость LIMIT.
 */
public interface LoreSearchRepository extends JpaRepository<Lore, Long> {

    @Query(value = LoreSearchSql.SEARCH_CTE + """
            SELECT vl.id       AS id,
                   vl.title    AS title,
                   vl.by_admin AS byAdmin,
                   (
                     EXISTS (SELECT 1 FROM lore_roles lr WHERE lr.lore_id = vl.id)
                     OR EXISTS (SELECT 1 FROM block b2 WHERE b2.lore_id = vl.id)
                   )           AS secret,
                   vl.content  AS content
            FROM visible_lore vl
            JOIN matched m ON m.id = vl.id
            ORDER BY vl.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<LoreSearchProjection> search(
            @Param("query") String query,
            @Param("roomId") Long roomId,
            @Param("viewerAccountId") Long viewerAccountId,
            @Param("viewerCharId") Long viewerCharId,
            @Param("canSeeAll") boolean canSeeAll,
            @Param("viewerHasRoles") boolean viewerHasRoles,
            @Param("viewerRoleIds") long[] viewerRoleIds,
            @Param("title") String title,
            @Param("createdByCreator") Boolean createdByCreator,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * Точный totalElements. Раньше приложение считало его само, прогоняя КАЖДУЮ
     * совпавшую строку через process(), потому что БД не умела проверить
     * видимость текста внутри content. Теперь умеет — видимость стала свойством
     * строки, а не подстроки, — поэтому обычный count(*) и корректен, и не течёт.
     */
    @Query(value = LoreSearchSql.SEARCH_CTE + """
            SELECT count(*) FROM matched
            """, nativeQuery = true)
    long countSearch(
            @Param("query") String query,
            @Param("roomId") Long roomId,
            @Param("viewerAccountId") Long viewerAccountId,
            @Param("viewerCharId") Long viewerCharId,
            @Param("canSeeAll") boolean canSeeAll,
            @Param("viewerHasRoles") boolean viewerHasRoles,
            @Param("viewerRoleIds") long[] viewerRoleIds,
            @Param("title") String title,
            @Param("createdByCreator") Boolean createdByCreator,
            @Param("status") String status
    );

    /**
     * ВСЕ видимые блоки страницы, а не только совпавшие: блок, стоящий рядом с
     * совпадением в публичном тексте, всё равно нужен — из него берётся контекст
     * вокруг матча.
     */
    @Query(value = """
            SELECT b.id
            FROM block b
            WHERE b.lore_id = ANY(:loreIds)
              AND """ + LoreSearchSql.BLOCK_VISIBLE, nativeQuery = true)
    List<Long> findVisibleBlockIds(
            @Param("loreIds") long[] loreIds,
            @Param("viewerCharId") Long viewerCharId,
            @Param("canSeeAll") boolean canSeeAll,
            @Param("viewerHasRoles") boolean viewerHasRoles,
            @Param("viewerRoleIds") long[] viewerRoleIds
    );

    /**
     * Пакетная подсветка: один round-trip на всю страницу вместо одного на
     * каждый сегмент. ts_headline применяется к КАЖДОМУ сегменту отдельно —
     * публичные куски и тела блоков — и никогда к собранному документу,
     * потому что в собранном есть строка-заголовок
     * "::: restrictedBlock {json}", и HighlightAll=true с радостью вставит
     * "==" внутрь этого JSON (запрос вида "1234" или по ключу навыка),
     * после чего фронт не сможет его распарсить.
     */
    @Query(value = """
            SELECT t.ord AS ord,
                   ts_headline('russian', t.txt,
                               phraseto_tsquery('russian', :query),
                               :options) AS headline
            FROM unnest(CAST(:ords AS bigint[]), CAST(:texts AS text[])) AS t(ord, txt)
            """, nativeQuery = true)
    List<HighlightProjection> highlightBatch(
            @Param("ords") long[] ords,
            @Param("texts") String[] texts,
            @Param("query") String query,
            @Param("options") String options
    );
}