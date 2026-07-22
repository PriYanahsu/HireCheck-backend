package com.hirecheck.repository;

import com.hirecheck.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Integer> {
    Optional<Candidate> findByTestLink(String testLink);
    List<Candidate> findByTestId(Integer testId);
    Optional<Candidate> findByEmailAndTestId(String email, Integer testId);
    void deleteByTestId(Integer testId);

    @Query("SELECT c FROM Candidate c WHERE c.status = 'in_progress' AND c.startedAt IS NOT NULL AND c.completedAt IS NULL")
    List<Candidate> findInProgress();
}
