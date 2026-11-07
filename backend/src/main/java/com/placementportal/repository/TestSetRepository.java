package com.placementportal.repository;

import com.placementportal.entity.TestSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestSetRepository extends JpaRepository<TestSet, Long> {
    List<TestSet> findByActiveTrueOrderByIdAsc();
}
