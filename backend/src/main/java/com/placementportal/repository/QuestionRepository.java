package com.placementportal.repository;

import com.placementportal.entity.Question;
import com.placementportal.entity.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    /**
     * Filtered + paginated search. The @EntityGraph tells Hibernate to JOIN the topic in the same SQL,
     * which prevents the classic N+1 problem (1 query for the page + 1 query per row to load its topic).
     */
    @Override
    @EntityGraph(attributePaths = "topic")
    Page<Question> findAll(Specification<Question> spec, Pageable pageable);

    /** Detail view: topic + options in one query. Only ONE collection is fetched (fetching two List collections breaks Hibernate). */
    @Query("select distinct q from Question q join fetch q.topic left join fetch q.options where q.id = :id")
    Optional<Question> findDetailById(@Param("id") Long id);

    /** All questions of a test (topic loaded; options are batch-loaded lazily, see default_batch_fetch_size). */
    @Query("select q from Question q join fetch q.topic "
            + "where q.id in (select q2.id from TestSet t join t.questions q2 where t.id = :testId) order by q.id")
    List<Question> findByTestId(@Param("testId") Long testId);

    @Query("select q from Question q join fetch q.topic where q.topic.id = :topicId")
    List<Question> findByTopicIdWithTopic(@Param("topicId") Long topicId);

    boolean existsByTopicId(Long topicId);

    long countByType(QuestionType type);

    @Query("select q.difficulty as difficulty, count(q) as cnt from Question q where q.type = :type group by q.difficulty")
    List<DifficultyCount> countByDifficultyForType(@Param("type") QuestionType type);

    @Query("select q.difficulty as difficulty, count(q) as cnt from Question q group by q.difficulty")
    List<DifficultyCount> countByDifficulty();

    interface TopicCount {
        String getTopic();
        Long getTotal();
    }

    @Query("select t.name as topic, count(q) as total from Question q join q.topic t group by t.name order by t.name")
    List<TopicCount> countByTopic();
}
