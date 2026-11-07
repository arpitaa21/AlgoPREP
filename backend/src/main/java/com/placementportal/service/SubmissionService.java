package com.placementportal.service;

import com.placementportal.dto.StudyDtos.SubmissionRequest;
import com.placementportal.dto.StudyDtos.SubmissionResponse;
import com.placementportal.entity.CodingSubmission;
import com.placementportal.entity.Question;
import com.placementportal.entity.QuestionType;
import com.placementportal.entity.SubmissionStatus;
import com.placementportal.exception.BadRequestException;
import com.placementportal.exception.ResourceNotFoundException;
import com.placementportal.repository.CodingSubmissionRepository;
import com.placementportal.repository.QuestionRepository;
import com.placementportal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Records that a student tried / solved a DSA question. The portal does NOT run code: the student solves on
 * LeetCode/GFG/etc. and logs the result here. (Running untrusted code safely needs a sandbox - see limitations.)
 */
@Service
public class SubmissionService {

    private final CodingSubmissionRepository submissionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final SolvedService solvedService;
    private final Clock clock;

    public SubmissionService(CodingSubmissionRepository submissionRepository, QuestionRepository questionRepository,
                             UserRepository userRepository, SolvedService solvedService, Clock clock) {
        this.submissionRepository = submissionRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.solvedService = solvedService;
        this.clock = clock;
    }

    @Transactional
    public SubmissionResponse record(Long userId, SubmissionRequest req) {
        Question q = questionRepository.findById(req.questionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + req.questionId()));
        if (q.getType() != QuestionType.DSA) {
            throw new BadRequestException("Coding submissions are only for DSA questions");
        }
        CodingSubmission saved = submissionRepository.save(new CodingSubmission(
                userRepository.getReferenceById(userId), q, req.language().trim(), req.code(),
                req.status(), req.timeTakenMinutes(), clock.instant()));
        if (req.status() == SubmissionStatus.SOLVED) {
            solvedService.markSolved(userId, q.getId());       // a SOLVED submission also marks the question solved
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> list(Long userId, Long questionId) {
        List<CodingSubmission> rows = questionId == null
                ? submissionRepository.findAllByUser(userId)
                : submissionRepository.findByUserAndQuestion(userId, questionId);
        return rows.stream().map(SubmissionService::toResponse).toList();
    }

    private static SubmissionResponse toResponse(CodingSubmission s) {
        return new SubmissionResponse(s.getId(), s.getQuestion().getId(), s.getQuestion().getTitle(),
                s.getLanguage(), s.getCode(), s.getStatus(), s.getTimeTakenMinutes(), s.getSubmittedAt());
    }
}
