package com.placementportal.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Bookmark is a join ENTITY (not a plain @ManyToMany join table) because the relationship itself
 * carries data (createdAt). The unique constraint stops duplicates even under concurrent clicks.
 */
@Entity
@Table(name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(name = "uk_bookmark_user_question", columnNames = {"user_id", "question_id"}))
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Bookmark() { }

    public Bookmark(User user, Question question) {
        this.user = user;
        this.question = question;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Question getQuestion() { return question; }
    public Instant getCreatedAt() { return createdAt; }
}
