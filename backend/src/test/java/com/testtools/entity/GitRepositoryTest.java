package com.testtools.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class GitRepositoryTest {

    private Validator validator;
    private GitRepository gitRepository;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        gitRepository = new GitRepository();
        gitRepository.setName("test-repository");
        gitRepository.setGitUrl("https://github.com/test/repository.git");
        gitRepository.setLocalPath("./workspace/test-repository");
        gitRepository.setBranch("main");
        gitRepository.setStatus("CREATED");
        gitRepository.setCreatedTime(LocalDateTime.now());
        gitRepository.setUpdatedTime(LocalDateTime.now());
    }

    @Test
    void testValidGitRepository() {
        Set<ConstraintViolation<GitRepository>> violations = validator.validate(gitRepository);
        assertTrue(violations.isEmpty(), "Valid GitRepository should have no violations");
    }

    @Test
    void testGettersAndSetters() {
        // 测试所有getter和setter
        Long testId = 123L;
        gitRepository.setId(testId);
        assertEquals(testId, gitRepository.getId());

        String testName = "new-repo-name";
        gitRepository.setName(testName);
        assertEquals(testName, gitRepository.getName());

        String testGitUrl = "https://github.com/new/repo.git";
        gitRepository.setGitUrl(testGitUrl);
        assertEquals(testGitUrl, gitRepository.getGitUrl());

        String testLocalPath = "./new/local/path";
        gitRepository.setLocalPath(testLocalPath);
        assertEquals(testLocalPath, gitRepository.getLocalPath());

        String testBranch = "develop";
        gitRepository.setBranch(testBranch);
        assertEquals(testBranch, gitRepository.getBranch());

        String testCommitId = "abc123def456";
        gitRepository.setLastCommitId(testCommitId);
        assertEquals(testCommitId, gitRepository.getLastCommitId());

        String testStatus = "READY";
        gitRepository.setStatus(testStatus);
        assertEquals(testStatus, gitRepository.getStatus());

        LocalDateTime testCreatedTime = LocalDateTime.now().minusDays(1);
        gitRepository.setCreatedTime(testCreatedTime);
        assertEquals(testCreatedTime, gitRepository.getCreatedTime());

        LocalDateTime testUpdatedTime = LocalDateTime.now();
        gitRepository.setUpdatedTime(testUpdatedTime);
        assertEquals(testUpdatedTime, gitRepository.getUpdatedTime());
    }

    @Test
    void testDefaultValues() {
        GitRepository newRepo = new GitRepository();
        
        // 测试默认值
        assertEquals("main", newRepo.getBranch());
        assertEquals("CREATED", newRepo.getStatus());
        assertNotNull(newRepo.getCreatedTime());
        assertNotNull(newRepo.getUpdatedTime());
    }

    @Test
    void testNameValidation() {
        // 测试名称为空的情况
        gitRepository.setName(null);
        Set<ConstraintViolation<GitRepository>> violations = validator.validate(gitRepository);
        assertFalse(violations.isEmpty(), "Name should not be null");

        // 测试名称为空字符串的情况
        gitRepository.setName("");
        violations = validator.validate(gitRepository);
        assertFalse(violations.isEmpty(), "Name should not be empty");
    }

    @Test
    void testGitUrlValidation() {
        // 测试Git URL为空的情况
        gitRepository.setGitUrl(null);
        Set<ConstraintViolation<GitRepository>> violations = validator.validate(gitRepository);
        assertFalse(violations.isEmpty(), "Git URL should not be null");

        // 测试Git URL为空字符串的情况
        gitRepository.setGitUrl("");
        violations = validator.validate(gitRepository);
        assertFalse(violations.isEmpty(), "Git URL should not be empty");
    }

    @Test
    void testValidStatusValues() {
        String[] validStatuses = {"CREATED", "CLONING", "UPDATING", "READY", "ERROR"};
        
        for (String status : validStatuses) {
            gitRepository.setStatus(status);
            assertEquals(status, gitRepository.getStatus());
        }
    }

    @Test
    void testValidBranchNames() {
        String[] validBranches = {
            "main", "master", "develop", "feature/new-feature", 
            "bugfix/fix-issue", "release/1.0.0", "hotfix/urgent-fix"
        };
        
        for (String branch : validBranches) {
            gitRepository.setBranch(branch);
            assertEquals(branch, gitRepository.getBranch());
        }
    }

    @Test
    void testValidGitUrls() {
        String[] validUrls = {
            "https://github.com/user/repo.git",
            "https://gitlab.com/user/repo.git",
            "https://bitbucket.org/user/repo.git",
            "git@github.com:user/repo.git",
            "ssh://git@gitlab.com:2222/user/repo.git",
            "file:///local/path/to/repo.git"
        };
        
        for (String url : validUrls) {
            gitRepository.setGitUrl(url);
            assertEquals(url, gitRepository.getGitUrl());
        }
    }

    @Test
    void testLocalPathVariations() {
        String[] validPaths = {
            "./workspace/repo",
            "/absolute/path/to/repo",
            "relative/path/to/repo",
            "C:\\Windows\\Path\\To\\Repo",
            "/home/user/repositories/repo"
        };
        
        for (String path : validPaths) {
            gitRepository.setLocalPath(path);
            assertEquals(path, gitRepository.getLocalPath());
        }
    }

    @Test
    void testCommitIdFormats() {
        String[] validCommitIds = {
            "abc123def456", // 短格式
            "abc123def456789012345678901234567890abcd", // 完整SHA-1
            "1234567890abcdef1234567890abcdef12345678", // 另一个完整SHA-1
            "a1b2c3d4e5f6", // 短SHA
            null // 可以为空
        };
        
        for (String commitId : validCommitIds) {
            gitRepository.setLastCommitId(commitId);
            assertEquals(commitId, gitRepository.getLastCommitId());
        }
    }

    @Test
    void testTimestampHandling() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusDays(30);
        LocalDateTime future = now.plusDays(1);
        
        // 测试过去的时间
        gitRepository.setCreatedTime(past);
        gitRepository.setUpdatedTime(past);
        assertEquals(past, gitRepository.getCreatedTime());
        assertEquals(past, gitRepository.getUpdatedTime());
        
        // 测试未来的时间
        gitRepository.setCreatedTime(future);
        gitRepository.setUpdatedTime(future);
        assertEquals(future, gitRepository.getCreatedTime());
        assertEquals(future, gitRepository.getUpdatedTime());
        
        // 测试null值
        gitRepository.setCreatedTime(null);
        gitRepository.setUpdatedTime(null);
        assertNull(gitRepository.getCreatedTime());
        assertNull(gitRepository.getUpdatedTime());
    }

    @Test
    void testRepositoryEquality() {
        GitRepository repo1 = new GitRepository();
        repo1.setId(1L);
        repo1.setName("test-repo");
        repo1.setGitUrl("https://github.com/test/repo.git");
        
        GitRepository repo2 = new GitRepository();
        repo2.setId(1L);
        repo2.setName("test-repo");
        repo2.setGitUrl("https://github.com/test/repo.git");
        
        // 注意：由于没有重写equals和hashCode，这里比较的是对象引用
        assertNotEquals(repo1, repo2);
        assertEquals(repo1.getId(), repo2.getId());
        assertEquals(repo1.getName(), repo2.getName());
        assertEquals(repo1.getGitUrl(), repo2.getGitUrl());
    }

    @Test
    void testRepositoryCloning() {
        // 测试仓库对象的复制
        GitRepository original = gitRepository;
        GitRepository clone = new GitRepository();
        
        clone.setId(original.getId());
        clone.setName(original.getName());
        clone.setGitUrl(original.getGitUrl());
        clone.setLocalPath(original.getLocalPath());
        clone.setBranch(original.getBranch());
        clone.setLastCommitId(original.getLastCommitId());
        clone.setStatus(original.getStatus());
        clone.setCreatedTime(original.getCreatedTime());
        clone.setUpdatedTime(original.getUpdatedTime());
        
        assertEquals(original.getId(), clone.getId());
        assertEquals(original.getName(), clone.getName());
        assertEquals(original.getGitUrl(), clone.getGitUrl());
        assertEquals(original.getLocalPath(), clone.getLocalPath());
        assertEquals(original.getBranch(), clone.getBranch());
        assertEquals(original.getLastCommitId(), clone.getLastCommitId());
        assertEquals(original.getStatus(), clone.getStatus());
        assertEquals(original.getCreatedTime(), clone.getCreatedTime());
        assertEquals(original.getUpdatedTime(), clone.getUpdatedTime());
    }

    @Test
    void testNullSafeOperations() {
        GitRepository emptyRepo = new GitRepository();
        
        // 确保在字段为null时不会抛出异常
        assertDoesNotThrow(() -> {
            emptyRepo.getId();
            emptyRepo.getName();
            emptyRepo.getGitUrl();
            emptyRepo.getLocalPath();
            emptyRepo.getBranch(); // 有默认值
            emptyRepo.getLastCommitId();
            emptyRepo.getStatus(); // 有默认值
            emptyRepo.getCreatedTime(); // 有默认值
            emptyRepo.getUpdatedTime(); // 有默认值
        });
    }

    @Test
    void testFieldLengthLimits() {
        // 测试字段长度限制（基于@Column注解）
        
        // gitUrl最大500字符
        String longUrl = "https://github.com/test/" + "a".repeat(500) + ".git";
        gitRepository.setGitUrl(longUrl);
        assertEquals(longUrl, gitRepository.getGitUrl());
        
        // localPath最大500字符  
        String longPath = "./workspace/" + "b".repeat(490);
        gitRepository.setLocalPath(longPath);
        assertEquals(longPath, gitRepository.getLocalPath());
        
        // branch最大100字符
        String longBranch = "feature/" + "c".repeat(90);
        gitRepository.setBranch(longBranch);
        assertEquals(longBranch, gitRepository.getBranch());
        
        // lastCommitId最大100字符
        String longCommitId = "d".repeat(100);
        gitRepository.setLastCommitId(longCommitId);
        assertEquals(longCommitId, gitRepository.getLastCommitId());
        
        // status最大50字符
        String longStatus = "CUSTOM_STATUS_" + "e".repeat(35);
        gitRepository.setStatus(longStatus);
        assertEquals(longStatus, gitRepository.getStatus());
    }
}