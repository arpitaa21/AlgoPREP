package com.placementportal.entity;

import jakarta.persistence.*;

/** One answer choice of an MCQ. (Named QuestionOption because OPTION is an SQL keyword.) */
@Entity
@Table(name = "question_options")
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    protected QuestionOption() { }

    public QuestionOption(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    void setQuestion(Question question) { this.question = question; }   // package-private: only Question.addOption calls it
    /** Edit in place so the option keeps its id (old test answers point at it). */
    public void update(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
}
