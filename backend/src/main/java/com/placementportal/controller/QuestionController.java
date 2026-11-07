package com.placementportal.controller;

import com.placementportal.dto.CommonDtos.PageResponse;
import com.placementportal.dto.QuestionDtos.*;
import com.placementportal.entity.Difficulty;
import com.placementportal.entity.QuestionType;
import com.placementportal.security.UserPrincipal;
import com.placementportal.service.PracticeService;
import com.placementportal.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final PracticeService practiceService;

    public QuestionController(QuestionService questionService, PracticeService practiceService) {
        this.questionService = questionService;
        this.practiceService = practiceService;
    }

    /** GET /api/questions?topicId=1&difficulty=EASY&type=DSA&keyword=array&page=0&size=10 */
    @GetMapping
    public PageResponse<QuestionSummary> list(@AuthenticationPrincipal UserPrincipal me,
                                              @RequestParam(required = false) Long topicId,
                                              @RequestParam(required = false) Difficulty difficulty,
                                              @RequestParam(required = false) QuestionType type,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return questionService.search(me.getId(), topicId, difficulty, type, keyword, page, size);
    }

    @GetMapping("/{id}")
    public QuestionDetail get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return questionService.getDetail(me.getId(), id);
    }

    /** Answer one MCQ outside a test (targeted practice). */
    @PostMapping("/{id}/practice")
    public PracticeResult practice(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                   @Valid @RequestBody PracticeRequest request) {
        return practiceService.check(me.getId(), id, request.optionId());
    }
}
