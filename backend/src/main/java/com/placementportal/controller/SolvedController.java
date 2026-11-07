package com.placementportal.controller;

import com.placementportal.dto.QuestionDtos.QuestionSummary;
import com.placementportal.security.UserPrincipal;
import com.placementportal.service.SolvedService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solved")
public class SolvedController {

    private final SolvedService solvedService;

    public SolvedController(SolvedService solvedService) {
        this.solvedService = solvedService;
    }

    @GetMapping
    public List<QuestionSummary> list(@AuthenticationPrincipal UserPrincipal me) {
        return solvedService.list(me.getId());
    }

    @PostMapping("/{questionId}")
    public ResponseEntity<Void> mark(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long questionId) {
        solvedService.markSolved(me.getId(), questionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> unmark(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long questionId) {
        solvedService.unmark(me.getId(), questionId);
        return ResponseEntity.noContent().build();
    }
}
