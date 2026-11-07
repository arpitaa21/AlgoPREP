package com.placementportal.repository;

import com.placementportal.entity.AttemptStatus;
import com.placementportal.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    /** Ownership check built into the query: a student can never load someone else's attempt. */
    Optional<TestAttempt> findByIdAndUserId(Long id, Long userId);

    Optional<TestAttempt> findFirstByUserIdAndTestSetIdAndStatus(Long userId, Long testSetId, AttemptStatus status);

    @Query("select a from TestAttempt a join fetch a.testSet where a.user.id = :userId order by a.startedAt desc")
    List<TestAttempt> findHistory(@Param("userId") Long userId);

    @Query("select avg(a.score * 100.0 / a.totalQuestions) from TestAttempt a "
            + "where a.user.id = :userId and a.status = :status and a.totalQuestions > 0")
    Double averagePercentage(@Param("userId") Long userId, @Param("status") AttemptStatus status);

    @Query("select avg(a.score * 100.0 / a.totalQuestions) from TestAttempt a "
            + "where a.status = :status and a.totalQuestions > 0")
    Double averagePercentageAll(@Param("status") AttemptStatus status);

    long countByUserIdAndStatus(Long userId, AttemptStatus status);
}
