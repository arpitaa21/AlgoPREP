package com.placementportal.entity;

import jakarta.persistence.*;

/** The student's answer to one question inside one attempt. selectedOption is null when skipped. */
@Entity
@Table(name = "answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_answer_attempt_question", columnNames = {"attempt_id", "question_id"}))
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuestionOption selectedOption;

    // Stored (denormalised) so the result of an old attempt never changes if an admin edits the options later.
    @Column(nullable = false)
    private boolean correct;

    protected Answer() { }

    public Answer(TestAttempt attempt, Question question, QuestionOption selectedOption, boolean correct) {
        this.attempt = attempt;
        this.question = question;
        this.selectedOption = selectedOption;
        this.correct = correct;
    }

    public Long getId() { return id; }
    public TestAttempt getAttempt() { return attempt; }
    public Question getQuestion() { return question; }
    public QuestionOption getSelectedOption() { return selectedOption; }
    public boolean isCorrect() { return correct; }
}
