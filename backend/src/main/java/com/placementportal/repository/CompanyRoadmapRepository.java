package com.placementportal.repository;

import com.placementportal.entity.CompanyRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyRoadmapRepository extends JpaRepository<CompanyRoadmap, Long> {

    @Query("select r from CompanyRoadmap r left join fetch r.topic where r.company.id = :companyId order by r.stepOrder")
    List<CompanyRoadmap> findByCompanyWithTopic(@Param("companyId") Long companyId);

    boolean existsByTopicId(Long topicId);
}
