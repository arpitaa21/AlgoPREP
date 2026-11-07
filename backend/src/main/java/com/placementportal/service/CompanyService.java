package com.placementportal.service;

import com.placementportal.dto.CompanyDtos.*;
import com.placementportal.entity.Company;
import com.placementportal.entity.CompanyRoadmap;
import com.placementportal.entity.Topic;
import com.placementportal.exception.BadRequestException;
import com.placementportal.exception.ConflictException;
import com.placementportal.exception.ResourceNotFoundException;
import com.placementportal.repository.CompanyRepository;
import com.placementportal.repository.CompanyRoadmapRepository;
import com.placementportal.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyRoadmapRepository roadmapRepository;
    private final TopicRepository topicRepository;

    public CompanyService(CompanyRepository companyRepository, CompanyRoadmapRepository roadmapRepository,
                          TopicRepository topicRepository) {
        this.companyRepository = companyRepository;
        this.roadmapRepository = roadmapRepository;
        this.topicRepository = topicRepository;
    }

    // ---------------- student reads ----------------

    @Transactional(readOnly = true)
    public List<CompanySummary> list() {
        return companyRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new CompanySummary(c.getId(), c.getName(), c.getDescription(), c.getRoadmap().size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyDetail detail(Long id) {
        Company c = findCompany(id);
        List<RoadmapStepResponse> steps = roadmapRepository.findByCompanyWithTopic(id).stream()
                .map(CompanyService::toStep).toList();
        return new CompanyDetail(c.getId(), c.getName(), c.getDescription(), steps);
    }

    // ---------------- admin writes ----------------

    @Transactional
    public CompanySummary create(CompanyRequest req) {
        String name = req.name().trim();
        if (companyRepository.existsByName(name)) {
            throw new ConflictException("Company '" + name + "' already exists");
        }
        Company saved = companyRepository.save(new Company(name, req.description()));
        return new CompanySummary(saved.getId(), saved.getName(), saved.getDescription(), 0);
    }

    @Transactional
    public CompanySummary update(Long id, CompanyRequest req) {
        Company c = findCompany(id);
        String name = req.name().trim();
        if (!c.getName().equalsIgnoreCase(name) && companyRepository.existsByName(name)) {
            throw new ConflictException("Company '" + name + "' already exists");
        }
        c.setName(name);
        c.setDescription(req.description());
        return new CompanySummary(c.getId(), c.getName(), c.getDescription(), c.getRoadmap().size());
    }

    @Transactional
    public void delete(Long id) {
        companyRepository.delete(findCompany(id));      // cascade ALL removes its roadmap steps too
    }

    @Transactional
    public RoadmapStepResponse addStep(Long companyId, RoadmapStepRequest req) {
        Company c = findCompany(companyId);
        CompanyRoadmap step = new CompanyRoadmap(c, req.stepOrder(), req.title().trim(), req.description(),
                findTopicOrNull(req.topicId()), req.resourceUrl());
        return toStep(roadmapRepository.save(step));
    }

    @Transactional
    public RoadmapStepResponse updateStep(Long stepId, RoadmapStepRequest req) {
        CompanyRoadmap step = roadmapRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap step not found: " + stepId));
        step.setStepOrder(req.stepOrder());
        step.setTitle(req.title().trim());
        step.setDescription(req.description());
        step.setTopic(findTopicOrNull(req.topicId()));
        step.setResourceUrl(req.resourceUrl());
        return toStep(step);
    }

    @Transactional
    public void deleteStep(Long stepId) {
        if (!roadmapRepository.existsById(stepId)) {
            throw new ResourceNotFoundException("Roadmap step not found: " + stepId);
        }
        roadmapRepository.deleteById(stepId);
    }

    // ---------------- helpers ----------------

    private Company findCompany(Long id) {
        return companyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));
    }

    private Topic findTopicOrNull(Long topicId) {
        if (topicId == null) {
            return null;
        }
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new BadRequestException("Topic does not exist: " + topicId));
    }

    private static RoadmapStepResponse toStep(CompanyRoadmap r) {
        Topic t = r.getTopic();
        return new RoadmapStepResponse(r.getId(), r.getStepOrder(), r.getTitle(), r.getDescription(),
                t == null ? null : t.getId(), t == null ? null : t.getName(), r.getResourceUrl());
    }
}
