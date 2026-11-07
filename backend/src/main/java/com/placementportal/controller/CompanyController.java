package com.placementportal.controller;

import com.placementportal.dto.CompanyDtos.CompanyDetail;
import com.placementportal.dto.CompanyDtos.CompanySummary;
import com.placementportal.service.CompanyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public List<CompanySummary> list() {
        return companyService.list();
    }

    @GetMapping("/{id}")
    public CompanyDetail detail(@PathVariable Long id) {
        return companyService.detail(id);
    }
}
