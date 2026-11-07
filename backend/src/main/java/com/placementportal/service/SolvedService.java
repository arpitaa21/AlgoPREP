package com.placementportal.service;

import com.placementportal.dto.QuestionDtos.QuestionSummary;
import com.placementportal.entity.Question;
import com.placementportal.entity.SolvedQuestion;
import com.placementportal.exception.ResourceNotFoundException;
import com.placementportal.repository.QuestionRepository;
import com.placementportal.repository.SolvedQuestionRepository;
import com.placementportal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SolvedService {

    private final SolvedQuestionRepository solvedRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuestionService questionService;

    public SolvedService(SolvedQuestionRepository solvedRepository, QuestionRepository questionRepository,
                         UserRepository userRepository, QuestionService questionService) {
        this.solvedRepository = solvedRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.questionService = questionService;
    }

    /** Idempotent: marking an already-solved question again does nothing (no duplicate row, no error). */
    @Transactional
    public void markSolved(Long userId, Long questionId) {
        if (solvedRepository.existsByUserIdAndQuestionId(userId, questionId)) {
            return;
        }
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        // getReferenceById = a lightweight proxy holding only the id; avoids a SELECT just to set a foreign key.
        solvedRepository.save(new SolvedQuestion(userRepository.getReferenceById(userId), q));
    }

    @Transactional
    public void unmark(Long userId, Long questionId) {
        solvedRepository.deleteByUserIdAndQuestionId(userId, questionId);
    }

    @Transactional(readOnly = true)
    public List<QuestionSummary> list(Long userId) {
        List<Question> questions = solvedRepository.findAllByUser(userId).stream()
                .map(SolvedQuestion::getQuestion).toList();
        return questionService.toSummaries(userId, questions);
    }
}
