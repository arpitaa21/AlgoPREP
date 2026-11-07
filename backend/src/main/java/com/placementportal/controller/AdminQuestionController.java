package com.placementportal.controller;

import com.placementportal.dto.CommonDtos.PageResponse;
import com.placementportal.dto.QuestionDtos.AdminQuestionResponse;
import com.placementportal.dto.QuestionDtos.QuestionRequest;
import com.placementportal.entity.Difficulty;
import com.placementportal.entity.QuestionType;
import com.placementportal.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

    private final QuestionService questionService;

    public AdminQuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public PageResponse<AdminQuestionResponse> list(@RequestParam(required = false) Long topicId,
                                                    @RequestParam(required = false) Difficulty difficulty,
                                                    @RequestParam(required = false) QuestionType type,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return questionService.adminSearch(topicId, difficulty, type, keyword, page, size);
    }

    @GetMapping("/{id}")
    public AdminQuestionResponse get(@PathVariable Long id) {
        return questionService.adminGet(id);
    }

    @PostMapping
    public ResponseEntity<AdminQuestionResponse> create(@Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(request));
    }

    @PutMapping("/{id}")
    public AdminQuestionResponse update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return questionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
