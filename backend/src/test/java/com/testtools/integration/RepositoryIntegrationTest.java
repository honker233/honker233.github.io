package com.testtools.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testtools.entity.GitRepository;
import com.testtools.repository.GitRepositoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureWebMvc
@Transactional
public class RepositoryIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private GitRepositoryRepo gitRepositoryRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private GitRepository testRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        testRepository = new GitRepository();
        testRepository.setName("integration-test-repo");
        testRepository.setGitUrl("https://github.com/test/integration-repo.git");
        testRepository.setBranch("main");
        testRepository.setStatus("CREATED");
        testRepository.setCreatedTime(LocalDateTime.now());
        testRepository.setUpdatedTime(LocalDateTime.now());
    }

    @Test
    void testFullRepositoryLifecycle() throws Exception {
        // 1. 创建仓库
        String repositoryJson = objectMapper.writeValueAsString(testRepository);
        
        String response = mockMvc.perform(post("/api/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(repositoryJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("integration-test-repo"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        GitRepository createdRepo = objectMapper.readValue(response, GitRepository.class);
        Long repoId = createdRepo.getId();
        assertNotNull(repoId);

        // 2. 获取创建的仓库
        mockMvc.perform(get("/api/repositories/" + repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(repoId))
                .andExpect(jsonPath("$.name").value("integration-test-repo"))
                .andExpect(jsonPath("$.gitUrl").value("https://github.com/test/integration-repo.git"));

        // 3. 获取所有仓库列表
        mockMvc.perform(get("/api/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(repoId));

        // 4. 尝试克隆仓库（会失败，因为URL不存在，但测试API调用）
        mockMvc.perform(post("/api/repositories/" + repoId + "/clone"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Failed to clone repository")));

        // 5. 尝试拉取仓库（会失败，因为仓库未克隆）
        mockMvc.perform(post("/api/repositories/" + repoId + "/pull"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Failed to pull repository")));

        // 6. 删除仓库
        mockMvc.perform(delete("/api/repositories/" + repoId))
                .andExpect(status().isOk())
                .andExpect(content().string("Repository deleted successfully"));

        // 7. 验证仓库已删除
        mockMvc.perform(get("/api/repositories/" + repoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testRepositoryValidation() throws Exception {
        // 测试无效的仓库数据
        GitRepository invalidRepo = new GitRepository();
        // 不设置必需字段

        String invalidJson = objectMapper.writeValueAsString(invalidRepo);

        mockMvc.perform(post("/api/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDuplicateRepositoryName() throws Exception {
        // 首先创建一个仓库
        GitRepository saved = gitRepositoryRepo.save(testRepository);
        
        // 尝试创建同名仓库
        GitRepository duplicateRepo = new GitRepository();
        duplicateRepo.setName("integration-test-repo"); // 相同名称
        duplicateRepo.setGitUrl("https://github.com/test/another-repo.git");
        duplicateRepo.setBranch("main");

        String duplicateJson = objectMapper.writeValueAsString(duplicateRepo);

        mockMvc.perform(post("/api/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRepositoryNotFound() throws Exception {
        // 测试访问不存在的仓库
        mockMvc.perform(get("/api/repositories/99999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/repositories/99999/clone"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/repositories/99999/pull"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/repositories/99999"))
                .andExpect(status().isOk()); // 删除不存在的资源通常返回200
    }

    @Test
    void testRepositoryStatusUpdates() throws Exception {
        // 创建仓库
        GitRepository saved = gitRepositoryRepo.save(testRepository);
        Long repoId = saved.getId();

        // 验证初始状态
        mockMvc.perform(get("/api/repositories/" + repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"));

        // 模拟状态变化（通过数据库直接更新，因为Git操作会失败）
        saved.setStatus("CLONING");
        gitRepositoryRepo.save(saved);

        mockMvc.perform(get("/api/repositories/" + repoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLONING"));
    }

    @Test
    void testRepositoryBranchSupport() throws Exception {
        // 测试不同分支的仓库创建
        String[] branches = {"main", "master", "develop", "feature/test", "release/1.0"};

        for (int i = 0; i < branches.length; i++) {
            GitRepository repo = new GitRepository();
            repo.setName("test-repo-branch-" + i);
            repo.setGitUrl("https://github.com/test/repo-" + i + ".git");
            repo.setBranch(branches[i]);

            String repoJson = objectMapper.writeValueAsString(repo);

            mockMvc.perform(post("/api/repositories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(repoJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.branch").value(branches[i]));
        }
    }

    @Test
    void testRepositorySearch() throws Exception {
        // 创建多个测试仓库
        for (int i = 1; i <= 3; i++) {
            GitRepository repo = new GitRepository();
            repo.setName("search-test-repo-" + i);
            repo.setGitUrl("https://github.com/test/search-repo-" + i + ".git");
            repo.setBranch("main");
            repo.setStatus(i % 2 == 0 ? "READY" : "CREATED");
            gitRepositoryRepo.save(repo);
        }

        // 获取所有仓库
        mockMvc.perform(get("/api/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    void testConcurrentRepositoryOperations() throws Exception {
        // 创建仓库
        GitRepository saved = gitRepositoryRepo.save(testRepository);
        Long repoId = saved.getId();

        // 模拟并发操作 - 多个线程同时尝试操作同一个仓库
        Thread[] threads = new Thread[5];
        boolean[] results = new boolean[5];

        for (int i = 0; i < 5; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    mockMvc.perform(get("/api/repositories/" + repoId))
                            .andExpect(status().isOk());
                    results[index] = true;
                } catch (Exception e) {
                    results[index] = false;
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有操作都成功
        for (boolean result : results) {
            assertTrue(result);
        }
    }

    @Test
    void testRepositoryPersistence() throws Exception {
        // 创建仓库
        String repositoryJson = objectMapper.writeValueAsString(testRepository);
        
        String response = mockMvc.perform(post("/api/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(repositoryJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        GitRepository createdRepo = objectMapper.readValue(response, GitRepository.class);
        Long repoId = createdRepo.getId();

        // 验证数据库中确实存在该仓库
        assertTrue(gitRepositoryRepo.existsById(repoId));
        
        // 通过Repository接口验证数据
        GitRepository fromDb = gitRepositoryRepo.findById(repoId).orElse(null);
        assertNotNull(fromDb);
        assertEquals("integration-test-repo", fromDb.getName());
        assertEquals("https://github.com/test/integration-repo.git", fromDb.getGitUrl());
        assertEquals("main", fromDb.getBranch());
        assertEquals("CREATED", fromDb.getStatus());
        assertNotNull(fromDb.getCreatedTime());
        assertNotNull(fromDb.getUpdatedTime());
    }
}