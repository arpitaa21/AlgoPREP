package com.placementportal.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Running totals per (user, topic): how many questions were attempted and how many answered correctly.
 * The recommender reads ONLY this table, so it never has to re-scan every old answer.
 */
@Entity
@Table(name = "user_topic_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_stat_user_topic", columnNames = {"user_id", "topic_id"}))
public class UserTopicStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(nullable = false)
    private int attempted;

    @Column(nullable = false)
    private int correct;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserTopicStat() { }

    public UserTopicStat(User user, Topic topic) {
        this.user = user;
        this.topic = topic;
    }

    /** Record one more answered question. */
    public void record(boolean wasCorrect) {
        attempted++;
        if (wasCorrect) {
            correct++;
        }
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Topic getTopic() { return topic; }
    public int getAttempted() { return attempted; }
    public int getCorrect() { return correct; }
}
