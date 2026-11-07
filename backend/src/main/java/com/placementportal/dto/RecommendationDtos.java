package com.placementportal.dto;

import com.placementportal.entity.Difficulty;
import com.placementportal.entity.QuestionType;

public final class RecommendationDtos {

    private RecommendationDtos() { }

    public record WeakTopic(Long topicId, String topicName, int attempted, int correct,
                            double accuracy, double weaknessScore) { }

    public record RecommendedQuestion(Long questionId, String title, Difficulty difficulty, QuestionType type,
                                      Long topicId, String topicName, String reason) { }
}
