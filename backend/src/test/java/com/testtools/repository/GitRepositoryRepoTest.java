package com.testtools.repository;

import com.testtools.entity.GitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class GitRepositoryRepoTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GitRepositoryRepo gitRepositoryRepo;

    private GitRepository testRepository1;
    private GitRepository testRepository2;

    @BeforeEach
    void setUp() {
        testRepository1 = new GitRepository();
        testRepository1.setName("test-repo-1");
        testRepository1.setGitUrl("https://github.com/test/repo1.git");
        testRepository1.setLocalPath("./workspace/test-repo-1");
        testRepository1.setBranch("main");
        testRepository1.setStatus("CREATED");
        testRepository1.setCreatedTime(LocalDateTime.now());
        testRepository1.setUpdatedTime(LocalDateTime.now());

        testRepository2 = new GitRepository();
        testRepository2.setName("test-repo-2");
        testRepository2.setGitUrl("https://github.com/test/repo2.git");
        testRepository2.setLocalPath("./workspace/test-repo-2");
        testRepository2.setBranch("develop");
        testRepository2.setStatus("READY");
        testRepository2.setCreatedTime(LocalDateTime.now());
        testRepository2.setUpdatedTime(LocalDateTime.now());
    }

    @Test
    void testSaveAndFindById() {
        // 保存测试仓库
        GitRepository saved = gitRepositoryRepo.save(testRepository1);
        entityManager.flush();

        // 验证保存成功
        assertNotNull(saved.getId());
        assertEquals("test-repo-1", saved.getName());

        // 通过ID查找
        Optional<GitRepository> found = gitRepositoryRepo.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("test-repo-1", found.get().getName());
        assertEquals("https://github.com/test/repo1.git", found.get().getGitUrl());
        assertEquals("main", found.get().getBranch());
        assertEquals("CREATED", found.get().getStatus());
    }

    @Test
    void testFindByName() {
        // 保存测试仓库
        gitRepositoryRepo.save(testRepository1);
        gitRepositoryRepo.save(testRepository2);
        entityManager.flush();

        // 通过名称查找
        Optional<GitRepository> found = gitRepositoryRepo.findByName("test-repo-1");
        assertTrue(found.isPresent());
        assertEquals("test-repo-1", found.get().getName());
        assertEquals("https://github.com/test/repo1.git", found.get().getGitUrl());

        // 查找不存在的名称
        Optional<GitRepository> notFound = gitRepositoryRepo.findByName("non-existent-repo");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testFindByStatus() {
        // 保存测试仓库
        gitRepositoryRepo.save(testRepository1);
        gitRepositoryRepo.save(testRepository2);
        entityManager.flush();

        // 通过状态查找
        List<GitRepository> createdRepos = gitRepositoryRepo.findByStatus("CREATED");
        assertEquals(1, createdRepos.size());
        assertEquals("test-repo-1", createdRepos.get(0).getName());

        List<GitRepository> readyRepos = gitRepositoryRepo.findByStatus("READY");
        assertEquals(1, readyRepos.size());
        assertEquals("test-repo-2", readyRepos.get(0).getName());

        // 查找不存在的状态
        List<GitRepository> errorRepos = gitRepositoryRepo.findByStatus("ERROR");
        assertTrue(errorRepos.isEmpty());
    }

    @Test
    void testFindAll() {
        // 保存测试仓库
        gitRepositoryRepo.save(testRepository1);
        gitRepositoryRepo.save(testRepository2);
        entityManager.flush();

        // 查找所有仓库
        List<GitRepository> allRepos = gitRepositoryRepo.findAll();
        assertEquals(2, allRepos.size());

        // 验证仓库名称
        assertTrue(allRepos.stream().anyMatch(repo -> "test-repo-1".equals(repo.getName())));
        assertTrue(allRepos.stream().anyMatch(repo -> "test-repo-2".equals(repo.getName())));
    }

    @Test
    void testUpdate() {
        // 保存测试仓库
        GitRepository saved = gitRepositoryRepo.save(testRepository1);
        entityManager.flush();

        // 更新仓库信息
        saved.setStatus("READY");
        saved.setLastCommitId("abc123456");
        saved.setUpdatedTime(LocalDateTime.now());

        GitRepository updated = gitRepositoryRepo.save(saved);
        entityManager.flush();

        // 验证更新成功
        Optional<GitRepository> found = gitRepositoryRepo.findById(updated.getId());
        assertTrue(found.isPresent());
        assertEquals("READY", found.get().getStatus());
        assertEquals("abc123456", found.get().getLastCommitId());
    }

    @Test
    void testDelete() {
        // 保存测试仓库
        GitRepository saved = gitRepositoryRepo.save(testRepository1);
        entityManager.flush();

        // 验证仓库存在
        Optional<GitRepository> found = gitRepositoryRepo.findById(saved.getId());
        assertTrue(found.isPresent());

        // 删除仓库
        gitRepositoryRepo.deleteById(saved.getId());
        entityManager.flush();

        // 验证删除成功
        Optional<GitRepository> deleted = gitRepositoryRepo.findById(saved.getId());
        assertFalse(deleted.isPresent());
    }

    @Test
    void testUniqueNameConstraint() {
        // 保存第一个仓库
        gitRepositoryRepo.save(testRepository1);
        entityManager.flush();

        // 尝试保存同名仓库
        GitRepository duplicateRepo = new GitRepository();
        duplicateRepo.setName("test-repo-1"); // 相同的名称
        duplicateRepo.setGitUrl("https://github.com/test/duplicate.git");
        duplicateRepo.setLocalPath("./workspace/duplicate");
        duplicateRepo.setBranch("main");
        duplicateRepo.setStatus("CREATED");

        // 这应该抛出异常，因为名称必须唯一
        assertThrows(Exception.class, () -> {
            gitRepositoryRepo.save(duplicateRepo);
            entityManager.flush();
        });
    }

    @Test
    void testNullableFields() {
        // 测试可为空的字段
        GitRepository minimalRepo = new GitRepository();
        minimalRepo.setName("minimal-repo");
        minimalRepo.setGitUrl("https://github.com/test/minimal.git");
        // localPath, lastCommitId 可以为空

        GitRepository saved = gitRepositoryRepo.save(minimalRepo);
        entityManager.flush();

        // 验证保存成功
        assertNotNull(saved.getId());
        assertEquals("minimal-repo", saved.getName());
        assertEquals("main", saved.getBranch()); // 默认值
        assertEquals("CREATED", saved.getStatus()); // 默认值
        assertNull(saved.getLocalPath());
        assertNull(saved.getLastCommitId());
    }

    @Test
    void testRequiredFields() {
        // 测试必填字段
        GitRepository invalidRepo = new GitRepository();
        // 不设置 name 和 gitUrl

        // 这应该抛出异常，因为 name 和 gitUrl 是必填的
        assertThrows(Exception.class, () -> {
            gitRepositoryRepo.save(invalidRepo);
            entityManager.flush();
        });
    }

    @Test
    void testBranchAndStatusDefaults() {
        // 测试默认值
        GitRepository repoWithDefaults = new GitRepository();
        repoWithDefaults.setName("default-test-repo");
        repoWithDefaults.setGitUrl("https://github.com/test/default.git");

        GitRepository saved = gitRepositoryRepo.save(repoWithDefaults);
        entityManager.flush();

        // 验证默认值
        assertEquals("main", saved.getBranch());
        assertEquals("CREATED", saved.getStatus());
        assertNotNull(saved.getCreatedTime());
        assertNotNull(saved.getUpdatedTime());
    }

    @Test
    void testTimestampFields() {
        // 测试时间戳字段
        LocalDateTime beforeSave = LocalDateTime.now().minusSeconds(1);
        
        GitRepository saved = gitRepositoryRepo.save(testRepository1);
        entityManager.flush();

        LocalDateTime afterSave = LocalDateTime.now().plusSeconds(1);

        // 验证时间戳在合理范围内
        assertTrue(saved.getCreatedTime().isAfter(beforeSave));
        assertTrue(saved.getCreatedTime().isBefore(afterSave));
        assertTrue(saved.getUpdatedTime().isAfter(beforeSave));
        assertTrue(saved.getUpdatedTime().isBefore(afterSave));
    }

    @Test
    void testFieldLengthValidation() {
        // 测试字段长度验证
        GitRepository repoWithLongFields = new GitRepository();
        repoWithLongFields.setName("test-repo-with-very-long-name");
        repoWithLongFields.setGitUrl("https://github.com/test/" + "a".repeat(450) + ".git");
        repoWithLongFields.setLocalPath("./workspace/" + "b".repeat(450));
        repoWithLongFields.setBranch("feature/" + "c".repeat(80));
        repoWithLongFields.setLastCommitId("d".repeat(90));
        repoWithLongFields.setStatus("CREATED");

        // 这应该能够成功保存，因为字段长度在限制范围内
        assertDoesNotThrow(() -> {
            GitRepository saved = gitRepositoryRepo.save(repoWithLongFields);
            entityManager.flush();
            assertNotNull(saved.getId());
        });
    }
}