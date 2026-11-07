package com.placementportal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class CompanyDtos {

    private CompanyDtos() { }

    public record CompanyRequest(@NotBlank @Size(max = 100) String name, @Size(max = 1000) String description) { }

    public record RoadmapStepRequest(
            @NotNull @Min(1) Integer stepOrder,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 1000) String description,
            Long topicId,
            @Size(max = 500) String resourceUrl) { }

    public record RoadmapStepResponse(Long id, int stepOrder, String title, String description,
                                      Long topicId, String topicName, String resourceUrl) { }

    public record CompanySummary(Long id, String name, String description, int stepCount) { }

    public record CompanyDetail(Long id, String name, String description, List<RoadmapStepResponse> roadmap) { }
}
