package com.placementportal.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

    @Test
    void noActivity_isZero() {
        assertEquals(0, StreakCalculator.currentStreak(Set.of(), TODAY));
    }

    @Test
    void threeConsecutiveDaysEndingToday_isThree() {
        Set<LocalDate> days = Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));
        assertEquals(3, StreakCalculator.currentStreak(days, TODAY));
    }

    @Test
    void streakIsStillAliveIfLastActivityWasYesterday() {
        Set<LocalDate> days = Set.of(TODAY.minusDays(1), TODAY.minusDays(2));
        assertEquals(2, StreakCalculator.currentStreak(days, TODAY));
    }

    @Test
    void gapBreaksTheStreak() {
        Set<LocalDate> days = Set.of(TODAY, TODAY.minusDays(2), TODAY.minusDays(3));
        assertEquals(1, StreakCalculator.currentStreak(days, TODAY));
    }

    @Test
    void lastActivityTwoDaysAgo_isZero() {
        assertEquals(0, StreakCalculator.currentStreak(Set.of(TODAY.minusDays(2)), TODAY));
    }
}
