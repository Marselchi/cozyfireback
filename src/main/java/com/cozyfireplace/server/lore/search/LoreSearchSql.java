package com.cozyfireplace.server.lore.search;

/**
 * Константы SQL для полнотекстового поиска по Lore.
 * <p>
 * Вынесены отдельно по одной причине: предикат видимости блока должен быть
 * ОДИН на три запроса — выборку страницы, подсчёт totalElements и загрузку
 * видимых блоков. Если эти три разойдутся, получится утечка: либо
 * totalElements подтвердит существование текста, которого пользователь не
 * видит, либо в сниппет попадёт блок, который не прошёл проверку.
 * <p>
 * Правило видимости блока (Block):
 * <ol>
 *   <li>роли: у блока нет ролей ИЛИ у зрителя есть хотя бы одна из них;</li>
 *   <li>бросок: у блока нет ChanceConfig ИЛИ у персонажа зрителя есть
 *       AccessRecord со статусом PASSED/GRANTED.</li>
 * </ol>
 * Оба условия должны выполняться одновременно.
 * <p>
 * Предикат ссылается на алиас {@code b} — таблица block должна быть
 * заджойнена именно под этим алиасом.
 */
public final class LoreSearchSql {

    private LoreSearchSql() {
    }

    public static final String BLOCK_VISIBLE = """
            (
              :canSeeAll = TRUE
              OR (
                   (
                     NOT EXISTS (SELECT 1 FROM block_roles br WHERE br.block_id = b.id)
                     OR (:viewerHasRoles = TRUE AND EXISTS (
                           SELECT 1 FROM block_roles br
                           WHERE br.block_id = b.id
                             AND br.role_id = ANY(:viewerRoleIds)))
                   )
                   AND (
                     NOT EXISTS (SELECT 1 FROM chance_config cc WHERE cc.block_id = b.id)
                     OR EXISTS (
                           SELECT 1 FROM access_record ar
                           WHERE ar.block_id = b.id
                             AND ar.account_char_id = :viewerCharId
                             AND ar.status IN ('PASSED', 'GRANTED'))
                   )
                 )
            )
            """;

    /**
     * Общая часть запроса страницы и запроса count.
     * <p>
     * visible_lore — записи комнаты, прошедшие фильтры и entry-level проверку
     * lore_roles. matched — те из них, где есть совпадение ЛИБО в публичном
     * тексте (lore.content_tsv, из которого маркеры вычищены), ЛИБО в
     * видимом блоке. Видимость теперь целиком выражается в SQL, поэтому
     * count(*) по matched — точный, и приложению больше не нужен цикл
     * дочитывания.
     */
    public static final String SEARCH_CTE = """
            WITH visible_lore AS (
                SELECT l.id                          AS id,
                       l.title                       AS title,
                       l.content                     AS content,
                       l.content_tsv                 AS content_tsv,
                       (a.id = r.creator_account_id) AS by_admin
                FROM lore l
                JOIN accounts a ON a.id = l.account_id
                JOIN rooms    r ON r.id = a.room_id
                LEFT JOIN lore_view lv ON lv.lore_id = l.id AND lv.account_id = :viewerAccountId
                WHERE r.id = :roomId
                  AND (:title IS NULL OR LOWER(l.title) LIKE CONCAT('%', LOWER(:title), '%'))
                  AND (
                        :createdByCreator IS NULL
                        OR (:createdByCreator = TRUE  AND a.id =  r.creator_account_id)
                        OR (:createdByCreator = FALSE AND a.id <> r.creator_account_id)
                      )
                  AND (
                        :status IS NULL
                        OR (:status = 'viewed'   AND lv.viewed = TRUE)
                        OR (:status = 'unviewed' AND lv.viewed IS NULL)
                        OR (:status = 'updated'  AND lv.viewed = FALSE)
                      )
                  AND (
                        :canSeeAll = TRUE
                        OR NOT EXISTS (SELECT 1 FROM lore_roles lr WHERE lr.lore_id = l.id)
                        OR (:viewerHasRoles = TRUE AND EXISTS (
                              SELECT 1 FROM lore_roles lr
                              WHERE lr.lore_id = l.id
                                AND lr.role_id = ANY(:viewerRoleIds)))
                      )
            ),
            matched AS (
                    SELECT vl.id AS id
                    FROM visible_lore vl
                    WHERE vl.content_tsv @@ phraseto_tsquery('russian', :query)
                UNION
                    SELECT b.lore_id AS id
                    FROM block b
                    JOIN visible_lore vl ON vl.id = b.lore_id
                    WHERE b.content_tsv @@ phraseto_tsquery('russian', :query)
                      AND """ + BLOCK_VISIBLE + """
            )
            """;
}