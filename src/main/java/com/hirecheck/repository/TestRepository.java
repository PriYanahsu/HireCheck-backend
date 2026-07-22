package com.hirecheck.repository;

import com.hirecheck.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestRepository extends JpaRepository<Test, Integer> {
    List<Test> findByCreatedBy(Integer createdBy);
}
