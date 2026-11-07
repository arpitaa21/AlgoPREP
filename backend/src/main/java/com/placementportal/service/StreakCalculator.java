package com.placementportal.service;

import java.time.LocalDate;
import java.util.Set;

/** Counts consecutive active days. A streak is still "alive" if the last active day was yesterday. */
public final class StreakCalculator {

    private StreakCalculator() { }

    public static int currentStreak(Set<LocalDate> activeDays, LocalDate today) {
        LocalDate day;
        if (activeDays.contains(today)) {
            day = today;
        } else if (activeDays.contains(today.minusDays(1))) {
            day = today.minusDays(1);
        } else {
            return 0;
        }
        int streak = 0;
        while (activeDays.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}
