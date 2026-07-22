package com.hirecheck.controller;

import com.hirecheck.entity.Candidate;
import com.hirecheck.entity.Test;
import com.hirecheck.exception.ApiException;
import com.hirecheck.repository.CandidateRepository;
import com.hirecheck.repository.TestRepository;
import com.hirecheck.util.ClientIpUtil;
import com.hirecheck.util.NanoidUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public-test")
public class PublicTestController {

    private final TestRepository testRepository;
    private final CandidateRepository candidateRepository;

    public PublicTestController(TestRepository testRepository, CandidateRepository candidateRepository) {
        this.testRepository = testRepository;
        this.candidateRepository = candidateRepository;
    }

    @PostMapping("/{testId}/register")
    public ResponseEntity<Map<String, String>> register(
            @PathVariable Integer testId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test not found"));

        String email = body.get("email");
        if (candidateRepository.findByEmailAndTestId(email, testId).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You have already taken this test");
        }

        Candidate candidate = new Candidate();
        candidate.setName(body.get("name"));
        candidate.setEmail(email);
        candidate.setPhone(body.get("phone"));
        candidate.setTestId(testId);
        candidate.setTestLink(NanoidUtil.generate(10));
        candidate.setStatus("pending");
        candidate.setInvitedBy(test.getCreatedBy());
        candidate.setIpAddress(ClientIpUtil.getClientIp(request));
        candidate = candidateRepository.save(candidate);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("testLink", candidate.getTestLink()));
    }
}
