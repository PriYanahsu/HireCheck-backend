package com.hirecheck.controller;

import com.hirecheck.entity.Candidate;
import com.hirecheck.entity.Question;
import com.hirecheck.entity.Response;
import com.hirecheck.entity.Test;
import com.hirecheck.exception.ApiException;
import com.hirecheck.repository.*;
import com.hirecheck.security.UserPrincipal;
import com.hirecheck.service.CandidateTestService;
import com.hirecheck.service.EmailService;
import com.hirecheck.util.ClientIpUtil;
import com.hirecheck.util.NanoidUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
public class CandidateController {

    private final CandidateRepository candidateRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final CandidateTestService candidateTestService;
    private final EmailService emailService;

    public CandidateController(
            CandidateRepository candidateRepository,
            TestRepository testRepository,
            QuestionRepository questionRepository,
            ResponseRepository responseRepository,
            UserRepository userRepository,
            CandidateTestService candidateTestService,
            EmailService emailService) {
        this.candidateRepository = candidateRepository;
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.responseRepository = responseRepository;
        this.userRepository = userRepository;
        this.candidateTestService = candidateTestService;
        this.emailService = emailService;
    }

    @GetMapping("/api/candidates")
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Test test : testRepository.findByCreatedBy(principal.getId())) {
            for (Candidate candidate : candidateRepository.findByTestId(test.getId())) {
                if (status != null && !status.equals(candidate.getStatus())) continue;
                Map<String, Object> item = toCandidateMap(candidate);
                item.put("testTitle", test.getTitle());
                item.put("passingScore", test.getPassingScore());
                result.add(item);
            }
        }
        result.sort((a, b) -> {
            String aTs = (String) a.get("invitedAt");
            String bTs = (String) b.get("invitedAt");
            if (aTs == null && bTs == null) return 0;
            if (aTs == null) return 1;
            if (bTs == null) return -1;
            return bTs.compareTo(aTs);
        });
        return result;
    }

    @PostMapping("/api/candidates")
    public ResponseEntity<Map<String, Object>> invite(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        Integer testId = ((Number) body.get("testId")).intValue();
        Test test = requireOwnedTest(testId, principal);

        Candidate candidate = new Candidate();
        candidate.setName((String) body.get("name"));
        candidate.setEmail((String) body.get("email"));
        if (body.get("phone") != null) candidate.setPhone((String) body.get("phone"));
        candidate.setTestId(testId);
        candidate.setInvitedBy(principal.getId());
        candidate.setTestLink(NanoidUtil.generate(10));
        candidate.setStatus("pending");
        candidate = candidateRepository.save(candidate);

        var user = userRepository.findById(principal.getId()).orElseThrow();
        Map<String, Object> emailResult = emailService.sendTestInvitation(
                candidate.getEmail(), candidate.getName(), test.getTitle(),
                candidate.getTestLink(), test.getDuration(), user.getCompany());

        Map<String, Object> result = toCandidateMap(candidate);
        result.put("emailSent", emailResult.get("success"));
        if (emailResult.get("error") != null) result.put("emailError", emailResult.get("error"));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/api/candidates/bulk-invite")
    public Map<String, Object> bulkInvite(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
        if (candidates == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Candidates array is required.");
        Integer testId = body.get("testId") != null ? ((Number) body.get("testId")).intValue() : null;
        if (testId == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Test ID is required.");

        Test test = requireOwnedTest(testId, principal);
        var user = userRepository.findById(principal.getId()).orElseThrow();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> c : candidates) {
            String name = (String) c.get("name");
            String phone = (String) c.get("phone");
            String email = (String) c.get("email");

            if (name == null || phone == null || email == null) {
                Map<String, Object> r = new LinkedHashMap<>(c);
                r.put("success", false);
                r.put("error", "Missing required fields");
                results.add(r);
                continue;
            }
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                Map<String, Object> r = new LinkedHashMap<>(c);
                r.put("success", false);
                r.put("error", "Invalid email format");
                results.add(r);
                continue;
            }
            if (!phone.matches("^[0-9+\\-\\s()]{7,}$")) {
                Map<String, Object> r = new LinkedHashMap<>(c);
                r.put("success", false);
                r.put("error", "Invalid phone number");
                results.add(r);
                continue;
            }

            try {
                Candidate candidate = new Candidate();
                candidate.setName(name);
                candidate.setEmail(email);
                candidate.setPhone(phone);
                candidate.setTestId(test.getId());
                candidate.setInvitedBy(principal.getId());
                candidate.setTestLink(NanoidUtil.generate(10));
                candidate.setStatus("pending");
                candidate = candidateRepository.save(candidate);

                emailService.sendTestInvitation(
                        candidate.getEmail(), candidate.getName(), test.getTitle(),
                        candidate.getTestLink(), test.getDuration(), user.getCompany());

                Map<String, Object> r = new LinkedHashMap<>(c);
                r.put("id", candidate.getId());
                r.put("testId", candidate.getTestId());
                r.put("testLink", candidate.getTestLink());
                r.put("status", candidate.getStatus());
                r.put("invitedAt", candidate.getInvitedAt() != null ? candidate.getInvitedAt().toString() : null);
                r.put("success", true);
                results.add(r);
            } catch (Exception ex) {
                Map<String, Object> r = new LinkedHashMap<>(c);
                r.put("success", false);
                r.put("error", ex.getMessage() != null ? ex.getMessage() : "Failed to create candidate");
                results.add(r);
            }
        }
        return Map.of("results", results);
    }

    @DeleteMapping("/api/candidates/{id}")
    public Map<String, Object> delete(@PathVariable Integer id, @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Candidate not found"));
        requireOwnedTest(candidate.getTestId(), principal);
        candidateRepository.deleteById(id);
        return Map.of("success", true);
    }

    @GetMapping("/api/candidate/{testLink}")
    public Map<String, Object> getTest(@PathVariable String testLink) {
        Candidate candidate = candidateRepository.findByTestLink(testLink)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid test link"));
        if ("completed".equals(candidate.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This test has already been completed");
        }

        Test test = testRepository.findById(candidate.getTestId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test not found"));

        List<Question> questions = new ArrayList<>(questionRepository.findByTestIdOrderByOrderAsc(test.getId()));
        if (Boolean.TRUE.equals(test.getShuffleQuestions())) {
            Collections.shuffle(questions);
        }

        List<Map<String, Object>> questionDtos = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", q.getId());
            dto.put("type", q.getType());
            dto.put("content", q.getContent());
            dto.put("codeSnippet", q.getCodeSnippet());
            if ("multipleChoice".equals(q.getType()) || "patternRecognition".equals(q.getType())) {
                dto.put("options", q.getOptions());
            }
            if ("coding".equals(q.getType())) dto.put("testCases", q.getTestCases());
            if ("subjective".equals(q.getType())) dto.put("evaluationGuidelines", q.getEvaluationGuidelines());
            if ("patternRecognition".equals(q.getType())) dto.put("imageUrl", q.getImageUrl());
            questionDtos.add(dto);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidateId", candidate.getId());
        result.put("candidateName", candidate.getName());
        result.put("testId", test.getId());
        result.put("testTitle", test.getTitle());
        result.put("testDescription", test.getDescription());
        result.put("duration", test.getDuration());
        result.put("startedAt", candidate.getStartedAt() != null ? candidate.getStartedAt().toString() : null);
        result.put("questions", questionDtos);
        return result;
    }

    @GetMapping("/api/candidate/{testLink}/responses/{questionId}")
    public Response getResponse(@PathVariable String testLink, @PathVariable Integer questionId) {
        Candidate candidate = candidateRepository.findByTestLink(testLink)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid test link"));
        return responseRepository.findByCandidateIdAndQuestionId(candidate.getId(), questionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Response not found"));
    }

    @PostMapping("/api/candidate/{testLink}/responses")
    public ResponseEntity<Map<String, Object>> saveResponse(
            @PathVariable String testLink,
            @RequestBody Map<String, Object> body) {
        Candidate candidate = candidateRepository.findByTestLink(testLink)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid test link"));
        if ("completed".equals(candidate.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This test has already been completed");
        }

        if (body.get("questionId") == null || body.get("response") == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing questionId or response");
        }

        Integer questionId = ((Number) body.get("questionId")).intValue();
        String responseText = String.valueOf(body.get("response"));
        Map<String, Object> result = candidateTestService.evaluateAndSaveResponse(candidate, questionId, responseText);

        boolean isNew = result.get("id") != null;
        return isNew ? ResponseEntity.status(HttpStatus.CREATED).body(result) : ResponseEntity.ok(result);
    }

    @PostMapping("/api/candidate/{testLink}/submit")
    public Map<String, Object> submit(
            @PathVariable String testLink,
            @RequestBody(required = false) Map<String, Object> body) {
        Candidate candidate = candidateRepository.findByTestLink(testLink)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid test link"));
        boolean autoSubmitted = body != null && Boolean.TRUE.equals(body.get("autoSubmitted"));
        return candidateTestService.submitCandidateTest(candidate, autoSubmitted);
    }

    @PostMapping("/api/candidate/{testLink}/start")
    public Map<String, Object> start(@PathVariable String testLink, HttpServletRequest request) {
        Candidate candidate = candidateRepository.findByTestLink(testLink)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid test link"));
        if ("completed".equals(candidate.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This test has already been completed");
        }
        if ("in_progress".equals(candidate.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This test is already in progress");
        }

        candidate.setStatus("in_progress");
        candidate.setStartedAt(Instant.now());
        candidate.setIpAddress(ClientIpUtil.getClientIp(request));
        candidate = candidateRepository.save(candidate);
        return Map.of("startedAt", candidate.getStartedAt().toString());
    }

    private Map<String, Object> toCandidateMap(Candidate c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("email", c.getEmail());
        m.put("phone", c.getPhone());
        m.put("testId", c.getTestId());
        m.put("invitedBy", c.getInvitedBy());
        m.put("testLink", c.getTestLink());
        m.put("status", c.getStatus());
        m.put("invitedAt", c.getInvitedAt() != null ? c.getInvitedAt().toString() : null);
        m.put("startedAt", c.getStartedAt() != null ? c.getStartedAt().toString() : null);
        m.put("completedAt", c.getCompletedAt() != null ? c.getCompletedAt().toString() : null);
        m.put("score", c.getScore());
        m.put("autoSubmitted", c.getAutoSubmitted());
        m.put("ipAddress", c.getIpAddress());
        return m;
    }

    private Test requireOwnedTest(Integer testId, UserPrincipal principal) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test not found"));
        if (!test.getCreatedBy().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Unauthorized to invite candidates for this test");
        }
        return test;
    }

    private void requireAuth(UserPrincipal principal) {
        if (principal == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}
