package com.placementportal.dto;

import com.placementportal.entity.AttemptStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class TestDtos {

    private TestDtos() { }

    public record TestSummary(Long id, String title, String description, int durationMinutes, int questionCount) { }

    /** A question shown DURING a test: options carry no "correct" flag. */
    public record TestQuestion(Long id, String title, String description, String topicName,
                               List<QuestionDtos.OptionResponse> options) { }

    public record StartTestResponse(Long attemptId, Long testId, String title, Instant startedAt,
                                    Instant deadline, int durationMinutes, long secondsRemaining,
                                    List<TestQuestion> questions) { }

    public record AnswerRequest(@NotNull Long questionId, Long optionId) { }   // optionId null = skipped

    public record SubmitRequest(@NotNull @Valid List<AnswerRequest> answers) { }

    public record AnswerReview(Long questionId, String title, String topicName, Long selectedOptionId,
                               Long correctOptionId, boolean correct, String explanation) { }

    public record AttemptSummary(Long id, Long testId, String testTitle, AttemptStatus status, Integer score,
                                 int totalQuestions, double percentage, Instant startedAt, Instant submittedAt) { }

    public record AttemptResult(AttemptSummary summary, List<AnswerReview> review) { }
}
