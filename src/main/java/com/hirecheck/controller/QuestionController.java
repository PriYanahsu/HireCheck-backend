package com.hirecheck.controller;

import com.hirecheck.entity.Question;
import com.hirecheck.entity.Test;
import com.hirecheck.exception.ApiException;
import com.hirecheck.repository.QuestionRepository;
import com.hirecheck.repository.TestRepository;
import com.hirecheck.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;

    public QuestionController(QuestionRepository questionRepository, TestRepository testRepository) {
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
    }

    @PostMapping
    public ResponseEntity<Question> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        Integer testId = ((Number) body.get("testId")).intValue();
        requireOwnedTest(testId, principal);

        Question q = mapQuestion(body, new Question());
        q.setTestId(testId);
        return ResponseEntity.status(HttpStatus.CREATED).body(questionRepository.save(q));
    }

    @PutMapping("/{id}")
    public Question update(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found"));
        requireOwnedTest(question.getTestId(), principal);
        return questionRepository.save(mapQuestion(body, question));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Integer id, @AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found"));
        requireOwnedTest(question.getTestId(), principal);
        questionRepository.deleteById(id);
        return Map.of("message", "Question deleted successfully");
    }

    private Question mapQuestion(Map<String, Object> body, Question q) {
        if (body.containsKey("type")) q.setType((String) body.get("type"));
        if (body.containsKey("content")) q.setContent((String) body.get("content"));
        if (body.containsKey("codeSnippet")) q.setCodeSnippet((String) body.get("codeSnippet"));
        if (body.containsKey("options")) q.setOptions(toStringList(body.get("options")));
        if (body.containsKey("answer")) q.setAnswer(body.get("answer") != null ? String.valueOf(body.get("answer")) : null);
        if (body.containsKey("testCases")) q.setTestCases(toTestCases(body.get("testCases")));
        if (body.containsKey("evaluationGuidelines")) q.setEvaluationGuidelines((String) body.get("evaluationGuidelines"));
        if (body.containsKey("imageUrl")) q.setImageUrl((String) body.get("imageUrl"));
        if (body.containsKey("points")) q.setPoints(((Number) body.get("points")).intValue());
        if (body.containsKey("order")) q.setOrder(((Number) body.get("order")).intValue());
        return q;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "options must be an array of strings");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> toTestCases(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list) {
            return list.stream().map(item -> {
                if (!(item instanceof Map<?, ?> map)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "testCases must be an array of objects");
                }
                return Map.of(
                        "input", String.valueOf(map.get("input")),
                        "output", String.valueOf(map.get("output"))
                );
            }).toList();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "testCases must be an array");
    }

    private Test requireOwnedTest(Integer testId, UserPrincipal principal) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test not found"));
        if (!test.getCreatedBy().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Unauthorized to add questions to this test");
        }
        return test;
    }

    private void requireAuth(UserPrincipal principal) {
        if (principal == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}
