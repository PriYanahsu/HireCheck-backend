package com.hirecheck.controller;

import com.hirecheck.entity.Test;
import com.hirecheck.exception.ApiException;
import com.hirecheck.repository.*;
import com.hirecheck.security.UserPrincipal;
import com.hirecheck.service.StatsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final CandidateRepository candidateRepository;
    private final ResponseRepository responseRepository;
    private final StatsService statsService;

    public TestController(
            TestRepository testRepository,
            QuestionRepository questionRepository,
            CandidateRepository candidateRepository,
            ResponseRepository responseRepository,
            StatsService statsService) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.candidateRepository = candidateRepository;
        this.responseRepository = responseRepository;
        this.statsService = statsService;
    }

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        List<Test> tests = testRepository.findByCreatedBy(principal.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Test test : tests) {
            Map<String, Object> item = toTestMap(test);
            item.put("questionCount", questionRepository.countByTestId(test.getId()));
            item.put("stats", statsService.getTestStats(test.getId()));
            result.add(item);
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Integer id, @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test not found"));
        if (!test.getCreatedBy().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Unauthorized to access this test");
        }
        Map<String, Object> result = toTestMap(test);
        result.put("questions", questionRepository.findByTestIdOrderByOrderAsc(id));
        result.put("stats", statsService.getTestStats(id));
        return result;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        Test test = new Test();
        test.setTitle((String) body.get("title"));
        test.setDescription((String) body.get("description"));
        test.setDuration(((Number) body.get("duration")).intValue());
        if (body.get("passingScore") != null) test.setPassingScore(((Number) body.get("passingScore")).intValue());
        if (body.get("shuffleQuestions") != null) test.setShuffleQuestions((Boolean) body.get("shuffleQuestions"));
        test.setCreatedBy(principal.getId());
        test = testRepository.save(test);
        return ResponseEntity.status(HttpStatus.CREATED).body(toTestMap(test));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        Test test = requireOwnedTest(id, principal);
        if (body.containsKey("title")) test.setTitle((String) body.get("title"));
        if (body.containsKey("description")) test.setDescription((String) body.get("description"));
        if (body.containsKey("duration")) test.setDuration(((Number) body.get("duration")).intValue());
        if (body.containsKey("passingScore")) test.setPassingScore(body.get("passingScore") != null ? ((Number) body.get("passingScore")).intValue() : null);
        if (body.containsKey("shuffleQuestions")) test.setShuffleQuestions((Boolean) body.get("shuffleQuestions"));
        return toTestMap(testRepository.save(test));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Integer id, @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        requireOwnedTest(id, principal);

        List<Integer> candidateIds = candidateRepository.findByTestId(id).stream()
                .map(c -> c.getId()).toList();
        if (!candidateIds.isEmpty()) {
            responseRepository.deleteByCandidateIdIn(candidateIds);
            candidateRepository.deleteByTestId(id);
        }
        questionRepository.deleteByTestId(id);
        testRepository.deleteById(id);
        return Map.of("message", "Test deleted successfully");
    }

    @GetMapping("/{id}/candidates")
    public List<com.hirecheck.entity.Candidate> listCandidates(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        requireOwnedTest(id, principal);
        return candidateRepository.findByTestId(id);
    }

    private Test requireOwnedTest(Integer id, UserPrincipal principal) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test not found"));
        if (!test.getCreatedBy().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Unauthorized to update this test");
        }
        return test;
    }

    private Map<String, Object> toTestMap(Test test) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", test.getId());
        m.put("title", test.getTitle());
        m.put("description", test.getDescription());
        m.put("duration", test.getDuration());
        m.put("passingScore", test.getPassingScore());
        m.put("shuffleQuestions", test.getShuffleQuestions());
        m.put("createdBy", test.getCreatedBy());
        m.put("createdAt", test.getCreatedAt() != null ? test.getCreatedAt().toString() : null);
        return m;
    }

    private void requireAuth(UserPrincipal principal) {
        if (principal == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}
