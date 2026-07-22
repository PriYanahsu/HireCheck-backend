package com.hirecheck.service;

import com.hirecheck.entity.Candidate;
import com.hirecheck.entity.Test;
import com.hirecheck.repository.CandidateRepository;
import com.hirecheck.repository.TestRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class StatsService {
    private final TestRepository testRepository;
    private final CandidateRepository candidateRepository;

    public StatsService(TestRepository testRepository, CandidateRepository candidateRepository) {
        this.testRepository = testRepository;
        this.candidateRepository = candidateRepository;
    }

    public Map<String, Object> getTestStats(Integer testId) {
        List<Candidate> candidates = candidateRepository.findByTestId(testId);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", candidates.size());
        stats.put("completed", candidates.stream().filter(c -> "completed".equals(c.getStatus())).count());
        stats.put("inProgress", candidates.stream().filter(c -> "in_progress".equals(c.getStatus())).count());
        stats.put("pending", candidates.stream().filter(c -> "pending".equals(c.getStatus())).count());
        stats.put("avgScore", candidates.stream()
                .filter(c -> "completed".equals(c.getStatus()) && c.getScore() != null)
                .mapToInt(Candidate::getScore).average().orElse(0));
        return stats;
    }

    public Map<String, Object> getDashboardStats(Integer userId) {
        List<Test> tests = testRepository.findByCreatedBy(userId);
        int pending = 0, completed = 0;
        for (Test t : tests) {
            for (Candidate c : candidateRepository.findByTestId(t.getId())) {
                if ("completed".equals(c.getStatus())) completed++;
                else pending++;
            }
        }
        return Map.of("activeTests", tests.size(), "pendingAssessments", pending, "completedTests", completed);
    }

    public List<Map<String, Object>> getTestPerformance(Integer userId, int limit) {
        List<Map<String, Object>> performance = new ArrayList<>();
        for (Test test : testRepository.findByCreatedBy(userId)) {
            Map<String, Object> stats = getTestStats(test.getId());
            double avgScore = ((Number) stats.get("avgScore")).doubleValue();
            long completed = ((Number) stats.get("completed")).longValue();
            if (completed > 0 && avgScore > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("testId", test.getId());
                item.put("name", test.getTitle());
                item.put("score", (int) Math.round(avgScore));
                item.put("completed", completed);
                performance.add(item);
            }
        }
        performance.sort((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score")));
        return performance.stream().limit(limit).toList();
    }

    public List<Map<String, Object>> getRecentActivity(Integer userId, int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();
        for (Test test : testRepository.findByCreatedBy(userId)) {
            for (Candidate c : candidateRepository.findByTestId(test.getId())) {
                if ("completed".equals(c.getStatus()) && c.getCompletedAt() != null) {
                    activities.add(activity(c, test, "completed", c.getCompletedAt().toString(), c.getScore(), c.getAutoSubmitted()));
                }
                if (c.getStartedAt() != null) {
                    activities.add(activity(c, test, "started", c.getStartedAt().toString(), null, null));
                }
            }
        }
        activities.sort((a, b) -> ((String) b.get("timestamp")).compareTo((String) a.get("timestamp")));
        return activities.stream().limit(limit).toList();
    }

    private Map<String, Object> activity(Candidate c, Test t, String action, String ts, Integer score, Boolean auto) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("candidateId", c.getId());
        m.put("candidateName", c.getName());
        m.put("testId", t.getId());
        m.put("testTitle", t.getTitle());
        m.put("action", action);
        m.put("timestamp", ts);
        if (score != null) m.put("score", score);
        if (auto != null) m.put("autoSubmitted", auto);
        return m;
    }
}
