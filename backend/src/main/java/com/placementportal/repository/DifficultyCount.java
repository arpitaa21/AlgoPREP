package com.placementportal.repository;

import com.placementportal.entity.Difficulty;

/** Interface projection: Spring maps the aliases in the JPQL (difficulty, cnt) onto these getters. */
public interface DifficultyCount {
    Difficulty getDifficulty();
    Long getCnt();
}
