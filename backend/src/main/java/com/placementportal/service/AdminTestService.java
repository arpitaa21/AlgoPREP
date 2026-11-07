package com.placementportal.service;

import com.placementportal.dto.AdminDtos.AdminTestResponse;
import com.placementportal.dto.AdminDtos.TestRequest;
import com.placementportal.entity.Question;
import com.placementportal.entity.QuestionType;
import com.placementportal.entity.TestSet;
import com.placementportal.exception.BadRequestException;
import com.placementportal.exception.ResourceNotFoundException;
import com.placementportal.repository.QuestionRepository;
import com.placementportal.repository.TestSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin CRUD for timed tests. */
@Service
public class AdminTestService {

    private final TestSetRepository testRepository;
    private final QuestionRepository questionRepository;

    public AdminTestService(TestSetRepository testRepository, QuestionRepository questionRepository) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminTestResponse> list() {
        return testRepository.findAll().stream().map(AdminTestService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdminTestResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public AdminTestResponse create(TestRequest req) {
        TestSet test = new TestSet(req.title().trim(), req.description(), req.durationMinutes(), req.active());
        test.getQuestions().addAll(loadMcqs(req.questionIds()));
        return toResponse(testRepository.save(test));
    }

    @Transactional
    public AdminTestResponse update(Long id, TestRequest req) {
        TestSet test = find(id);
        test.setTitle(req.title().trim());
        test.setDescription(req.description());
        test.setDurationMinutes(req.durationMinutes());
        test.setActive(req.active());
        List<Question> questions = loadMcqs(req.questionIds());
        test.getQuestions().clear();
        test.getQuestions().addAll(questions);
        return toResponse(test);
    }

    @Transactional
    public void delete(Long id) {
        testRepository.delete(find(id));
        testRepository.flush();         // surfaces a foreign-key failure (test already attempted) as a 409
    }

    private TestSet find(Long id) {
        return testRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Test not found: " + id));
    }

    private List<Question> loadMcqs(List<Long> questionIds) {
        List<Long> ids = questionIds.stream().distinct().toList();
        List<Question> found = questionRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new BadRequestException("Some question ids do not exist");
        }
        if (found.stream().anyMatch(q -> q.getType() != QuestionType.MCQ)) {
            throw new BadRequestException("Only MCQ questions can be added to a timed test");
        }
        return found;
    }

    private static AdminTestResponse toResponse(TestSet t) {
        return new AdminTestResponse(t.getId(), t.getTitle(), t.getDescription(), t.getDurationMinutes(),
                t.isActive(), t.getQuestions().stream().map(Question::getId).toList());
    }
}
