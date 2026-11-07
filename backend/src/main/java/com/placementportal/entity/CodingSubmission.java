package com.placementportal.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** A log entry: "I tried / solved this DSA question, in this language, in N minutes." */
@Entity
@Table(name = "coding_submissions",
        indexes = @Index(name = "idx_submissions_user_time", columnList = "user_id, submitted_at"))
public class CodingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false, length = 30)
    private String language;

    @Column(columnDefinition = "TEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status;

    @Column(name = "time_taken_minutes")
    private Integer timeTakenMinutes;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected CodingSubmission() { }

    public CodingSubmission(User user, Question question, String language, String code,
                            SubmissionStatus status, Integer timeTakenMinutes, Instant submittedAt) {
        this.user = user;
        this.question = question;
        this.language = language;
        this.code = code;
        this.status = status;
        this.timeTakenMinutes = timeTakenMinutes;
        this.submittedAt = submittedAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Question getQuestion() { return question; }
    public String getLanguage() { return language; }
    public String getCode() { return code; }
    public SubmissionStatus getStatus() { return status; }
    public Integer getTimeTakenMinutes() { return timeTakenMinutes; }
    public Instant getSubmittedAt() { return submittedAt; }
}
