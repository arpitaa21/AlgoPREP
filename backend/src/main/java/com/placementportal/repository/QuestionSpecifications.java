package com.placementportal.repository;

import com.placementportal.entity.Difficulty;
import com.placementportal.entity.Question;
import com.placementportal.entity.QuestionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the WHERE clause dynamically: only the filters the user actually sent are added.
 * Why Specifications instead of one big @Query with "(:x is null or ...)"? It is type-safe and avoids
 * null-parameter typing problems with enums in Hibernate 6.
 */
public final class QuestionSpecifications {

    private QuestionSpecifications() { }

    public static Specification<Question> filter(Long topicId, Difficulty difficulty,
                                                 QuestionType type, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (topicId != null) {
                predicates.add(cb.equal(root.get("topic").get("id"), topicId));
            }
            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
