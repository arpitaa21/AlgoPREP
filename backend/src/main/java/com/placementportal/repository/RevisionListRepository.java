package com.placementportal.repository;

import com.placementportal.entity.RevisionList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RevisionListRepository extends JpaRepository<RevisionList, Long> {
    List<RevisionList> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<RevisionList> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
}
