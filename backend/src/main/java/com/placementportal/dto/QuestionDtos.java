package com.placementportal.dto;

import com.placementportal.entity.Difficulty;
import com.placementportal.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class QuestionDtos {

    private QuestionDtos() { }

    // ---------- What STUDENTS see (note: no "correct" flag on options!) ----------
    public record OptionResponse(Long id, String text) { }

    public record QuestionSummary(Long id, String title, Difficulty difficulty, QuestionType type,
                                  Long topicId, String topicName, boolean bookmarked, boolean solved) { }

    public record QuestionDetail(Long id, String title, String description, Difficulty difficulty,
                                 QuestionType type, Long topicId, String topicName, String externalLink,
                                 String explanation, boolean bookmarked, boolean solved,
                                 List<OptionResponse> options) { }

    // ---------- What ADMINS see / send ----------
    public record AdminOptionResponse(Long id, String text, boolean correct) { }

    public record AdminQuestionResponse(Long id, String title, String description, Difficulty difficulty,
                                        QuestionType type, Long topicId, String topicName, String externalLink,
                                        String explanation, List<AdminOptionResponse> options) { }

    public record OptionRequest(@NotBlank @Size(max = 500) String text, boolean correct) { }

    public record QuestionRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String description,
            @NotNull Difficulty difficulty,
            @NotNull QuestionType type,
            @NotNull Long topicId,
            @Size(max = 500) String externalLink,
            String explanation,
            @Valid List<OptionRequest> options) { }

    // ---------- Practice an MCQ outside a test ----------
    public record PracticeRequest(@NotNull Long optionId) { }

    public record PracticeResult(boolean correct, Long correctOptionId, String explanation) { }

    // ---------- Topics ----------
    public record TopicRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) { }

    public record TopicResponse(Long id, String name, String description) { }
}
