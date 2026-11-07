package com.placementportal.service;

import com.placementportal.dto.CommonDtos.PageResponse;
import com.placementportal.dto.QuestionDtos.*;
import com.placementportal.entity.*;
import com.placementportal.exception.BadRequestException;
import com.placementportal.exception.ResourceNotFoundException;
import com.placementportal.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final BookmarkRepository bookmarkRepository;
    private final SolvedQuestionRepository solvedRepository;

    public QuestionService(QuestionRepository questionRepository, TopicRepository topicRepository,
                           BookmarkRepository bookmarkRepository, SolvedQuestionRepository solvedRepository) {
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.solvedRepository = solvedRepository;
    }

    // =====================================================================
    //  STUDENT side
    // =====================================================================

    @Transactional(readOnly = true)
    public PageResponse<QuestionSummary> search(Long userId, Long topicId, Difficulty difficulty,
                                                QuestionType type, String keyword, int page, int size) {
        Page<Question> result = questionRepository.findAll(
                QuestionSpecifications.filter(topicId, difficulty, type, keyword), pageable(page, size));
        List<QuestionSummary> content = toSummaries(userId, result.getContent());
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public QuestionDetail getDetail(Long userId, Long id) {
        Question q = findDetail(id);
        boolean bookmarked = bookmarkRepository.existsByUserIdAndQuestionId(userId, id);
        boolean solved = solvedRepository.existsByUserIdAndQuestionId(userId, id);
        // Never leak answers: options are sent WITHOUT the correct flag, and the explanation of an MCQ
        // (which gives the answer away) is only returned for DSA questions.
        List<OptionResponse> options = q.getOptions().stream()
                .map(o -> new OptionResponse(o.getId(), o.getText())).toList();
        String explanation = q.getType() == QuestionType.DSA ? q.getExplanation() : null;
        return new QuestionDetail(q.getId(), q.getTitle(), q.getDescription(), q.getDifficulty(), q.getType(),
                q.getTopic().getId(), q.getTopic().getName(), q.getExternalLink(), explanation,
                bookmarked, solved, options);
    }

    /**
     * Converts questions to DTOs and adds the per-user flags (bookmarked / solved) using TWO queries for the
     * whole list - not two queries per question. Must be called inside a transaction (topic is lazy).
     */
    public List<QuestionSummary> toSummaries(Long userId, Collection<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        List<Long> ids = questions.stream().map(Question::getId).toList();
        Set<Long> bookmarked = new HashSet<>(bookmarkRepository.findBookmarkedIds(userId, ids));
        Set<Long> solved = new HashSet<>(solvedRepository.findSolvedIds(userId, ids));
        return questions.stream().map(q -> new QuestionSummary(
                q.getId(), q.getTitle(), q.getDifficulty(), q.getType(),
                q.getTopic().getId(), q.getTopic().getName(),
                bookmarked.contains(q.getId()), solved.contains(q.getId()))).toList();
    }

    // =====================================================================
    //  ADMIN side
    // =====================================================================

    @Transactional(readOnly = true)
    public PageResponse<AdminQuestionResponse> adminSearch(Long topicId, Difficulty difficulty, QuestionType type,
                                                           String keyword, int page, int size) {
        Page<Question> result = questionRepository.findAll(
                QuestionSpecifications.filter(topicId, difficulty, type, keyword), pageable(page, size));
        return PageResponse.from(result.map(this::toAdmin));
    }

    @Transactional(readOnly = true)
    public AdminQuestionResponse adminGet(Long id) {
        return toAdmin(findDetail(id));
    }

    @Transactional
    public AdminQuestionResponse create(QuestionRequest req) {
        validateOptions(req);
        Topic topic = topicRepository.findById(req.topicId())
                .orElseThrow(() -> new BadRequestException("Topic does not exist: " + req.topicId()));
        Question q = Question.create(req.title().trim(), req.description(), req.difficulty(), req.type(), topic);
        q.setExternalLink(req.externalLink());
        q.setExplanation(req.explanation());
        if (req.type() == QuestionType.MCQ) {
            req.options().forEach(o -> q.addOption(new QuestionOption(o.text().trim(), o.correct())));
        }
        return toAdmin(questionRepository.save(q));
    }

    @Transactional
    public AdminQuestionResponse update(Long id, QuestionRequest req) {
        validateOptions(req);
        Question q = findDetail(id);
        Topic topic = topicRepository.findById(req.topicId())
                .orElseThrow(() -> new BadRequestException("Topic does not exist: " + req.topicId()));
        q.setTitle(req.title().trim());
        q.setDescription(req.description());
        q.setDifficulty(req.difficulty());
        q.setType(req.type());
        q.setTopic(topic);
        q.setExternalLink(req.externalLink());
        q.setExplanation(req.explanation());
        syncOptions(q, req);
        return toAdmin(q);
    }

    @Transactional
    public void delete(Long id) {
        Question q = findDetail(id);
        questionRepository.delete(q);
        questionRepository.flush();    // force the SQL now, so a foreign-key failure surfaces as a clean 409
    }

    // ---------------------------------------------------------------------

    /** Update options IN PLACE (same ids) so answers of past attempts keep pointing to valid rows. */
    private void syncOptions(Question q, QuestionRequest req) {
        List<QuestionOption> existing = q.getOptions();
        List<OptionRequest> wanted = req.type() == QuestionType.MCQ ? req.options() : List.of();
        for (int i = 0; i < wanted.size(); i++) {
            OptionRequest w = wanted.get(i);
            if (i < existing.size()) {
                existing.get(i).update(w.text().trim(), w.correct());
            } else {
                q.addOption(new QuestionOption(w.text().trim(), w.correct()));
            }
        }
        while (existing.size() > wanted.size()) {
            existing.remove(existing.size() - 1);        // orphanRemoval deletes the row
        }
    }

    private void validateOptions(QuestionRequest req) {
        if (req.type() != QuestionType.MCQ) {
            return;
        }
        List<OptionRequest> options = req.options();
        if (options == null || options.size() < 2 || options.size() > 6) {
            throw new BadRequestException("An MCQ needs between 2 and 6 options");
        }
        long correct = options.stream().filter(OptionRequest::correct).count();
        if (correct != 1) {
            throw new BadRequestException("An MCQ must have exactly one correct option");
        }
    }

    private Question findDetail(Long id) {
        return questionRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + id));
    }

    private AdminQuestionResponse toAdmin(Question q) {
        List<AdminOptionResponse> options = q.getOptions().stream()
                .map(o -> new AdminOptionResponse(o.getId(), o.getText(), o.isCorrect())).toList();
        return new AdminQuestionResponse(q.getId(), q.getTitle(), q.getDescription(), q.getDifficulty(), q.getType(),
                q.getTopic().getId(), q.getTopic().getName(), q.getExternalLink(), q.getExplanation(), options);
    }

    /** Page size is capped: a client asking for size=1000000 must not be able to load the whole table. */
    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("id"));
    }
}
