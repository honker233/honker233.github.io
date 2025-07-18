package com.testtools.controller;

import com.testtools.entity.GitRepository;
import com.testtools.repository.GitRepositoryRepo;
import com.testtools.service.GitAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/repositories")
@CrossOrigin(origins = "*")
public class RepositoryController {
    
    @Autowired
    private GitRepositoryRepo gitRepositoryRepo;
    
    @Autowired
    private GitAnalysisService gitAnalysisService;
    
    @Value("${app.git.workspace:./workspace}")
    private String workspace;

    @GetMapping
    public ResponseEntity<List<GitRepository>> getRepositories() {
        List<GitRepository> repositories = gitRepositoryRepo.findAll();
        return ResponseEntity.ok(repositories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GitRepository> getRepository(@PathVariable Long id) {
        Optional<GitRepository> repository = gitRepositoryRepo.findById(id);
        return repository.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<GitRepository> createRepository(@RequestBody GitRepository repository) {
        try {
            repository.setLocalPath(workspace + File.separator + repository.getName());
            repository.setStatus("CREATED");
            repository.setCreatedTime(LocalDateTime.now());
            repository.setUpdatedTime(LocalDateTime.now());
            
            GitRepository saved = gitRepositoryRepo.save(repository);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            System.err.println("Failed to create repository: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<String> cloneRepository(@PathVariable Long id) {
        try {
            Optional<GitRepository> repoOpt = gitRepositoryRepo.findById(id);
            if (!repoOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            GitRepository repository = repoOpt.get();
            repository.setStatus("CLONING");
            gitRepositoryRepo.save(repository);
            
            gitAnalysisService.cloneOrPullRepository(repository);
            
            String latestCommitId = gitAnalysisService.getLatestCommitId(repository);
            repository.setLastCommitId(latestCommitId);
            repository.setStatus("READY");
            repository.setUpdatedTime(LocalDateTime.now());
            
            gitRepositoryRepo.save(repository);
            
            return ResponseEntity.ok("Repository cloned successfully");
        } catch (Exception e) {
            System.err.println("Failed to clone repository: " + e.getMessage());
            
            Optional<GitRepository> repoOpt = gitRepositoryRepo.findById(id);
            if (repoOpt.isPresent()) {
                GitRepository repository = repoOpt.get();
                repository.setStatus("ERROR");
                gitRepositoryRepo.save(repository);
            }
            
            return ResponseEntity.badRequest().body("Failed to clone repository: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/pull")
    public ResponseEntity<String> pullRepository(@PathVariable Long id) {
        try {
            Optional<GitRepository> repoOpt = gitRepositoryRepo.findById(id);
            if (!repoOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            GitRepository repository = repoOpt.get();
            repository.setStatus("UPDATING");
            gitRepositoryRepo.save(repository);
            
            gitAnalysisService.cloneOrPullRepository(repository);
            
            String latestCommitId = gitAnalysisService.getLatestCommitId(repository);
            repository.setLastCommitId(latestCommitId);
            repository.setStatus("READY");
            repository.setUpdatedTime(LocalDateTime.now());
            
            gitRepositoryRepo.save(repository);
            
            return ResponseEntity.ok("Repository updated successfully");
        } catch (Exception e) {
            System.err.println("Failed to pull repository: " + e.getMessage());
            
            Optional<GitRepository> repoOpt = gitRepositoryRepo.findById(id);
            if (repoOpt.isPresent()) {
                GitRepository repository = repoOpt.get();
                repository.setStatus("ERROR");
                gitRepositoryRepo.save(repository);
            }
            
            return ResponseEntity.badRequest().body("Failed to pull repository: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRepository(@PathVariable Long id) {
        try {
            gitRepositoryRepo.deleteById(id);
            return ResponseEntity.ok("Repository deleted successfully");
        } catch (Exception e) {
            System.err.println("Failed to delete repository: " + e.getMessage());
            return ResponseEntity.badRequest().body("Failed to delete repository: " + e.getMessage());
        }
    }
}