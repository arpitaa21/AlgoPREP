package com.placementportal.controller;

import com.placementportal.dto.StudyDtos.StatsResponse;
import com.placementportal.security.UserPrincipal;
import com.placementportal.service.StatsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/me")
    public StatsResponse me(@AuthenticationPrincipal UserPrincipal me) {
        return statsService.myStats(me.getId());
    }
}
