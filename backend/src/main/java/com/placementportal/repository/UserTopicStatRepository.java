package com.placementportal.repository;

import com.placementportal.entity.UserTopicStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserTopicStatRepository extends JpaRepository<UserTopicStat, Long> {

    @Query("select s from UserTopicStat s join fetch s.topic where s.user.id = :userId")
    List<UserTopicStat> findByUserIdWithTopic(@Param("userId") Long userId);

    Optional<UserTopicStat> findByUserIdAndTopicId(Long userId, Long topicId);
}
