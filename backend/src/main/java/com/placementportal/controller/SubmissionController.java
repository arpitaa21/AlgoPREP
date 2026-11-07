package com.placementportal.controller;

import com.placementportal.dto.StudyDtos.SubmissionRequest;
import com.placementportal.dto.StudyDtos.SubmissionResponse;
import com.placementportal.security.UserPrincipal;
import com.placementportal.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<SubmissionResponse> record(@AuthenticationPrincipal UserPrincipal me,
                                                     @Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionService.record(me.getId(), request));
    }

    @GetMapping
    public List<SubmissionResponse> list(@AuthenticationPrincipal UserPrincipal me,
                                         @RequestParam(required = false) Long questionId) {
        return submissionService.list(me.getId(), questionId);
    }
}
