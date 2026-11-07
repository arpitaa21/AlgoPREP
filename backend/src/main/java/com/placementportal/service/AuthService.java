package com.placementportal.service;

import com.placementportal.dto.AuthDtos.*;
import com.placementportal.entity.Role;
import com.placementportal.entity.RoleName;
import com.placementportal.entity.User;
import com.placementportal.exception.ConflictException;
import com.placementportal.repository.RoleRepository;
import com.placementportal.repository.UserRepository;
import com.placementportal.security.JwtService;
import com.placementportal.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        // Security rule: anyone who registers through the public API is ALWAYS a STUDENT.
        // Admins are created only by the seeder / directly in the database.
        Role student = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new IllegalStateException("STUDENT role is missing"));
        User user = new User(req.name().trim(), email, passwordEncoder.encode(req.password()));
        user.getRoles().add(student);
        userRepository.save(user);
        return buildResponse(new UserPrincipal(user));
    }

    public AuthResponse login(LoginRequest req) {
        // The AuthenticationManager loads the user, BCrypt-compares the password and throws
        // BadCredentialsException on failure (handled -> 401).
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim().toLowerCase(), req.password()));
        return buildResponse((UserPrincipal) auth.getPrincipal());
    }

    public UserInfo me(UserPrincipal p) {
        return toInfo(p);
    }

    private AuthResponse buildResponse(UserPrincipal p) {
        return new AuthResponse(jwtService.generateToken(p), "Bearer", jwtService.getExpirationMs(), toInfo(p));
    }

    private UserInfo toInfo(UserPrincipal p) {
        return new UserInfo(p.getId(), p.getName(), p.getUsername(),
                p.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", "")).toList());
    }
}
