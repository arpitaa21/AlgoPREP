package com.placementportal.controller;

import com.placementportal.dto.CompanyDtos.*;
import com.placementportal.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminCompanyController {

    private final CompanyService companyService;

    public AdminCompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/companies")
    public ResponseEntity<CompanySummary> create(@Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(request));
    }

    @PutMapping("/companies/{id}")
    public CompanySummary update(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/companies/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/companies/{id}/steps")
    public ResponseEntity<RoadmapStepResponse> addStep(@PathVariable Long id,
                                                       @Valid @RequestBody RoadmapStepRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.addStep(id, request));
    }

    @PutMapping("/roadmap-steps/{stepId}")
    public RoadmapStepResponse updateStep(@PathVariable Long stepId, @Valid @RequestBody RoadmapStepRequest request) {
        return companyService.updateStep(stepId, request);
    }

    @DeleteMapping("/roadmap-steps/{stepId}")
    public ResponseEntity<Void> deleteStep(@PathVariable Long stepId) {
        companyService.deleteStep(stepId);
        return ResponseEntity.noContent().build();
    }
}
