package com.testtools.repository;

import com.testtools.entity.CodeChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeChangeRepo extends JpaRepository<CodeChange, Long> {
    List<CodeChange> findByRepositoryId(Long repositoryId);
    List<CodeChange> findByRepositoryIdAndCommitId(Long repositoryId, String commitId);
}