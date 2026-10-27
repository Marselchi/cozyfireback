package com.cozyfireplace.server.templates;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    Page<Template> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<Template> findByName(String name);

    // Комнаты у шаблона нет — она берётся через автора. Чужой templateId даёт пустой
    // результат (404), а не правку/удаление шаблона из другой комнаты.
    @Query("""
        SELECT t FROM Template t
        JOIN t.author acc
        JOIN acc.room r
        WHERE r.id = :roomId AND t.id = :templateId
    """)
    Optional<Template> findByRoomIdAndId(@Param("roomId") Long roomId,
                                         @Param("templateId") Long templateId);
}
