package com.placementportal.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A student's named collection of questions to revise (e.g. "Before Amazon OA"). */
@Entity
@Table(name = "revision_lists",
        uniqueConstraints = @UniqueConstraint(name = "uk_revision_user_name", columnNames = {"user_id", "name"}))
public class RevisionList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // MANY-TO-MANY with a pure join table (no extra columns needed) -> plain @ManyToMany is enough.
    @ManyToMany
    @JoinTable(name = "revision_list_questions",
            joinColumns = @JoinColumn(name = "revision_list_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id"))
    @OrderBy("id ASC")
    private List<Question> questions = new ArrayList<>();

    protected RevisionList() { }

    public RevisionList(User user, String name) {
        this.user = user;
        this.name = name;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Question> getQuestions() { return questions; }
}
