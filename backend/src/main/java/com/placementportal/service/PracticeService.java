package com.placementportal.service;

import com.placementportal.dto.QuestionDtos.PracticeResult;
import com.placementportal.entity.Question;
import com.placementportal.entity.QuestionOption;
import com.placementportal.entity.QuestionType;
import com.placementportal.entity.UserTopicStat;
import com.placementportal.exception.BadRequestException;
import com.placementportal.exception.ResourceNotFoundException;
import com.placementportal.repository.QuestionRepository;
import com.placementportal.repository.UserRepository;
import com.placementportal.repository.UserTopicStatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets a student answer ONE MCQ outside a timed test (this is how "targeted practice" on a weak topic works).
 * The answer updates the weak-topic statistics, so practising really improves the recommender's view of the student.
 */
@Service
public class PracticeService {

    private final QuestionRepository questionRepository;
    private final UserTopicStatRepository statRepository;
    private final UserRepository userRepository;
    private final SolvedService solvedService;

    public PracticeService(QuestionRepository questionRepository, UserTopicStatRepository statRepository,
                           UserRepository userRepository, SolvedService solvedService) {
        this.questionRepository = questionRepository;
        this.statRepository = statRepository;
        this.userRepository = userRepository;
        this.solvedService = solvedService;
    }

    @Transactional
    public PracticeResult check(Long userId, Long questionId, Long optionId) {
        Question q = questionRepository.findDetailById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        if (q.getType() != QuestionType.MCQ) {
            throw new BadRequestException("Only MCQ questions can be answered here");
        }
        QuestionOption chosen = q.getOptions().stream()
                .filter(o -> o.getId().equals(optionId)).findFirst()
                .orElseThrow(() -> new BadRequestException("Option does not belong to this question"));
        boolean correct = chosen.isCorrect();

        UserTopicStat stat = statRepository.findByUserIdAndTopicId(userId, q.getTopic().getId())
                .orElseGet(() -> new UserTopicStat(userRepository.getReferenceById(userId), q.getTopic()));
        stat.record(correct);
        statRepository.save(stat);

        if (correct) {
            solvedService.markSolved(userId, questionId);
        }
        Long correctId = q.getOptions().stream().filter(QuestionOption::isCorrect)
                .findFirst().map(QuestionOption::getId).orElse(null);
        return new PracticeResult(correct, correctId, q.getExplanation());
    }
}
