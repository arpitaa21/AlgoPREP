package com.placementportal.controller;

import com.placementportal.dto.TestDtos.*;
import com.placementportal.security.UserPrincipal;
import com.placementportal.service.TestService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/tests")
    public List<TestSummary> tests() {
        return testService.listActiveTests();
    }

    @PostMapping("/tests/{testId}/start")
    public StartTestResponse start(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long testId) {
        return testService.start(me.getId(), testId);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public AttemptResult submit(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long attemptId,
                                @Valid @RequestBody SubmitRequest request) {
        return testService.submit(me.getId(), attemptId, request);
    }

    @GetMapping("/attempts")
    public List<AttemptSummary> history(@AuthenticationPrincipal UserPrincipal me) {
        return testService.history(me.getId());
    }

    @GetMapping("/attempts/{attemptId}")
    public AttemptResult attempt(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long attemptId) {
        return testService.getAttempt(me.getId(), attemptId);
    }
}
