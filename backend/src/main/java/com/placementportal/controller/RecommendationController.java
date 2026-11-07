package com.placementportal.controller;

import com.placementportal.dto.RecommendationDtos.RecommendedQuestion;
import com.placementportal.dto.RecommendationDtos.WeakTopic;
import com.placementportal.security.UserPrincipal;
import com.placementportal.service.RecommenderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommenderService recommenderService;

    public RecommendationController(RecommenderService recommenderService) {
        this.recommenderService = recommenderService;
    }

    @GetMapping
    public List<RecommendedQuestion> recommend(@AuthenticationPrincipal UserPrincipal me,
                                               @RequestParam(defaultValue = "6") int limit) {
        return recommenderService.recommend(me.getId(), limit);
    }

    @GetMapping("/weak-topics")
    public List<WeakTopic> weakTopics(@AuthenticationPrincipal UserPrincipal me) {
        return recommenderService.weakTopics(me.getId());
    }
}
