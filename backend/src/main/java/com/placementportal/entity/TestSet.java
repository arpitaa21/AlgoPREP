package com.placementportal.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A timed test = an ordered collection of MCQ questions. Named TestSet to avoid clashing with JUnit's @Test. */
@Entity
@Table(name = "test_sets")
public class TestSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    // Inactive tests are hidden from students (admin can prepare a test before publishing it).
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // MANY-TO-MANY: one question can appear in many tests and a test has many questions.
    // The join table test_set_questions holds the pairs.
    @ManyToMany
    @JoinTable(name = "test_set_questions",
            joinColumns = @JoinColumn(name = "test_set_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id"))
    @OrderBy("id ASC")
    private List<Question> questions = new ArrayList<>();

    protected TestSet() { }

    public TestSet(String title, String description, int durationMinutes, boolean active) {
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<Question> getQuestions() { return questions; }
}
