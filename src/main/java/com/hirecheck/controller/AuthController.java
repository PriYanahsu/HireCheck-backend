package com.hirecheck.controller;

import com.hirecheck.dto.LoginRequest;
import com.hirecheck.dto.SignupRequest;
import com.hirecheck.entity.User;
import com.hirecheck.exception.ApiException;
import com.hirecheck.repository.UserRepository;
import com.hirecheck.security.JwtUtil;
import com.hirecheck.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest body) {
        User user = userRepository.findByUsername(body.getUsername())
                .filter(u -> u.getPassword().equals(body.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect username or password"));
        return ResponseEntity.ok(authResponse(user));
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest body) {
        if (userRepository.findByUsername(body.getUsername()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userRepository.findByEmail(body.getEmail()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        User user = new User();
        user.setUsername(body.getUsername());
        user.setPassword(body.getPassword());
        user.setEmail(body.getEmail());
        user.setName(body.getName());
        user.setCompany(body.getCompany());
        user = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse(user));
    }

    @PostMapping("/logout")
    public Map<String, String> logout() {
        return Map.of("message", "Logged out successfully");
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("name", user.getName());
        result.put("company", user.getCompany());
        return result;
    }

    private Map<String, Object> authResponse(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtUtil.sign(user));
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("name", user.getName());
        result.put("company", user.getCompany());
        return result;
    }
}
