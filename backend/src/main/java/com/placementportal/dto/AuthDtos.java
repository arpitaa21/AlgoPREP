package com.placementportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() { }

    // max 72: BCrypt only looks at the first 72 bytes of a password.
    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String password) { }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }

    public record UserInfo(Long id, String name, String email, List<String> roles) { }

    public record AuthResponse(String token, String tokenType, long expiresInMs, UserInfo user) { }
}
