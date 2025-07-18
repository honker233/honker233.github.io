package com.testtools.controller;

import com.testtools.dto.TestCaseRecommendation;
import com.testtools.entity.CodeChange;
import com.testtools.entity.GitRepository;
import com.testtools.entity.TestCase;
import com.testtools.repository.CodeChangeRepo;
import com.testtools.repository.GitRepositoryRepo;
import com.testtools.repository.TestCaseRepo;
import com.testtools.service.GitAnalysisService;
import com.testtools.service.TestCaseRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class AnalysisController {
    
    @Autowired
    private GitAnalysisService gitAnalysisService;
    
    @Autowired
    private TestCaseRecommendationService recommendationService;
    
    @Autowired
    private GitRepositoryRepo gitRepositoryRepo;
    
    @Autowired
    private TestCaseRepo testCaseRepo;
    
    @Autowired
    private CodeChangeRepo codeChangeRepo;

    @PostMapping("/git-changes")
    public ResponseEntity<?> analyzeGitChanges(@RequestBody Map<String, Object> request) {
        try {
            Long repositoryId = Long.valueOf(request.get("repositoryId").toString());
            String fromCommit = request.get("fromCommit").toString();
            String toCommit = request.get("toCommit").toString();
            
            Optional<GitRepository> repoOpt = gitRepositoryRepo.findById(repositoryId);
            if (!repoOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Repository not found");
            }
            
            GitRepository repository = repoOpt.get();
            if (!"READY".equals(repository.getStatus())) {
                return ResponseEntity.badRequest().body("Repository is not ready for analysis");
            }
            
            List<CodeChange> codeChanges = gitAnalysisService.analyzeCodeChanges(
                    repository, fromCommit, toCommit);
            
            for (CodeChange codeChange : codeChanges) {
                codeChangeRepo.save(codeChange);
            }
            
            return ResponseEntity.ok(codeChanges);
            
        } catch (Exception e) {
            System.err.println("Failed to analyze git changes: " + e.getMessage());
            return ResponseEntity.badRequest().body("Failed to analyze git changes: " + e.getMessage());
        }
    }

    @PostMapping("/recommend-testcases")
    public ResponseEntity<?> recommendTestCases(@RequestBody Map<String, Object> request) {
        try {
            Long repositoryId = Long.valueOf(request.get("repositoryId").toString());
            String fromCommit = request.get("fromCommit").toString();
            String toCommit = request.get("toCommit").toString();
            
            // 查找或创建代码变更
            List<CodeChange> codeChanges = codeChangeRepo.findByRepositoryIdAndCommitId(repositoryId, toCommit);
            
            if (codeChanges.isEmpty()) {
                Optional<GitRepository> repoOpt = gitRepositoryRepo.findById(repositoryId);
                if (!repoOpt.isPresent()) {
                    return ResponseEntity.badRequest().body("Repository not found");
                }
                
                GitRepository repository = repoOpt.get();
                codeChanges = gitAnalysisService.analyzeCodeChanges(repository, fromCommit, toCommit);
                
                for (CodeChange codeChange : codeChanges) {
                    codeChangeRepo.save(codeChange);
                }
            }
            
            List<TestCase> testCases = testCaseRepo.findByRepositoryId(repositoryId);
            
            List<TestCaseRecommendation> recommendations = recommendationService
                    .recommendTestCases(codeChanges, testCases);
            
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            System.err.println("Failed to recommend test cases: " + e.getMessage());
            return ResponseEntity.badRequest().body("Failed to recommend test cases: " + e.getMessage());
        }
    }

    @GetMapping("/code-changes")
    public ResponseEntity<List<CodeChange>> getCodeChanges(
            @RequestParam Long repositoryId,
            @RequestParam(required = false) String commitId) {
        
        List<CodeChange> codeChanges;
        if (commitId != null) {
            codeChanges = codeChangeRepo.findByRepositoryIdAndCommitId(repositoryId, commitId);
        } else {
            codeChanges = codeChangeRepo.findByRepositoryId(repositoryId);
        }
        
        return ResponseEntity.ok(codeChanges);
    }
}