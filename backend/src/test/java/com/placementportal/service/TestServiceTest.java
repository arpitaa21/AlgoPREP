package com.placementportal.service;

import com.placementportal.dto.TestDtos.*;
import com.placementportal.entity.*;
import com.placementportal.exception.BadRequestException;
import com.placementportal.exception.ConflictException;
import com.placementportal.exception.TestExpiredException;
import com.placementportal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the timed test engine. Repositories are Mockito mocks and the Clock is fixed,
 * so "what happens 11 seconds after the deadline?" becomes a one-line test.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestServiceTest {

    private static final long USER_ID = 7L;
    private static final long ATTEMPT_ID = 100L;
    private static final long TEST_ID = 5L;
    private static final long GRACE = 10;
    private static final Instant T0 = Instant.parse("2026-10-28T10:00:00Z");   // attempt start; deadline = T0 + 10 min

    @Mock TestSetRepository testRepository;
    @Mock TestAttemptRepository attemptRepository;
    @Mock QuestionRepository questionRepository;
    @Mock AnswerRepository answerRepository;
    @Mock UserTopicStatRepository statRepository;
    @Mock UserRepository userRepository;

    private Topic arrays;
    private Topic dbms;
    private Question q1;      // arrays, correct option = index 0
    private Question q2;      // arrays
    private Question q3;      // dbms
    private TestAttempt attempt;

    @BeforeEach
    void setUp() {
        arrays = topic(1L, "Arrays");
        dbms = topic(2L, "DBMS");
        q1 = mcq(11L, arrays);
        q2 = mcq(12L, arrays);
        q3 = mcq(13L, dbms);

        TestSet test = new TestSet("Sample", "d", 10, true);
        ReflectionTestUtils.setField(test, "id", TEST_ID);
        attempt = new TestAttempt(null, test, T0, T0.plusSeconds(600), 3);
        ReflectionTestUtils.setField(attempt, "id", ATTEMPT_ID);

        when(attemptRepository.findByIdAndUserId(ATTEMPT_ID, USER_ID)).thenReturn(Optional.of(attempt));
        when(questionRepository.findByTestId(TEST_ID)).thenReturn(List.of(q1, q2, q3));
        when(statRepository.findByUserIdWithTopic(USER_ID)).thenReturn(new ArrayList<>());
    }

    private TestService serviceAt(Instant now) {
        return new TestService(testRepository, attemptRepository, questionRepository, answerRepository,
                statRepository, userRepository, Clock.fixed(now, ZoneOffset.UTC), GRACE);
    }

    // ------------------------------------------------------------------ scoring

    @Test
    void submit_scoresCorrectAnswers_andUpdatesTopicStats() {
        SubmitRequest request = new SubmitRequest(List.of(
                new AnswerRequest(11L, optionId(q1, 0)),      // right
                new AnswerRequest(12L, optionId(q2, 1)),      // wrong
                new AnswerRequest(13L, null)));               // skipped

        AttemptResult result = serviceAt(T0.plusSeconds(300)).submit(USER_ID, ATTEMPT_ID, request);

        assertEquals(1, result.summary().score());
        assertEquals(3, result.summary().totalQuestions());
        assertEquals(33.33, result.summary().percentage(), 0.001);
        assertEquals(AttemptStatus.SUBMITTED, attempt.getStatus());
        assertEquals(3, result.review().size());

        ArgumentCaptor<Iterable<UserTopicStat>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(statRepository).saveAll(captor.capture());
        List<UserTopicStat> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        UserTopicStat arraysStat = saved.stream().filter(s -> s.getTopic() == arrays).findFirst().orElseThrow();
        UserTopicStat dbmsStat = saved.stream().filter(s -> s.getTopic() == dbms).findFirst().orElseThrow();
        assertEquals(2, arraysStat.getAttempted());
        assertEquals(1, arraysStat.getCorrect());
        assertEquals(1, dbmsStat.getAttempted());      // a skipped question still counts as attempted...
        assertEquals(0, dbmsStat.getCorrect());        // ...and as not correct
    }

    @Test
    void submit_allCorrect_givesFullMarks() {
        SubmitRequest request = new SubmitRequest(List.of(
                new AnswerRequest(11L, optionId(q1, 0)),
                new AnswerRequest(12L, optionId(q2, 0)),
                new AnswerRequest(13L, optionId(q3, 0))));

        AttemptResult result = serviceAt(T0.plusSeconds(10)).submit(USER_ID, ATTEMPT_ID, request);

        assertEquals(3, result.summary().score());
        assertEquals(100.0, result.summary().percentage(), 0.001);
    }

    // ------------------------------------------------------------------ server-side timer

    @Test
    void submit_justInsideGracePeriod_isAccepted() {
        Instant now = T0.plusSeconds(600 + GRACE);              // exactly deadline + grace is still allowed
        AttemptResult result = serviceAt(now).submit(USER_ID, ATTEMPT_ID, new SubmitRequest(List.of()));
        assertEquals(AttemptStatus.SUBMITTED, result.summary().status());
    }

    @Test
    void submit_afterDeadlinePlusGrace_isRejected_andAttemptIsMarkedExpired() {
        Instant now = T0.plusSeconds(600 + GRACE + 1);
        TestService service = serviceAt(now);

        assertThrows(TestExpiredException.class,
                () -> service.submit(USER_ID, ATTEMPT_ID, new SubmitRequest(List.of())));

        assertEquals(AttemptStatus.EXPIRED, attempt.getStatus());
        verify(answerRepository, never()).saveAll(any());       // nothing is scored or stored
        verify(statRepository, never()).saveAll(any());
    }

    // ------------------------------------------------------------------ guard rails

    @Test
    void submit_twice_isRejectedWithConflict() {
        attempt.setStatus(AttemptStatus.SUBMITTED);
        TestService service = serviceAt(T0.plusSeconds(60));
        assertThrows(ConflictException.class,
                () -> service.submit(USER_ID, ATTEMPT_ID, new SubmitRequest(List.of())));
    }

    @Test
    void submit_optionBelongingToAnotherQuestion_isRejected() {
        SubmitRequest request = new SubmitRequest(List.of(new AnswerRequest(11L, optionId(q2, 0))));
        TestService service = serviceAt(T0.plusSeconds(60));
        assertThrows(BadRequestException.class, () -> service.submit(USER_ID, ATTEMPT_ID, request));
    }

    @Test
    void submit_questionNotInTest_isRejected() {
        SubmitRequest request = new SubmitRequest(List.of(new AnswerRequest(999L, 1L)));
        TestService service = serviceAt(T0.plusSeconds(60));
        assertThrows(BadRequestException.class, () -> service.submit(USER_ID, ATTEMPT_ID, request));
    }

    // ------------------------------------------------------------------ start / resume

    @Test
    void start_resumesAnUnfinishedAttempt_insteadOfCreatingANewOne() {
        TestSet test = attempt.getTestSet();
        when(testRepository.findById(TEST_ID)).thenReturn(Optional.of(test));
        when(attemptRepository.findFirstByUserIdAndTestSetIdAndStatus(USER_ID, TEST_ID, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));

        StartTestResponse response = serviceAt(T0.plusSeconds(100)).start(USER_ID, TEST_ID);

        assertEquals(ATTEMPT_ID, response.attemptId());
        assertEquals(500, response.secondsRemaining());          // 600 s duration - 100 s already used
        assertEquals(3, response.questions().size());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void start_neverLeaksTheCorrectOption() {
        TestSet test = attempt.getTestSet();
        when(testRepository.findById(TEST_ID)).thenReturn(Optional.of(test));
        when(attemptRepository.findFirstByUserIdAndTestSetIdAndStatus(USER_ID, TEST_ID, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));

        StartTestResponse response = serviceAt(T0.plusSeconds(1)).start(USER_ID, TEST_ID);

        // OptionResponse has only (id, text): there is simply no field that could carry the answer.
        assertEquals(2, response.questions().get(0).options().get(0).getClass().getRecordComponents().length);
    }

    // ------------------------------------------------------------------ helpers

    private static Topic topic(long id, String name) {
        Topic t = new Topic(name, "");
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    /** MCQ with three options (ids = questionId*10 + index); option 0 is the correct one. */
    private static Question mcq(long id, Topic topic) {
        Question q = Question.create("Q" + id, "d", Difficulty.EASY, QuestionType.MCQ, topic);
        ReflectionTestUtils.setField(q, "id", id);
        for (int i = 0; i < 3; i++) {
            QuestionOption o = new QuestionOption("opt" + i, i == 0);
            ReflectionTestUtils.setField(o, "id", id * 10 + i);
            q.addOption(o);
        }
        return q;
    }

    private static Long optionId(Question q, int index) {
        return q.getOptions().get(index).getId();
    }
}
