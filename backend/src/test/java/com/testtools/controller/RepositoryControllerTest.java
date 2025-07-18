package com.testtools.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testtools.entity.GitRepository;
import com.testtools.repository.GitRepositoryRepo;
import com.testtools.service.GitAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(RepositoryController.class)
public class RepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitRepositoryRepo gitRepositoryRepo;

    @MockBean
    private GitAnalysisService gitAnalysisService;

    @Autowired
    private ObjectMapper objectMapper;

    private GitRepository testRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testRepository = new GitRepository();
        testRepository.setId(1L);
        testRepository.setName("test-repo");
        testRepository.setGitUrl("https://github.com/test/repo.git");
        testRepository.setLocalPath("./workspace/test-repo");
        testRepository.setBranch("main");
        testRepository.setStatus("CREATED");
        testRepository.setCreatedTime(LocalDateTime.now());
        testRepository.setUpdatedTime(LocalDateTime.now());
    }

    @Test
    void testGetRepositories_Success() throws Exception {
        List<GitRepository> repositories = Arrays.asList(testRepository);
        when(gitRepositoryRepo.findAll()).thenReturn(repositories);

        mockMvc.perform(get("/api/repositories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("test-repo"))
                .andExpect(jsonPath("$[0].gitUrl").value("https://github.com/test/repo.git"));

        verify(gitRepositoryRepo, times(1)).findAll();
    }

    @Test
    void testGetRepository_Success() throws Exception {
        when(gitRepositoryRepo.findById(1L)).thenReturn(Optional.of(testRepository));

        mockMvc.perform(get("/api/repositories/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("test-repo"));

        verify(gitRepositoryRepo, times(1)).findById(1L);
    }

    @Test
    void testGetRepository_NotFound() throws Exception {
        when(gitRepositoryRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/repositories/999"))
                .andExpect(status().isNotFound());

        verify(gitRepositoryRepo, times(1)).findById(999L);
    }

    @Test
    void testCreateRepository_Success() throws Exception {
        GitRepository newRepository = new GitRepository();
        newRepository.setName("new-repo");
        newRepository.setGitUrl("https://github.com/test/new-repo.git");
        newRepository.setBranch("main");

        GitRepository savedRepository = new GitRepository();
        savedRepository.setId(2L);
        savedRepository.setName("new-repo");
        savedRepository.setGitUrl("https://github.com/test/new-repo.git");
        savedRepository.setLocalPath("./workspace/new-repo");
        savedRepository.setBranch("main");
        savedRepository.setStatus("CREATED");
        savedRepository.setCreatedTime(LocalDateTime.now());
        savedRepository.setUpdatedTime(LocalDateTime.now());

        when(gitRepositoryRepo.save(any(GitRepository.class))).thenReturn(savedRepository);

        mockMvc.perform(post("/api/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRepository)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("new-repo"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.localPath").value("./workspace/new-repo"));

        verify(gitRepositoryRepo, times(1)).save(any(GitRepository.class));
    }

    @Test
    void testCreateRepository_Exception() throws Exception {
        GitRepository newRepository = new GitRepository();
        newRepository.setName("invalid-repo");

        when(gitRepositoryRepo.save(any(GitRepository.class))).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/api/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRepository)))
                .andExpect(status().isBadRequest());

        verify(gitRepositoryRepo, times(1)).save(any(GitRepository.class));
    }

    @Test
    void testCloneRepository_Success() throws Exception {
        when(gitRepositoryRepo.findById(1L)).thenReturn(Optional.of(testRepository));
        when(gitAnalysisService.getLatestCommitId(testRepository)).thenReturn("abc123");
        when(gitRepositoryRepo.save(any(GitRepository.class))).thenReturn(testRepository);

        mockMvc.perform(post("/api/repositories/1/clone"))
                .andExpect(status().isOk())
                .andExpect(content().string("Repository cloned successfully"));

        verify(gitRepositoryRepo, times(2)).findById(1L);
        verify(gitAnalysisService, times(1)).cloneOrPullRepository(testRepository);
        verify(gitAnalysisService, times(1)).getLatestCommitId(testRepository);
        verify(gitRepositoryRepo, times(2)).save(any(GitRepository.class));
    }

    @Test
    void testCloneRepository_NotFound() throws Exception {
        when(gitRepositoryRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/repositories/999/clone"))
                .andExpect(status().isNotFound());

        verify(gitRepositoryRepo, times(1)).findById(999L);
        verify(gitAnalysisService, never()).cloneOrPullRepository(any());
    }

    @Test
    void testCloneRepository_Exception() throws Exception {
        when(gitRepositoryRepo.findById(1L)).thenReturn(Optional.of(testRepository));
        doThrow(new RuntimeException("Git clone failed")).when(gitAnalysisService).cloneOrPullRepository(testRepository);
        when(gitRepositoryRepo.save(any(GitRepository.class))).thenReturn(testRepository);

        mockMvc.perform(post("/api/repositories/1/clone"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to clone repository: Git clone failed"));

        verify(gitRepositoryRepo, times(2)).findById(1L);
        verify(gitAnalysisService, times(1)).cloneOrPullRepository(testRepository);
        verify(gitRepositoryRepo, times(2)).save(any(GitRepository.class));
    }

    @Test
    void testPullRepository_Success() throws Exception {
        testRepository.setStatus("READY");
        when(gitRepositoryRepo.findById(1L)).thenReturn(Optional.of(testRepository));
        when(gitAnalysisService.getLatestCommitId(testRepository)).thenReturn("def456");
        when(gitRepositoryRepo.save(any(GitRepository.class))).thenReturn(testRepository);

        mockMvc.perform(post("/api/repositories/1/pull"))
                .andExpect(status().isOk())
                .andExpect(content().string("Repository updated successfully"));

        verify(gitRepositoryRepo, times(2)).findById(1L);
        verify(gitAnalysisService, times(1)).cloneOrPullRepository(testRepository);
        verify(gitAnalysisService, times(1)).getLatestCommitId(testRepository);
        verify(gitRepositoryRepo, times(2)).save(any(GitRepository.class));
    }

    @Test
    void testPullRepository_NotFound() throws Exception {
        when(gitRepositoryRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/repositories/999/pull"))
                .andExpect(status().isNotFound());

        verify(gitRepositoryRepo, times(1)).findById(999L);
        verify(gitAnalysisService, never()).cloneOrPullRepository(any());
    }

    @Test
    void testPullRepository_Exception() throws Exception {
        when(gitRepositoryRepo.findById(1L)).thenReturn(Optional.of(testRepository));
        doThrow(new RuntimeException("Git pull failed")).when(gitAnalysisService).cloneOrPullRepository(testRepository);
        when(gitRepositoryRepo.save(any(GitRepository.class))).thenReturn(testRepository);

        mockMvc.perform(post("/api/repositories/1/pull"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to pull repository: Git pull failed"));

        verify(gitRepositoryRepo, times(2)).findById(1L);
        verify(gitAnalysisService, times(1)).cloneOrPullRepository(testRepository);
        verify(gitRepositoryRepo, times(2)).save(any(GitRepository.class));
    }

    @Test
    void testDeleteRepository_Success() throws Exception {
        doNothing().when(gitRepositoryRepo).deleteById(1L);

        mockMvc.perform(delete("/api/repositories/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Repository deleted successfully"));

        verify(gitRepositoryRepo, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteRepository_Exception() throws Exception {
        doThrow(new RuntimeException("Delete failed")).when(gitRepositoryRepo).deleteById(1L);

        mockMvc.perform(delete("/api/repositories/1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to delete repository: Delete failed"));

        verify(gitRepositoryRepo, times(1)).deleteById(1L);
    }
}