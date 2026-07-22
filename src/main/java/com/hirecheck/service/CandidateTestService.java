package com.hirecheck.service;

import com.hirecheck.entity.Candidate;
import com.hirecheck.entity.Question;
import com.hirecheck.entity.Response;
import com.hirecheck.exception.ApiException;
import com.hirecheck.repository.CandidateRepository;
import com.hirecheck.repository.QuestionRepository;
import com.hirecheck.repository.ResponseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class CandidateTestService {
    private final CandidateRepository candidateRepository;
    private final QuestionRepository questionRepository;
    private final ResponseRepository responseRepository;

    public CandidateTestService(CandidateRepository candidateRepository,
            QuestionRepository questionRepository, ResponseRepository responseRepository) {
        this.candidateRepository = candidateRepository;
        this.questionRepository = questionRepository;
        this.responseRepository = responseRepository;
    }

    @Transactional
    public Map<String, Object> submitCandidateTest(Candidate candidate, boolean autoSubmitted) {
        if ("completed".equals(candidate.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This test has already been completed");
        }
        List<Response> responses = responseRepository.findByCandidateId(candidate.getId());
        List<Question> questions = questionRepository.findByTestIdOrderByOrderAsc(candidate.getTestId());
        int totalScore = 0, totalPoints = 0;
        for (Question q : questions) {
            int pts = q.getPoints() != null ? q.getPoints() : 1;
            totalPoints += pts;
            totalScore += responses.stream().filter(r -> r.getQuestionId().equals(q.getId()))
                    .map(r -> r.getPoints() != null ? r.getPoints() : 0).findFirst().orElse(0);
        }
        int finalScore = totalPoints > 0 ? Math.round((totalScore * 100f) / totalPoints) : 0;
        candidate.setStatus("completed");
        candidate.setCompletedAt(Instant.now());
        candidate.setAutoSubmitted(autoSubmitted);
        candidate.setScore(finalScore);
        candidateRepository.save(candidate);
        return Map.of("message", "Test submitted successfully", "score", finalScore,
                "totalScore", totalScore, "totalPoints", totalPoints);
    }

    public Map<String, Object> evaluateAndSaveResponse(Candidate candidate, Integer questionId, String responseText) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found or doesn't belong to this test"));
        if (!question.getTestId().equals(candidate.getTestId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Question not found or doesn't belong to this test");
        }
        final boolean isCorrect;
        final int points;
        if ("multipleChoice".equals(question.getType()) || "patternRecognition".equals(question.getType())) {
            isCorrect = String.valueOf(responseText).equals(String.valueOf(question.getAnswer()));
            points = isCorrect ? (question.getPoints() != null ? question.getPoints() : 1) : 0;
        } else {
            isCorrect = false;
            points = 0;
        }
        Response saved = responseRepository.findByCandidateIdAndQuestionId(candidate.getId(), questionId)
                .map(r -> { r.setResponse(responseText); r.setIsCorrect(isCorrect); r.setPoints(points); return responseRepository.save(r); })
                .orElseGet(() -> {
                    Response r = new Response();
                    r.setCandidateId(candidate.getId());
                    r.setQuestionId(questionId);
                    r.setResponse(responseText);
                    r.setIsCorrect(isCorrect);
                    r.setPoints(points);
                    return responseRepository.save(r);
                });
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("candidateId", saved.getCandidateId());
        result.put("questionId", saved.getQuestionId());
        result.put("response", saved.getResponse());
        result.put("submittedAt", saved.getSubmittedAt() != null ? saved.getSubmittedAt().toString() : null);
        return result;
    }
}
