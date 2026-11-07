package com.placementportal.service;

import com.placementportal.dto.StudyDtos.StatsResponse;
import com.placementportal.dto.StudyDtos.TopicProgress;
import com.placementportal.entity.AttemptStatus;
import com.placementportal.entity.Difficulty;
import com.placementportal.entity.QuestionType;
import com.placementportal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/** Progress statistics for the student dashboard (analytics queries live in the repositories). */
@Service
public class StatsService {

    private final QuestionRepository questionRepository;
    private final SolvedQuestionRepository solvedRepository;
    private final TestAttemptRepository attemptRepository;
    private final CodingSubmissionRepository submissionRepository;
    private final Clock clock;

    public StatsService(QuestionRepository questionRepository, SolvedQuestionRepository solvedRepository,
                        TestAttemptRepository attemptRepository, CodingSubmissionRepository submissionRepository,
                        Clock clock) {
        this.questionRepository = questionRepository;
        this.solvedRepository = solvedRepository;
        this.attemptRepository = attemptRepository;
        this.submissionRepository = submissionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StatsResponse myStats(Long userId) {
        Map<Difficulty, Long> totalByDifficulty = toMap(questionRepository.countByDifficultyForType(QuestionType.DSA));
        Map<Difficulty, Long> solvedByDifficulty = toMap(solvedRepository.countSolvedByDifficulty(userId, QuestionType.DSA));
        long totalDsa = totalByDifficulty.values().stream().mapToLong(Long::longValue).sum();
        long solved = solvedByDifficulty.values().stream().mapToLong(Long::longValue).sum();

        // Native SQL result: [topicId, topicName, total, solved] per row
        List<TopicProgress> topics = new ArrayList<>();
        for (Object[] row : solvedRepository.topicProgress(userId)) {
            long total = ((Number) row[2]).longValue();
            long done = ((Number) row[3]).longValue();
            double pct = total == 0 ? 0 : Math.round(done * 10000.0 / total) / 100.0;
            topics.add(new TopicProgress(((Number) row[0]).longValue(), (String) row[1], total, done, pct));
        }

        Double avg = attemptRepository.averagePercentage(userId, AttemptStatus.SUBMITTED);
        long testsTaken = attemptRepository.countByUserIdAndStatus(userId, AttemptStatus.SUBMITTED);

        // Submissions per day (UTC) for the last 30 days + current streak, from one cheap query.
        LocalDate today = LocalDate.now(clock);
        Instant since = today.minusDays(365).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<LocalDate, Long> perDay = submissionRepository.findSubmissionTimesSince(userId, since).stream()
                .collect(Collectors.groupingBy(i -> LocalDate.ofInstant(i, ZoneOffset.UTC), Collectors.counting()));
        int streak = StreakCalculator.currentStreak(perDay.keySet(), today);

        Map<String, Long> last30 = new TreeMap<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            last30.put(d.toString(), perDay.getOrDefault(d, 0L));
        }

        return new StatsResponse(totalDsa, solved, solvedByDifficulty, totalByDifficulty, topics, testsTaken,
                avg == null ? 0 : Math.round(avg * 100.0) / 100.0, last30, streak);
    }

    /** Fills in zero for difficulties that have no rows, so the JSON always has EASY, MEDIUM and HARD. */
    private static Map<Difficulty, Long> toMap(List<DifficultyCount> rows) {
        Map<Difficulty, Long> map = new EnumMap<>(Difficulty.class);
        for (Difficulty d : Difficulty.values()) {
            map.put(d, 0L);
        }
        rows.forEach(r -> map.put(r.getDifficulty(), r.getCnt()));
        return map;
    }
}
