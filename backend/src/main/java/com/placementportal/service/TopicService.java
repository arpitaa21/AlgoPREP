package com.placementportal.service;

import com.placementportal.dto.QuestionDtos.TopicRequest;
import com.placementportal.dto.QuestionDtos.TopicResponse;
import com.placementportal.entity.Topic;
import com.placementportal.exception.ConflictException;
import com.placementportal.exception.ResourceNotFoundException;
import com.placementportal.repository.CompanyRoadmapRepository;
import com.placementportal.repository.QuestionRepository;
import com.placementportal.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final CompanyRoadmapRepository roadmapRepository;

    public TopicService(TopicRepository topicRepository, QuestionRepository questionRepository,
                        CompanyRoadmapRepository roadmapRepository) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.roadmapRepository = roadmapRepository;
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> list() {
        return topicRepository.findAllByOrderByNameAsc().stream().map(TopicService::toResponse).toList();
    }

    @Transactional
    public TopicResponse create(TopicRequest req) {
        String name = req.name().trim();
        if (topicRepository.existsByName(name)) {
            throw new ConflictException("Topic '" + name + "' already exists");
        }
        return toResponse(topicRepository.save(new Topic(name, req.description())));
    }

    @Transactional
    public TopicResponse update(Long id, TopicRequest req) {
        Topic topic = find(id);
        String name = req.name().trim();
        if (!topic.getName().equalsIgnoreCase(name) && topicRepository.existsByName(name)) {
            throw new ConflictException("Topic '" + name + "' already exists");
        }
        topic.setName(name);
        topic.setDescription(req.description());
        return toResponse(topic);                // managed entity: changes are saved automatically at commit (dirty checking)
    }

    @Transactional
    public void delete(Long id) {
        Topic topic = find(id);
        if (questionRepository.existsByTopicId(id) || roadmapRepository.existsByTopicId(id)) {
            throw new ConflictException("Topic is still used by questions or roadmap steps");
        }
        topicRepository.delete(topic);
    }

    private Topic find(Long id) {
        return topicRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
    }

    private static TopicResponse toResponse(Topic t) {
        return new TopicResponse(t.getId(), t.getName(), t.getDescription());
    }
}
