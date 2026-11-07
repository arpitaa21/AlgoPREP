package com.placementportal.repository;

import com.placementportal.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findAllByOrderByNameAsc();
    boolean existsByName(String name);
}
