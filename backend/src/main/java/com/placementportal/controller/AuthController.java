package com.placementportal.controller;

import com.placementportal.dto.AuthDtos.*;
import com.placementportal.security.UserPrincipal;
import com.placementportal.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** The frontend calls this on page load to find out who the stored token belongs to. */
    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal UserPrincipal me) {
        return authService.me(me);
    }
}
