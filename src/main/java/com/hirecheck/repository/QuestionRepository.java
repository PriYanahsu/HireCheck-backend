package com.hirecheck.repository;

import com.hirecheck.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByTestIdOrderByOrderAsc(Integer testId);
    void deleteByTestId(Integer testId);
    long countByTestId(Integer testId);
}
