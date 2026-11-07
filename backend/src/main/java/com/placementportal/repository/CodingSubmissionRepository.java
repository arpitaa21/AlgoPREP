package com.placementportal.repository;

import com.placementportal.entity.CodingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CodingSubmissionRepository extends JpaRepository<CodingSubmission, Long> {

    @Query("select s from CodingSubmission s join fetch s.question where s.user.id = :userId order by s.submittedAt desc")
    List<CodingSubmission> findAllByUser(@Param("userId") Long userId);

    @Query("select s from CodingSubmission s join fetch s.question "
            + "where s.user.id = :userId and s.question.id = :questionId order by s.submittedAt desc")
    List<CodingSubmission> findByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /** Only the timestamps: cheap to load, grouped by day in Java (keeps the SQL portable). */
    @Query("select s.submittedAt from CodingSubmission s where s.user.id = :userId and s.submittedAt >= :since")
    List<Instant> findSubmissionTimesSince(@Param("userId") Long userId, @Param("since") Instant since);
}
