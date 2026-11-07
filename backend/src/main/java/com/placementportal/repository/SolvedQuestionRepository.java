package com.placementportal.repository;

import com.placementportal.entity.QuestionType;
import com.placementportal.entity.SolvedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SolvedQuestionRepository extends JpaRepository<SolvedQuestion, Long> {

    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);

    long deleteByUserIdAndQuestionId(Long userId, Long questionId);

    @Query("select s from SolvedQuestion s join fetch s.question q join fetch q.topic "
            + "where s.user.id = :userId order by s.solvedAt desc")
    List<SolvedQuestion> findAllByUser(@Param("userId") Long userId);

    @Query("select s.question.id from SolvedQuestion s where s.user.id = :userId and s.question.id in :ids")
    List<Long> findSolvedIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Query("select s.question.id from SolvedQuestion s where s.user.id = :userId")
    List<Long> findAllSolvedIds(@Param("userId") Long userId);

    // ---- Analytics (JPQL) ----
    @Query("select q.difficulty as difficulty, count(s) as cnt from SolvedQuestion s join s.question q "
            + "where s.user.id = :userId and q.type = :type group by q.difficulty")
    List<DifficultyCount> countSolvedByDifficulty(@Param("userId") Long userId, @Param("type") QuestionType type);

    // ---- Analytics (native SQL) ----
    // Native SQL is shown here on purpose: LEFT JOINs + conditional counting per topic read more clearly in SQL.
    // We return Object[] rows (not an interface projection) because column-alias case differs between
    // MySQL and H2 (used in tests); reading by position works on both.
    // Row layout: [0] topic id, [1] topic name, [2] total DSA questions, [3] solved by this user.
    @Query(value = """
            SELECT t.id, t.name,
                   COUNT(DISTINCT q.id) AS total_questions,
                   COUNT(DISTINCT s.id) AS solved_questions
            FROM topics t
            LEFT JOIN questions q ON q.topic_id = t.id AND q.type = 'DSA'
            LEFT JOIN solved_questions s ON s.question_id = q.id AND s.user_id = :userId
            GROUP BY t.id, t.name
            ORDER BY t.name
            """, nativeQuery = true)
    List<Object[]> topicProgress(@Param("userId") Long userId);
}
