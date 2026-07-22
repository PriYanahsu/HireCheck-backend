package com.hirecheck.scheduler;

import com.hirecheck.entity.Candidate;
import com.hirecheck.entity.Test;
import com.hirecheck.repository.CandidateRepository;
import com.hirecheck.repository.TestRepository;
import com.hirecheck.service.CandidateTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class AutoSubmitScheduler {
    private static final Logger log = LoggerFactory.getLogger(AutoSubmitScheduler.class);
    private final CandidateRepository candidateRepository;
    private final TestRepository testRepository;
    private final CandidateTestService candidateTestService;

    public AutoSubmitScheduler(CandidateRepository candidateRepository, TestRepository testRepository,
            CandidateTestService candidateTestService) {
        this.candidateRepository = candidateRepository;
        this.testRepository = testRepository;
        this.candidateTestService = candidateTestService;
    }

    @Scheduled(fixedRate = 60_000)
    public void autoSubmitExpiredTests() {
        try {
            Instant now = Instant.now();
            for (Candidate candidate : candidateRepository.findInProgress()) {
                Test test = testRepository.findById(candidate.getTestId()).orElse(null);
                if (test == null || candidate.getStartedAt() == null) continue;
                Instant deadline = candidate.getStartedAt().plus(test.getDuration() + 5L, ChronoUnit.MINUTES);
                if (now.isAfter(deadline)) {
                    candidateTestService.submitCandidateTest(candidate, true);
                    log.info("Auto-submitted candidate {}", candidate.getId());
                }
            }
        } catch (Exception ex) {
            log.error("Auto-submit job error", ex);
        }
    }
}
