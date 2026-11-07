package com.placementportal.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** "User X has solved question Y" - one row per pair, created the first time it is solved. */
@Entity
@Table(name = "solved_questions",
        uniqueConstraints = @UniqueConstraint(name = "uk_solved_user_question", columnNames = {"user_id", "question_id"}))
public class SolvedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "solved_at", nullable = false)
    private Instant solvedAt = Instant.now();

    protected SolvedQuestion() { }

    public SolvedQuestion(User user, Question question) {
        this.user = user;
        this.question = question;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Question getQuestion() { return question; }
    public Instant getSolvedAt() { return solvedAt; }
}
