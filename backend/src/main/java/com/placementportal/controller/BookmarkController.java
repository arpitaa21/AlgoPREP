package com.placementportal.controller;

import com.placementportal.dto.QuestionDtos.QuestionSummary;
import com.placementportal.security.UserPrincipal;
import com.placementportal.service.BookmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public List<QuestionSummary> list(@AuthenticationPrincipal UserPrincipal me) {
        return bookmarkService.list(me.getId());
    }

    @PostMapping("/{questionId}")
    public ResponseEntity<Void> add(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long questionId) {
        bookmarkService.add(me.getId(), questionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long questionId) {
        bookmarkService.remove(me.getId(), questionId);
        return ResponseEntity.noContent().build();
    }
}
