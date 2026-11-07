package com.placementportal.service;

import com.placementportal.dto.QuestionDtos.QuestionSummary;
import com.placementportal.entity.Bookmark;
import com.placementportal.entity.Question;
import com.placementportal.exception.ResourceNotFoundException;
import com.placementportal.repository.BookmarkRepository;
import com.placementportal.repository.QuestionRepository;
import com.placementportal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuestionService questionService;

    public BookmarkService(BookmarkRepository bookmarkRepository, QuestionRepository questionRepository,
                           UserRepository userRepository, QuestionService questionService) {
        this.bookmarkRepository = bookmarkRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.questionService = questionService;
    }

    @Transactional
    public void add(Long userId, Long questionId) {
        if (bookmarkRepository.existsByUserIdAndQuestionId(userId, questionId)) {
            return;                                   // idempotent
        }
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        bookmarkRepository.save(new Bookmark(userRepository.getReferenceById(userId), q));
    }

    @Transactional
    public void remove(Long userId, Long questionId) {
        bookmarkRepository.deleteByUserIdAndQuestionId(userId, questionId);
    }

    @Transactional(readOnly = true)
    public List<QuestionSummary> list(Long userId) {
        List<Question> questions = bookmarkRepository.findAllByUser(userId).stream()
                .map(Bookmark::getQuestion).toList();
        return questionService.toSummaries(userId, questions);
    }
}
