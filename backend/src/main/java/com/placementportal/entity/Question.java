package com.placementportal.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions",
        indexes = @Index(name = "idx_questions_topic_difficulty", columnList = "topic_id, difficulty"))
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuestionType type;

    // Many questions -> one topic. LAZY so listing questions does not always drag topics in.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    // Link to the real problem on LeetCode / GeeksforGeeks etc. (we do not host a code judge)
    @Column(name = "external_link", length = 500)
    private String externalLink;

    // Approach notes for DSA, or the explanation of the right answer for MCQ.
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // One question -> many options. Options cannot exist without their question, hence
    // cascade = ALL (save/delete with the parent) and orphanRemoval (removing from list deletes the row).
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<QuestionOption> options = new ArrayList<>();

    protected Question() { }

    public void addOption(QuestionOption option) {
        option.setQuestion(this);              // keep BOTH sides of the relationship in sync
        options.add(option);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public QuestionType getType() { return type; }
    public void setType(QuestionType type) { this.type = type; }
    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }
    public String getExternalLink() { return externalLink; }
    public void setExternalLink(String externalLink) { this.externalLink = externalLink; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public Instant getCreatedAt() { return createdAt; }
    public List<QuestionOption> getOptions() { return options; }

    public static Question create(String title, String description, Difficulty difficulty,
                                  QuestionType type, Topic topic) {
        Question q = new Question();
        q.title = title;
        q.description = description;
        q.difficulty = difficulty;
        q.type = type;
        q.topic = topic;
        return q;
    }
}
