package com.cozyfireplace.server.answers;

import com.cozyfireplace.server.answers.dto.AnswerPathProjection;
import com.cozyfireplace.server.answers.dto.AnswerWithAdminFlag;
import com.cozyfireplace.server.answers.dto.AnswerWithReplyCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("SELECT a.author.id FROM Answer a WHERE a.id = :id")
    Optional<Long> findAuthorIdByAnswerId(@Param("id") Long id);

    @Query("""
    SELECT new com.cozyfireplace.server.answers.dto.AnswerWithAdminFlag(
        a,
        CASE WHEN a.author.id = a.question.author.room.creator.id THEN true ELSE false END
    )
    FROM Answer a
    LEFT JOIN FETCH a.author
    LEFT JOIN FETCH a.question q
    LEFT JOIN FETCH q.author qa
    LEFT JOIN FETCH qa.room
    WHERE a.id = :id
""")
    java.util.Optional<AnswerWithAdminFlag> findByIdFlaged(@Param("id") Long id);

    @Query("""
        SELECT a FROM Answer a
        LEFT JOIN FETCH a.author
        WHERE a.parent.id = :parentId
        ORDER BY a.id
    """)
    List<Answer> findChildAnswersByParentId(@Param("parentId") Long parentId);

    @Query("""
        SELECT COUNT(a) FROM Answer a
        WHERE a.parent.id = :parentId
    """)
    long countRepliesByParentId(@Param("parentId") Long parentId);

    @Query("""
        SELECT a FROM Answer a
        LEFT JOIN FETCH a.author
        LEFT JOIN FETCH a.question
        WHERE a.id = :id
    """)
    Optional<Answer> findByIdWithAuthorAndQuestion(@Param("id") Long id);

    @Query("""
        SELECT new com.cozyfireplace.server.answers.dto.AnswerWithReplyCount(
            a, COALESCE(COUNT(r.id), 0),
            CASE WHEN a.author.id = rm.creator.id THEN true ELSE false END
        )
        FROM Answer a
        LEFT JOIN a.replies r
        JOIN a.question q
        JOIN q.author qa
        JOIN qa.room rm
        WHERE a.id = :id
        GROUP BY a.id, rm.creator.id
    """)
    Optional<AnswerWithReplyCount> findByIdWithReplyCount(@Param("id") Long id);

    @Query("""
        SELECT new com.cozyfireplace.server.answers.dto.AnswerWithReplyCount(
            a, COALESCE(COUNT(r.id), 0),
            CASE WHEN a.author.id = rm.creator.id THEN true ELSE false END
        )
        FROM Answer a
        LEFT JOIN a.replies r
        JOIN a.question q
        JOIN q.author qa
        JOIN qa.room rm
        WHERE a.parent.id = :parentId
        GROUP BY a.id, rm.creator.id
        ORDER BY a.id
    """)
    List<AnswerWithReplyCount> findChildAnswersWithReplyCount(@Param("parentId") Long parentId);

    @Query(value = """
WITH RECURSIVE answer_path AS (
    SELECT a.id, a.content, a.created_at, a.updated_at,
           a.parent_id, a.author_id, a.question_id
    FROM answers a
    WHERE a.id = :answerId

    UNION ALL

    SELECT parent.id, parent.content, parent.created_at, parent.updated_at,
           parent.parent_id, parent.author_id, parent.question_id
    FROM answers parent
    JOIN answer_path ap ON ap.parent_id = parent.id
),

reply_counts AS (
    SELECT r.parent_id, COUNT(*) AS reply_count
    FROM answers r
    GROUP BY r.parent_id
)

SELECT
    a.id AS id,
    a.content AS content,
    a.created_at AS createdAt,
    a.updated_at AS updatedAt,
    a.parent_id AS parentId,

    acc.id AS authorId,
    acc.name AS username,

    COALESCE(rc.reply_count, 0) AS replyCount,

    CASE WHEN acc.id = rm.creator_account_id THEN true ELSE false END AS isAdmin

FROM answer_path a
JOIN accounts acc ON acc.id = a.author_id
JOIN questions q ON q.id = a.question_id
JOIN accounts qa ON qa.id = q.author_id
JOIN rooms rm ON rm.id = qa.room_id

LEFT JOIN reply_counts rc ON rc.parent_id = a.id
""", nativeQuery = true)
    List<AnswerPathProjection> findAnswerPathDetailed(@Param("answerId") Long answerId);


    @Query("""
        SELECT a FROM Answer a
        LEFT JOIN FETCH a.author
        LEFT JOIN FETCH a.replies r
        LEFT JOIN FETCH r.author
        WHERE a.id = :id
    """)
    Optional<Answer> findByIdWithReplies(@Param("id") Long id);

    @Query("""
        SELECT new com.cozyfireplace.server.answers.dto.AnswerWithReplyCount(
            a, COALESCE(COUNT(r.id), 0),
            CASE WHEN a.author.id = rm.creator.id THEN true ELSE false END
        )
        FROM Answer a
        LEFT JOIN a.replies r
        JOIN a.question q
        JOIN q.author qa
        JOIN qa.room rm
        WHERE a.question.id = :questionId AND a.parent IS NULL
        GROUP BY a.id, rm.creator.id
        ORDER BY a.id
    """)
    List<AnswerWithReplyCount> findQuestionAnswersWithReplyCount(@Param("questionId") Long questionId);
}
