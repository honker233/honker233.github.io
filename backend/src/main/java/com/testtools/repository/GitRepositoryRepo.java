package com.testtools.repository;

import com.testtools.entity.GitRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitRepositoryRepo extends JpaRepository<GitRepository, Long> {
    Optional<GitRepository> findByName(String name);
    List<GitRepository> findByStatus(String status);
}