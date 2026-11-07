package com.placementportal.controller;

import com.placementportal.dto.AdminDtos.AdminTestResponse;
import com.placementportal.dto.AdminDtos.TestRequest;
import com.placementportal.service.AdminTestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tests")
public class AdminTestController {

    private final AdminTestService service;

    public AdminTestController(AdminTestService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminTestResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public AdminTestResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<AdminTestResponse> create(@Valid @RequestBody TestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public AdminTestResponse update(@PathVariable Long id, @Valid @RequestBody TestRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
