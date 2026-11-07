package com.placementportal.service;

import com.placementportal.dto.StudyDtos.*;
import com.placementportal.entity.Question;
import com.placementportal.entity.RevisionList;
import com.placementportal.exception.ConflictException;
import com.placementportal.exception.ResourceNotFoundException;
import com.placementportal.repository.QuestionRepository;
import com.placementportal.repository.RevisionListRepository;
import com.placementportal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RevisionListService {

    private final RevisionListRepository listRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuestionService questionService;

    public RevisionListService(RevisionListRepository listRepository, QuestionRepository questionRepository,
                               UserRepository userRepository, QuestionService questionService) {
        this.listRepository = listRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.questionService = questionService;
    }

    @Transactional(readOnly = true)
    public List<RevisionListSummary> list(Long userId) {
        return listRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(l -> new RevisionListSummary(l.getId(), l.getName(), l.getQuestions().size())).toList();
    }

    @Transactional
    public RevisionListSummary create(Long userId, String name) {
        String clean = name.trim();
        if (listRepository.existsByUserIdAndName(userId, clean)) {
            throw new ConflictException("You already have a list named '" + clean + "'");
        }
        RevisionList saved = listRepository.save(new RevisionList(userRepository.getReferenceById(userId), clean));
        return new RevisionListSummary(saved.getId(), saved.getName(), 0);
    }

    @Transactional(readOnly = true)
    public RevisionListDetail get(Long userId, Long listId) {
        return toDetail(userId, find(userId, listId));
    }

    @Transactional
    public void delete(Long userId, Long listId) {
        listRepository.delete(find(userId, listId));
    }

    @Transactional
    public RevisionListDetail addQuestion(Long userId, Long listId, Long questionId) {
        RevisionList list = find(userId, listId);
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        boolean already = list.getQuestions().stream().anyMatch(x -> x.getId().equals(questionId));
        if (!already) {
            list.getQuestions().add(q);          // Hibernate inserts the join-table row at commit
        }
        return toDetail(userId, list);
    }

    @Transactional
    public RevisionListDetail removeQuestion(Long userId, Long listId, Long questionId) {
        RevisionList list = find(userId, listId);
        list.getQuestions().removeIf(x -> x.getId().equals(questionId));
        return toDetail(userId, list);
    }

    /** Ownership check: the list is looked up by (id AND userId), so other users' lists look "not found". */
    private RevisionList find(Long userId, Long listId) {
        return listRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Revision list not found: " + listId));
    }

    private RevisionListDetail toDetail(Long userId, RevisionList list) {
        return new RevisionListDetail(list.getId(), list.getName(),
                questionService.toSummaries(userId, list.getQuestions()));
    }
}
