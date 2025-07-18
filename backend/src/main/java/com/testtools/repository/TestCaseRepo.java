package com.testtools.repository;

import com.testtools.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepo extends JpaRepository<TestCase, Long> {
    List<TestCase> findByRepositoryId(Long repositoryId);
    List<TestCase> findByRepositoryIdAndCaseType(Long repositoryId, String caseType);
}