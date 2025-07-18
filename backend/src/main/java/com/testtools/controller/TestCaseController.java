package com.testtools.controller;

import com.testtools.entity.TestCase;
import com.testtools.repository.TestCaseRepo;
import com.testtools.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/testcases")
@CrossOrigin(origins = "*")
public class TestCaseController {
    
    @Autowired
    private TestCaseRepo testCaseRepo;
    
    @Autowired
    private TestCaseService testCaseService;

    @GetMapping
    public ResponseEntity<List<TestCase>> getTestCases(@RequestParam(required = false) Long repositoryId) {
        List<TestCase> testCases;
        if (repositoryId != null) {
            testCases = testCaseRepo.findByRepositoryId(repositoryId);
        } else {
            testCases = testCaseRepo.findAll();
        }
        return ResponseEntity.ok(testCases);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestCase> getTestCase(@PathVariable Long id) {
        Optional<TestCase> testCase = testCaseRepo.findById(id);
        return testCase.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadTestCases(
            @RequestParam("file") MultipartFile file,
            @RequestParam("repositoryId") Long repositoryId) {
        
        try {
            List<TestCase> testCases = testCaseService.parseTestCasesFromFile(file, repositoryId);
            
            for (TestCase testCase : testCases) {
                testCaseRepo.save(testCase);
            }
            
            return ResponseEntity.ok("Uploaded " + testCases.size() + " test cases successfully");
        } catch (Exception e) {
            System.err.println("Failed to upload test cases: " + e.getMessage());
            return ResponseEntity.badRequest().body("Failed to upload test cases: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<TestCase> createTestCase(@RequestBody TestCase testCase) {
        try {
            testCase.setCreatedTime(LocalDateTime.now());
            testCase.setUpdatedTime(LocalDateTime.now());
            TestCase saved = testCaseRepo.save(testCase);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            System.err.println("Failed to create test case: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestCase> updateTestCase(@PathVariable Long id, @RequestBody TestCase testCase) {
        try {
            testCase.setId(id);
            testCase.setUpdatedTime(LocalDateTime.now());
            TestCase saved = testCaseRepo.save(testCase);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            System.err.println("Failed to update test case: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTestCase(@PathVariable Long id) {
        try {
            testCaseRepo.deleteById(id);
            return ResponseEntity.ok("Test case deleted successfully");
        } catch (Exception e) {
            System.err.println("Failed to delete test case: " + e.getMessage());
            return ResponseEntity.badRequest().body("Failed to delete test case: " + e.getMessage());
        }
    }
}