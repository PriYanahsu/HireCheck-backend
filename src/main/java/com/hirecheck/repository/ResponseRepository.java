package com.hirecheck.repository;

import com.hirecheck.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResponseRepository extends JpaRepository<Response, Integer> {
    List<Response> findByCandidateId(Integer candidateId);
    Optional<Response> findByCandidateIdAndQuestionId(Integer candidateId, Integer questionId);
    void deleteByCandidateIdIn(List<Integer> candidateIds);
}
