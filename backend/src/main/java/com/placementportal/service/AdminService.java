package com.placementportal.service;

import com.placementportal.dto.AdminDtos.DashboardResponse;
import com.placementportal.dto.AdminDtos.TopicCount;
import com.placementportal.dto.AdminDtos.UserSummary;
import com.placementportal.entity.AttemptStatus;
import com.placementportal.entity.Difficulty;
import com.placementportal.entity.QuestionType;
import com.placementportal.entity.RoleName;
import com.placementportal.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final TestSetRepository testRepository;
    private final TestAttemptRepository attemptRepository;
    private final CodingSubmissionRepository submissionRepository;

    public AdminService(UserRepository userRepository, QuestionRepository questionRepository,
                        TestSetRepository testRepository, TestAttemptRepository attemptRepository,
                        CodingSubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
        this.attemptRepository = attemptRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Map<Difficulty, Long> byDifficulty = new EnumMap<>(Difficulty.class);
        for (Difficulty d : Difficulty.values()) {
            byDifficulty.put(d, 0L);
        }
        questionRepository.countByDifficulty().forEach(r -> byDifficulty.put(r.getDifficulty(), r.getCnt()));

        Map<QuestionType, Long> byType = new EnumMap<>(QuestionType.class);
        for (QuestionType t : QuestionType.values()) {
            byType.put(t, questionRepository.countByType(t));
        }

        List<TopicCount> byTopic = questionRepository.countByTopic().stream()
                .map(r -> new TopicCount(r.getTopic(), r.getTotal())).toList();

        Double avg = attemptRepository.averagePercentageAll(AttemptStatus.SUBMITTED);
        return new DashboardResponse(userRepository.count(), userRepository.countByRoleName(RoleName.STUDENT),
                questionRepository.count(), testRepository.count(), attemptRepository.count(),
                submissionRepository.count(), avg == null ? 0 : Math.round(avg * 100.0) / 100.0,
                byDifficulty, byType, byTopic);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> users() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(u -> new UserSummary(u.getId(), u.getName(), u.getEmail(),
                        u.getRoles().stream().map(r -> r.getName().name()).sorted().toList(),
                        u.isEnabled(), u.getCreatedAt()))
                .toList();
    }
}
