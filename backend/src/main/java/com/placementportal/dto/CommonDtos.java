package com.placementportal.dto;

import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class CommonDtos {

    private CommonDtos() { }

    /** Every error returned by the API has this shape, so the frontend needs just one error handler. */
    public record ErrorResponse(Instant timestamp, int status, String error, String message,
                                Map<String, String> fieldErrors) { }

    /** Our own page wrapper: a stable JSON contract instead of leaking Spring's internal Page structure. */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> from(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    public record MessageResponse(String message) { }
}
