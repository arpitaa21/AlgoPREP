package com.placementportal.dto;

import com.placementportal.entity.Difficulty;
import com.placementportal.entity.SubmissionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** DTOs for revision lists, coding submissions and progress statistics. */
public final class StudyDtos {

    private StudyDtos() { }

    public record RevisionListRequest(@NotBlank @Size(max = 100) String name) { }

    public record RevisionListSummary(Long id, String name, int questionCount) { }

    public record RevisionListDetail(Long id, String name, List<QuestionDtos.QuestionSummary> questions) { }

    public record SubmissionRequest(
            @NotNull Long questionId,
            @NotBlank @Size(max = 30) String language,
            @Size(max = 20000) String code,
            @NotNull SubmissionStatus status,
            @Min(0) Integer timeTakenMinutes) { }

    public record SubmissionResponse(Long id, Long questionId, String questionTitle, String language,
                                     String code, SubmissionStatus status, Integer timeTakenMinutes,
                                     Instant submittedAt) { }

    public record TopicProgress(Long topicId, String topicName, long total, long solved, double percentage) { }

    public record StatsResponse(long totalDsaQuestions, long solvedCount,
                                Map<Difficulty, Long> solvedByDifficulty,
                                Map<Difficulty, Long> totalByDifficulty,
                                List<TopicProgress> topicProgress,
                                long testsTaken, double averageTestPercentage,
                                Map<String, Long> submissionsLast30Days,
                                int currentStreakDays) { }
}
