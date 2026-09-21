package com.cozyfireplace.server.questions;

import com.cozyfireplace.server.accounts.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("""
        SELECT q FROM Question q
        LEFT JOIN FETCH q.author a
        LEFT JOIN FETCH q.lore
        LEFT JOIN FETCH q.character
        WHERE a.room.id = :roomId
          AND (:categoryId IS NULL OR q.category = :categoryId)
          AND (:isAnswered IS NULL OR q.isAnswered = :isAnswered)
          AND (:loreId IS NULL OR q.lore.id = :loreId)
          AND (:characterId IS NULL OR q.character.id = :characterId)
          AND (:hasLore IS NULL OR :hasLore = true AND q.lore IS NOT NULL OR :hasLore = false AND q.lore IS NULL)
          AND (:hasCharacter IS NULL OR :hasCharacter = true AND q.character IS NOT NULL OR :hasCharacter = false AND q.character IS NULL)
          AND (:authorId IS NULL OR q.author.id = :authorId)
          AND (:search IS NULL OR LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY q.id
    """)
    Page<Question> findAllWithFilters(
            @Param("roomId") Long roomId,
            @Param("categoryId") Integer categoryId,
            @Param("isAnswered") Boolean isAnswered,
            @Param("loreId") Long loreId,
            @Param("characterId") Long characterId,
            @Param("hasLore") Boolean hasLore,
            @Param("hasCharacter") Boolean hasCharacter,
            @Param("authorId") Long authorId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        SELECT q FROM Question q
        LEFT JOIN FETCH q.author
        LEFT JOIN FETCH q.lore
        LEFT JOIN FETCH q.answers a
        LEFT JOIN FETCH a.author
        LEFT JOIN FETCH a.replies r
        LEFT JOIN FETCH r.author
        WHERE q.id = :id
    """)
    Optional<Question> findByIdWithAnswersAndAuthor(@Param("id") Long id);

    @Query("SELECT q.author.id FROM Question q WHERE q.id = :id")
    Optional<Long> findAuthorIdByQuestionId(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT q.author FROM Question q
        WHERE q.author.room.id = :roomId
    """)
    List<Account> findDistinctAuthorsByRoomId(@Param("roomId") Long roomId);

    @Query("""
        SELECT COUNT(q) FROM Question q
        WHERE q.author.room.id = :roomId
        AND q.isAnswered = false
    """)
    long countUnansweredByRoomId(@Param("roomId") Long roomId);

    @Query("""
    SELECT q FROM Question q
        LEFT JOIN FETCH q.author
        WHERE q.id = :id
    """)
    Optional<Question> findByIdWithAuthor(@Param("id") Long id);

    @Query("""
        SELECT COUNT(q) FROM Question q
        WHERE q.lore.id = :loreId
        AND q.isAnswered = false
    """)
    long countUnansweredByLoreId(@Param("loreId") Long loreId);

    @Query("""
        SELECT COUNT(q) FROM Question q
        WHERE q.character.id = :characterId
        AND q.isAnswered = false
    """)
    long countUnansweredByCharacterId(@Param("characterId") Long characterId);
}
