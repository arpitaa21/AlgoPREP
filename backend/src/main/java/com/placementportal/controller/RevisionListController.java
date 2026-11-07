package com.placementportal.controller;

import com.placementportal.dto.StudyDtos.*;
import com.placementportal.security.UserPrincipal;
import com.placementportal.service.RevisionListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/revision-lists")
public class RevisionListController {

    private final RevisionListService service;

    public RevisionListController(RevisionListService service) {
        this.service = service;
    }

    @GetMapping
    public List<RevisionListSummary> list(@AuthenticationPrincipal UserPrincipal me) {
        return service.list(me.getId());
    }

    @PostMapping
    public ResponseEntity<RevisionListSummary> create(@AuthenticationPrincipal UserPrincipal me,
                                                      @Valid @RequestBody RevisionListRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(me.getId(), request.name()));
    }

    @GetMapping("/{id}")
    public RevisionListDetail get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return service.get(me.getId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        service.delete(me.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/questions/{questionId}")
    public RevisionListDetail addQuestion(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                          @PathVariable Long questionId) {
        return service.addQuestion(me.getId(), id, questionId);
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    public RevisionListDetail removeQuestion(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                             @PathVariable Long questionId) {
        return service.removeQuestion(me.getId(), id, questionId);
    }
}
