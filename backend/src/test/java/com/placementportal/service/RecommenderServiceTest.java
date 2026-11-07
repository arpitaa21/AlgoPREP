package com.placementportal.service;

import com.placementportal.dto.RecommendationDtos.RecommendedQuestion;
import com.placementportal.dto.RecommendationDtos.WeakTopic;
import com.placementportal.entity.*;
import com.placementportal.repository.AnswerRepository;
import com.placementportal.repository.QuestionRepository;
import com.placementportal.repository.SolvedQuestionRepository;
import com.placementportal.repository.UserTopicStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecommenderServiceTest {

    private static final long USER = 7L;

    @Mock UserTopicStatRepository statRepository;
    @Mock QuestionRepository questionRepository;
    @Mock SolvedQuestionRepository solvedRepository;
    @Mock AnswerRepository answerRepository;
    @InjectMocks RecommenderService service;

    private Topic arrays;

    @BeforeEach
    void setUp() {
        arrays = topic(1L, "Arrays");
        Topic graphs = topic(2L, "Graphs");
        Topic dbms = topic(3L, "DBMS");
        when(statRepository.findByUserIdWithTopic(USER)).thenReturn(List.of(
                stat(arrays, 10, 2),       // 20 % -> weak
                stat(graphs, 2, 0),        // too few attempts -> ignored
                stat(dbms, 5, 4)));        // 80 % -> strong
        when(solvedRepository.findAllSolvedIds(USER)).thenReturn(List.of(13L));
        when(answerRepository.findCorrectQuestionIds(USER)).thenReturn(List.of());
    }

    @Test
    void weakTopics_returnsOnlyTopicsBelowThresholdWithEnoughAttempts() {
        List<WeakTopic> weak = service.weakTopics(USER);

        assertEquals(1, weak.size());
        assertEquals("Arrays", weak.get(0).topicName());
        assertEquals(0.2, weak.get(0).accuracy(), 1e-9);
        assertEquals(0.8, weak.get(0).weaknessScore(), 1e-9);
    }

    @Test
    void recommend_skipsSolvedQuestions_andPrefersEasyForVeryWeakTopic() {
        when(questionRepository.findByTopicIdWithTopic(1L)).thenReturn(List.of(
                question(12L, arrays, Difficulty.HARD),
                question(11L, arrays, Difficulty.MEDIUM),
                question(10L, arrays, Difficulty.EASY),
                question(13L, arrays, Difficulty.EASY)));          // already solved -> must be excluded

        List<RecommendedQuestion> recs = service.recommend(USER, 3);

        assertEquals(List.of(10L, 11L, 12L), recs.stream().map(RecommendedQuestion::questionId).toList());
        assertTrue(recs.get(0).reason().contains("2 of 10"));
        assertTrue(recs.stream().noneMatch(r -> r.questionId() == 13L));
    }

    @Test
    void recommend_respectsTheLimit() {
        when(questionRepository.findByTopicIdWithTopic(1L)).thenReturn(List.of(
                question(10L, arrays, Difficulty.EASY), question(11L, arrays, Difficulty.EASY),
                question(12L, arrays, Difficulty.EASY)));

        assertEquals(2, service.recommend(USER, 2).size());
    }

    @Test
    void recommend_withNoWeakTopics_returnsEmptyList() {
        when(statRepository.findByUserIdWithTopic(USER)).thenReturn(List.of(stat(arrays, 10, 9)));
        assertTrue(service.recommend(USER, 5).isEmpty());
    }

    // ------------------------------------------------------------------ helpers

    private static Topic topic(long id, String name) {
        Topic t = new Topic(name, "");
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private static UserTopicStat stat(Topic topic, int attempted, int correct) {
        UserTopicStat s = new UserTopicStat(null, topic);
        for (int i = 0; i < attempted; i++) {
            s.record(i < correct);
        }
        return s;
    }

    private static Question question(long id, Topic topic, Difficulty d) {
        Question q = Question.create("Q" + id, "d", d, QuestionType.DSA, topic);
        ReflectionTestUtils.setField(q, "id", id);
        return q;
    }
}
