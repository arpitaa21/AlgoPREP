package com.placementportal.service;

import com.placementportal.entity.Difficulty;
import com.placementportal.service.WeaknessCalculator.TopicPerformance;
import com.placementportal.service.WeaknessCalculator.WeakTopicScore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests: no Spring, no database - which is exactly why the rules live in a plain class. */
class WeaknessCalculatorTest {

    @Test
    void accuracy_isCorrectDividedByAttempted() {
        assertEquals(0.4, WeaknessCalculator.accuracy(10, 4), 1e-9);
    }

    @Test
    void accuracy_isZeroWhenNothingAttempted() {
        assertEquals(0.0, WeaknessCalculator.accuracy(0, 0), 1e-9);
    }

    @Test
    void topic_belowFiftyPercentWithEnoughAttempts_isWeak() {
        assertTrue(WeaknessCalculator.isWeak(10, 4));      // 40 %
        assertTrue(WeaknessCalculator.isWeak(3, 1));       // 33 %, exactly the minimum attempts
    }

    @Test
    void topic_atExactlyFiftyPercent_isNotWeak() {
        assertFalse(WeaknessCalculator.isWeak(10, 5));     // the rule is "strictly below 50 %"
    }

    @Test
    void topic_withTooFewAttempts_isNotWeakEvenIfAllWrong() {
        assertFalse(WeaknessCalculator.isWeak(2, 0));      // not enough evidence yet
    }

    @Test
    void weaknessScore_growsWithConfidence() {
        // both 0 % accuracy, but 10 attempts are more convincing than 3
        assertEquals(1.0, WeaknessCalculator.weaknessScore(10, 0), 1e-9);
        assertEquals(0.3, WeaknessCalculator.weaknessScore(3, 0), 1e-9);
    }

    @Test
    void weaknessScore_confidenceIsCappedAtTenAttempts() {
        assertEquals(WeaknessCalculator.weaknessScore(10, 2), WeaknessCalculator.weaknessScore(50, 10), 1e-9);
    }

    @Test
    void rankWeakTopics_filtersAndSortsWeakestFirst() {
        List<TopicPerformance> input = List.of(
                new TopicPerformance(1L, "Arrays", 10, 2),      // 20 % -> score 0.8
                new TopicPerformance(2L, "Graphs", 4, 0),       //  0 % -> score 0.4
                new TopicPerformance(3L, "DBMS", 10, 9),        // strong -> excluded
                new TopicPerformance(4L, "OS", 2, 0));          // too few attempts -> excluded

        List<WeakTopicScore> ranked = WeaknessCalculator.rankWeakTopics(input);

        assertEquals(2, ranked.size());
        assertEquals("Arrays", ranked.get(0).performance().topicName());
        assertEquals(0.8, ranked.get(0).weaknessScore(), 1e-9);
        assertEquals("Graphs", ranked.get(1).performance().topicName());
    }

    @Test
    void preferredDifficulty_veryWeakGetsEasy_otherwiseMedium() {
        assertEquals(Difficulty.EASY, WeaknessCalculator.preferredDifficulty(0.10));
        assertEquals(Difficulty.MEDIUM, WeaknessCalculator.preferredDifficulty(0.25));
        assertEquals(Difficulty.MEDIUM, WeaknessCalculator.preferredDifficulty(0.45));
    }
}
