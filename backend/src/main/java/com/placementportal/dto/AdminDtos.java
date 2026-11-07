package com.placementportal.dto;

import com.placementportal.entity.Difficulty;
import com.placementportal.entity.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AdminDtos {

    private AdminDtos() { }

    public record TopicCount(String topic, long total) { }

    public record DashboardResponse(long totalUsers, long totalStudents, long totalQuestions, long totalTests,
                                    long totalAttempts, long totalSubmissions, double averageTestPercentage,
                                    Map<Difficulty, Long> questionsByDifficulty,
                                    Map<QuestionType, Long> questionsByType,
                                    List<TopicCount> questionsByTopic) { }

    public record UserSummary(Long id, String name, String email, List<String> roles,
                              boolean enabled, Instant createdAt) { }

    public record TestRequest(
            @NotBlank @Size(max = 150) String title,
            @Size(max = 500) String description,
            @Min(1) @Max(300) int durationMinutes,
            boolean active,
            @NotEmpty List<Long> questionIds) { }

    public record AdminTestResponse(Long id, String title, String description, int durationMinutes,
                                    boolean active, List<Long> questionIds) { }
}
