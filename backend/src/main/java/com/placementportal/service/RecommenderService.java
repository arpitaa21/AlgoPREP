package com.placementportal.service;

import com.placementportal.dto.RecommendationDtos.RecommendedQuestion;
import com.placementportal.dto.RecommendationDtos.WeakTopic;
import com.placementportal.entity.Difficulty;
import com.placementportal.entity.Question;
import com.placementportal.repository.AnswerRepository;
import com.placementportal.repository.QuestionRepository;
import com.placementportal.repository.SolvedQuestionRepository;
import com.placementportal.repository.UserTopicStatRepository;
import com.placementportal.service.WeaknessCalculator.TopicPerformance;
import com.placementportal.service.WeaknessCalculator.WeakTopicScore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Rule-based recommender: reads the per-topic statistics, finds weak topics with WeaknessCalculator,
 * and suggests unsolved questions from those topics. No machine learning - every recommendation can be
 * explained with one sentence (the "reason" field).
 */
@Service
public class RecommenderService {

    static final int MAX_TOPICS = 3;

    private final UserTopicStatRepository statRepository;
    private final QuestionRepository questionRepository;
    private final SolvedQuestionRepository solvedRepository;
    private final AnswerRepository answerRepository;

    public RecommenderService(UserTopicStatRepository statRepository, QuestionRepository questionRepository,
                              SolvedQuestionRepository solvedRepository, AnswerRepository answerRepository) {
        this.statRepository = statRepository;
        this.questionRepository = questionRepository;
        this.solvedRepository = solvedRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional(readOnly = true)
    public List<WeakTopic> weakTopics(Long userId) {
        return rank(userId).stream().map(w -> new WeakTopic(
                w.performance().topicId(), w.performance().topicName(),
                w.performance().attempted(), w.performance().correct(),
                round2(w.accuracy()), round2(w.weaknessScore()))).toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedQuestion> recommend(Long userId, int limit) {
        int max = Math.min(Math.max(limit, 1), 20);
        List<WeakTopicScore> topics = rank(userId).stream().limit(MAX_TOPICS).toList();
        if (topics.isEmpty()) {
            return List.of();
        }

        // Do not recommend what the student already solved or already answered correctly in a test.
        Set<Long> exclude = new HashSet<>(solvedRepository.findAllSolvedIds(userId));
        exclude.addAll(answerRepository.findCorrectQuestionIds(userId));

        int quotaPerTopic = (int) Math.ceil((double) max / topics.size());
        List<RecommendedQuestion> result = new ArrayList<>();
        for (WeakTopicScore w : topics) {
            Difficulty preferred = WeaknessCalculator.preferredDifficulty(w.accuracy());
            TopicPerformance p = w.performance();
            String reason = String.format("You answered %d of %d correctly in %s (%.0f%% accuracy)",
                    p.correct(), p.attempted(), p.topicName(), w.accuracy() * 100);

            questionRepository.findByTopicIdWithTopic(p.topicId()).stream()
                    .filter(q -> !exclude.contains(q.getId()))
                    // closest difficulty to the preferred one first, then by id for a stable order
                    .sorted(Comparator.comparingInt((Question q) -> Math.abs(q.getDifficulty().ordinal() - preferred.ordinal()))
                            .thenComparing(Question::getId))
                    .limit(quotaPerTopic)
                    .forEach(q -> result.add(new RecommendedQuestion(q.getId(), q.getTitle(), q.getDifficulty(),
                            q.getType(), p.topicId(), p.topicName(), reason)));
        }
        return result.stream().limit(max).toList();
    }

    private List<WeakTopicScore> rank(Long userId) {
        List<TopicPerformance> performances = statRepository.findByUserIdWithTopic(userId).stream()
                .map(s -> new TopicPerformance(s.getTopic().getId(), s.getTopic().getName(),
                        s.getAttempted(), s.getCorrect()))
                .toList();
        return WeaknessCalculator.rankWeakTopics(performances);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
