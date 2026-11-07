package com.placementportal.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One student's try at one test. The server stores startedAt and the computed deadline,
 * so the timer can never be cheated from the browser.
 */
@Entity
@Table(name = "test_attempts", indexes = @Index(name = "idx_attempts_user", columnList = "user_id"))
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_set_id")
    private TestSet testSet;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant deadline;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    private Integer score;                     // null until submitted

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    protected TestAttempt() { }

    public TestAttempt(User user, TestSet testSet, Instant startedAt, Instant deadline, int totalQuestions) {
        this.user = user;
        this.testSet = testSet;
        this.startedAt = startedAt;
        this.deadline = deadline;
        this.totalQuestions = totalQuestions;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public TestSet getTestSet() { return testSet; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getDeadline() { return deadline; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public int getTotalQuestions() { return totalQuestions; }
    public AttemptStatus getStatus() { return status; }
    public void setStatus(AttemptStatus status) { this.status = status; }
}
