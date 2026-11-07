package com.placementportal.service;

import com.placementportal.entity.Difficulty;

import java.util.Comparator;
import java.util.List;

/**
 * THE RULES of the recommender, as plain static methods with no Spring, no database.
 * That makes them trivial to unit-test and easy to explain in an interview.
 *
 *   accuracy      = correct / attempted
 *   confidence    = min(attempted, 10) / 10         (few answers => we are not sure yet)
 *   weaknessScore = (1 - accuracy) * confidence      (0 = strong ... 1 = very weak, with high confidence)
 *
 *   RULE 1  A topic is WEAK when attempted >= 3  AND  accuracy < 50 %.
 *   RULE 2  Weak topics are ranked by weaknessScore (highest first).
 *   RULE 3  Difficulty to practise next:  accuracy < 25 %  -> EASY,  otherwise MEDIUM.
 */
public final class WeaknessCalculator {

    public static final int MIN_ATTEMPTS = 3;
    public static final double WEAK_THRESHOLD = 0.50;
    public static final int CONFIDENCE_CAP = 10;
    public static final double EASY_THRESHOLD = 0.25;

    private WeaknessCalculator() { }

    public record TopicPerformance(Long topicId, String topicName, int attempted, int correct) { }

    public record WeakTopicScore(TopicPerformance performance, double accuracy, double weaknessScore) { }

    public static double accuracy(int attempted, int correct) {
        return attempted == 0 ? 0.0 : (double) correct / attempted;
    }

    public static double weaknessScore(int attempted, int correct) {
        double confidence = Math.min(attempted, CONFIDENCE_CAP) / (double) CONFIDENCE_CAP;
        return (1.0 - accuracy(attempted, correct)) * confidence;
    }

    public static boolean isWeak(int attempted, int correct) {
        return attempted >= MIN_ATTEMPTS && accuracy(attempted, correct) < WEAK_THRESHOLD;
    }

    /** Keeps only the weak topics and sorts them: weakest first (ties broken by topic name for a stable order). */
    public static List<WeakTopicScore> rankWeakTopics(List<TopicPerformance> all) {
        return all.stream()
                .filter(p -> isWeak(p.attempted(), p.correct()))
                .map(p -> new WeakTopicScore(p, accuracy(p.attempted(), p.correct()),
                        weaknessScore(p.attempted(), p.correct())))
                .sorted(Comparator.comparingDouble(WeakTopicScore::weaknessScore).reversed()
                        .thenComparing(w -> w.performance().topicName()))
                .toList();
    }

    public static Difficulty preferredDifficulty(double accuracy) {
        return accuracy < EASY_THRESHOLD ? Difficulty.EASY : Difficulty.MEDIUM;
    }
}
