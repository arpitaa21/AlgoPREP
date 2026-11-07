package com.placementportal.repository;

import com.placementportal.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("select a from Answer a join fetch a.question q join fetch q.topic "
            + "left join fetch a.selectedOption where a.attempt.id = :attemptId order by q.id")
    List<Answer> findByAttemptWithDetails(@Param("attemptId") Long attemptId);

    @Query("select distinct a.question.id from Answer a where a.attempt.user.id = :userId and a.correct = true")
    List<Long> findCorrectQuestionIds(@Param("userId") Long userId);
}
