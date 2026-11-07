package com.placementportal.service;

import com.placementportal.dto.QuestionDtos.OptionResponse;
import com.placementportal.dto.TestDtos.*;
import com.placementportal.entity.*;
import com.placementportal.exception.BadRequestException;
import com.placementportal.exception.ConflictException;
import com.placementportal.exception.ResourceNotFoundException;
import com.placementportal.exception.TestExpiredException;
import com.placementportal.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The timed test engine.
 *   start  -> server creates an attempt and stores the DEADLINE (startedAt + duration)
 *   submit -> server compares "now" with the stored deadline, scores the answers itself,
 *             and updates the weak-topic statistics.
 * The browser timer is only a display; the server's clock is the only one that counts.
 */
@Service
public class TestService {

    private final TestSetRepository testRepository;
    private final TestAttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserTopicStatRepository statRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final long graceSeconds;

    public TestService(TestSetRepository testRepository, TestAttemptRepository attemptRepository,
                       QuestionRepository questionRepository, AnswerRepository answerRepository,
                       UserTopicStatRepository statRepository, UserRepository userRepository,
                       Clock clock, @Value("${app.test.grace-seconds}") long graceSeconds) {
        this.testRepository = testRepository;
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.statRepository = statRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.graceSeconds = graceSeconds;
    }

    @Transactional(readOnly = true)
    public List<TestSummary> listActiveTests() {
        return testRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(t -> new TestSummary(t.getId(), t.getTitle(), t.getDescription(),
                        t.getDurationMinutes(), t.getQuestions().size()))
                .toList();
    }

    // =====================================================================
    //  START
    // =====================================================================
    @Transactional
    public StartTestResponse start(Long userId, Long testId) {
        TestSet test = testRepository.findById(testId)
                .filter(TestSet::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
        Instant now = clock.instant();

        // Resume support: refreshing the browser must not start a second attempt (and a fresh timer!).
        TestAttempt attempt = attemptRepository
                .findFirstByUserIdAndTestSetIdAndStatus(userId, testId, AttemptStatus.IN_PROGRESS).orElse(null);
        if (attempt != null && isPastDeadlineWithGrace(attempt, now)) {
            attempt.setStatus(AttemptStatus.EXPIRED);       // abandoned attempt: close it and start fresh
            attempt = null;
        }

        List<Question> questions = questionRepository.findByTestId(testId);
        if (attempt == null) {
            if (questions.isEmpty()) {
                throw new BadRequestException("This test has no questions yet");
            }
            Instant deadline = now.plus(Duration.ofMinutes(test.getDurationMinutes()));
            attempt = attemptRepository.save(new TestAttempt(
                    userRepository.getReferenceById(userId), test, now, deadline, questions.size()));
        }

        long secondsRemaining = Math.max(0, Duration.between(now, attempt.getDeadline()).getSeconds());
        List<TestQuestion> dtos = questions.stream().map(this::toTestQuestion).toList();
        return new StartTestResponse(attempt.getId(), test.getId(), test.getTitle(), attempt.getStartedAt(),
                attempt.getDeadline(), test.getDurationMinutes(), secondsRemaining, dtos);
    }

    // =====================================================================
    //  SUBMIT
    // =====================================================================
    /**
     * noRollbackFor: when the student is too late we mark the attempt EXPIRED and then throw.
     * Normally an exception rolls the transaction back and the EXPIRED status would be lost;
     * this attribute makes Spring commit that change anyway.
     */
    @Transactional(noRollbackFor = TestExpiredException.class)
    public AttemptResult submit(Long userId, Long attemptId, SubmitRequest request) {
        TestAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ConflictException("This attempt has already been closed");
        }

        // ---- 1. SERVER-SIDE TIMER VALIDATION ----
        Instant now = clock.instant();
        if (isPastDeadlineWithGrace(attempt, now)) {
            attempt.setStatus(AttemptStatus.EXPIRED);
            attempt.setSubmittedAt(now);
            throw new TestExpiredException("Time is over: the submission arrived after the deadline");
        }

        // ---- 2. Index what the student sent: questionId -> optionId (null = skipped) ----
        List<Question> questions = questionRepository.findByTestId(attempt.getTestSet().getId());
        Set<Long> validQuestionIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
        Map<Long, Long> chosen = new HashMap<>();
        for (AnswerRequest a : request.answers()) {
            if (!validQuestionIds.contains(a.questionId())) {
                throw new BadRequestException("Question " + a.questionId() + " is not part of this test");
            }
            chosen.put(a.questionId(), a.optionId());
        }

        // ---- 3. AUTO-SCORING (the client never tells us whether an answer is right) ----
        Map<Long, UserTopicStat> stats = statRepository.findByUserIdWithTopic(userId).stream()
                .collect(Collectors.toMap(s -> s.getTopic().getId(), s -> s));
        List<Answer> answers = new ArrayList<>();
        int score = 0;
        for (Question q : questions) {
            Long optionId = chosen.get(q.getId());
            QuestionOption selected = null;
            if (optionId != null) {
                selected = q.getOptions().stream().filter(o -> o.getId().equals(optionId)).findFirst()
                        .orElseThrow(() -> new BadRequestException(
                                "Option " + optionId + " does not belong to question " + q.getId()));
            }
            boolean correct = selected != null && selected.isCorrect();
            if (correct) {
                score++;
            }
            answers.add(new Answer(attempt, q, selected, correct));

            // ---- 4. UPDATE WEAK-TOPIC STATS (a skipped question counts as attempted-but-wrong) ----
            Topic topic = q.getTopic();
            UserTopicStat stat = stats.computeIfAbsent(topic.getId(),
                    id -> new UserTopicStat(attempt.getUser(), topic));
            stat.record(correct);
        }
        statRepository.saveAll(stats.values());
        answerRepository.saveAll(answers);

        attempt.setScore(score);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(now);

        return new AttemptResult(toSummary(attempt), answers.stream().map(TestService::toReview).toList());
    }

    // =====================================================================
    //  READ
    // =====================================================================
    @Transactional(readOnly = true)
    public List<AttemptSummary> history(Long userId) {
        return attemptRepository.findHistory(userId).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public AttemptResult getAttempt(Long userId, Long attemptId) {
        TestAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));
        // The review (it contains the right answers!) is only released once the attempt is closed.
        List<AnswerReview> review = attempt.getStatus() == AttemptStatus.SUBMITTED
                ? answerRepository.findByAttemptWithDetails(attemptId).stream().map(TestService::toReview).toList()
                : List.of();
        return new AttemptResult(toSummary(attempt), review);
    }

    // ---------------------------------------------------------------------

    private boolean isPastDeadlineWithGrace(TestAttempt attempt, Instant now) {
        return now.isAfter(attempt.getDeadline().plusSeconds(graceSeconds));
    }

    private TestQuestion toTestQuestion(Question q) {
        List<OptionResponse> options = q.getOptions().stream()
                .map(o -> new OptionResponse(o.getId(), o.getText())).toList();   // no "correct" flag!
        return new TestQuestion(q.getId(), q.getTitle(), q.getDescription(), q.getTopic().getName(), options);
    }

    private AttemptSummary toSummary(TestAttempt a) {
        double pct = (a.getScore() == null || a.getTotalQuestions() == 0)
                ? 0 : Math.round(a.getScore() * 10000.0 / a.getTotalQuestions()) / 100.0;
        return new AttemptSummary(a.getId(), a.getTestSet().getId(), a.getTestSet().getTitle(), a.getStatus(),
                a.getScore(), a.getTotalQuestions(), pct, a.getStartedAt(), a.getSubmittedAt());
    }

    private static AnswerReview toReview(Answer a) {
        Question q = a.getQuestion();
        Long correctId = q.getOptions().stream().filter(QuestionOption::isCorrect)
                .findFirst().map(QuestionOption::getId).orElse(null);
        Long selectedId = a.getSelectedOption() == null ? null : a.getSelectedOption().getId();
        return new AnswerReview(q.getId(), q.getTitle(), q.getTopic().getName(), selectedId, correctId,
                a.isCorrect(), q.getExplanation());
    }
}
