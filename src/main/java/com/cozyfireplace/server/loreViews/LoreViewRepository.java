package com.cozyfireplace.server.loreViews;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LoreViewRepository extends JpaRepository<LoreView, LoreViewId> {

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO lore_view (lore_id, account_id, viewed)
    VALUES (:loreId, :accountId, true)
    ON CONFLICT (lore_id, account_id)
    DO UPDATE SET viewed = true
    WHERE lore_view.viewed IS NOT TRUE
    """, nativeQuery = true)
    void upsertViewed(@Param("accountId") Long accountId, @Param("loreId") Long loreId);

    @Modifying
    @Transactional
    @Query("UPDATE LoreView lv SET lv.viewed = false WHERE lv.id.loreId = :loreId")
    int resetViewedByLoreId(@Param("loreId") Long loreId);
}
