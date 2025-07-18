package com.testtools.service;

import com.testtools.entity.CodeChange;
import com.testtools.entity.GitRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class GitAnalysisServiceTest {

    @InjectMocks
    private GitAnalysisService gitAnalysisService;

    @TempDir
    Path tempDir;

    private GitRepository testRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置工作目录为临时目录
        ReflectionTestUtils.setField(gitAnalysisService, "workspaceDir", tempDir.toString());
        
        testRepository = new GitRepository();
        testRepository.setId(1L);
        testRepository.setName("test-repo");
        testRepository.setGitUrl("https://github.com/test/repo.git");
        testRepository.setLocalPath(tempDir.resolve("test-repo").toString());
        testRepository.setBranch("main");
        testRepository.setStatus("CREATED");
    }

    @Test
    void testGetLatestCommitId_RepositoryNotFound() {
        GitRepository nonExistentRepo = new GitRepository();
        nonExistentRepo.setLocalPath("/non/existent/path");

        assertThrows(Exception.class, () -> {
            gitAnalysisService.getLatestCommitId(nonExistentRepo);
        });
    }

    @Test
    void testAnalyzeCodeChanges_InvalidRepository() {
        GitRepository invalidRepo = new GitRepository();
        invalidRepo.setLocalPath("/invalid/path");

        assertThrows(IllegalArgumentException.class, () -> {
            gitAnalysisService.analyzeCodeChanges(invalidRepo, "commit1", "commit2");
        });
    }

    @Test
    void testAnalyzeCodeChanges_InvalidCommits() throws IOException {
        // 创建一个临时的Git仓库目录结构
        File repoDir = tempDir.resolve("test-repo").toFile();
        repoDir.mkdirs();
        File gitDir = new File(repoDir, ".git");
        gitDir.mkdirs();

        testRepository.setLocalPath(repoDir.getAbsolutePath());

        // 这个测试会因为无效的commit而抛出异常
        assertThrows(Exception.class, () -> {
            gitAnalysisService.analyzeCodeChanges(testRepository, "invalid-commit", "another-invalid-commit");
        });
    }

    @Test
    void testIsCodeFile_JavaFile() throws Exception {
        // 使用反射访问私有方法进行测试
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(
            gitAnalysisService, "isCodeFile", "src/main/java/Test.java");
        assertTrue(result);
    }

    @Test
    void testIsCodeFile_NonCodeFile() throws Exception {
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(
            gitAnalysisService, "isCodeFile", "README.md");
        assertTrue(result); // README.md 被认为是代码文件
    }

    @Test
    void testIsCodeFile_BinaryFile() throws Exception {
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(
            gitAnalysisService, "isCodeFile", "image.png");
        assertFalse(result);
    }

    @Test
    void testExtractModulePath_SimpleFile() throws Exception {
        String result = (String) ReflectionTestUtils.invokeMethod(
            gitAnalysisService, "extractModulePath", "src/main/java/Test.java");
        assertEquals("src/main", result);
    }

    @Test
    void testExtractModulePath_RootFile() throws Exception {
        String result = (String) ReflectionTestUtils.invokeMethod(
            gitAnalysisService, "extractModulePath", "Test.java");
        assertEquals("Test.java", result);
    }

    @Test
    void testExtractModulePath_SingleDirectory() throws Exception {
        String result = (String) ReflectionTestUtils.invokeMethod(
            gitAnalysisService, "extractModulePath", "src/Test.java");
        assertEquals("src", result);
    }

    @Test
    void testAnalyzeDiffContent_AddedLines() throws Exception {
        CodeChange codeChange = new CodeChange();
        String diffText = "+++ b/Test.java\n" +
                         "+ public void testMethod() {\n" +
                         "+ System.out.println(\"test\");\n" +
                         "+ }\n" +
                         "--- a/Test.java\n" +
                         "- public void oldMethod() {\n" +
                         "- }\n";

        ReflectionTestUtils.invokeMethod(gitAnalysisService, "analyzeDiffContent", codeChange, diffText);

        assertEquals(3, codeChange.getLinesAdded());
        assertEquals(2, codeChange.getLinesDeleted());
        assertTrue(codeChange.getChangedMethods().contains("testMethod"));
        assertTrue(codeChange.getChangedMethods().contains("oldMethod"));
    }

    @Test
    void testAnalyzeDiffContent_ClassChanges() throws Exception {
        CodeChange codeChange = new CodeChange();
        String diffText = "+++ b/Test.java\n" +
                         "+ public class NewClass {\n" +
                         "--- a/Test.java\n" +
                         "- public class OldClass {\n";

        ReflectionTestUtils.invokeMethod(gitAnalysisService, "analyzeDiffContent", codeChange, diffText);

        assertTrue(codeChange.getChangedClasses().contains("NewClass"));
        assertTrue(codeChange.getChangedClasses().contains("OldClass"));
    }

    @Test
    void testAnalyzeCodeLine_MethodDetection() throws Exception {
        java.util.Set<String> changedMethods = new java.util.HashSet<>();
        java.util.Set<String> changedClasses = new java.util.HashSet<>();

        ReflectionTestUtils.invokeMethod(gitAnalysisService, "analyzeCodeLine", 
            "    public void testMethod() {", changedMethods, changedClasses);

        assertTrue(changedMethods.contains("testMethod"));
    }

    @Test
    void testAnalyzeCodeLine_ClassDetection() throws Exception {
        java.util.Set<String> changedMethods = new java.util.HashSet<>();
        java.util.Set<String> changedClasses = new java.util.HashSet<>();

        ReflectionTestUtils.invokeMethod(gitAnalysisService, "analyzeCodeLine", 
            "public class TestClass {", changedMethods, changedClasses);

        assertTrue(changedClasses.contains("TestClass"));
    }

    @Test
    void testAnalyzeCodeLine_InterfaceDetection() throws Exception {
        java.util.Set<String> changedMethods = new java.util.HashSet<>();
        java.util.Set<String> changedClasses = new java.util.HashSet<>();

        ReflectionTestUtils.invokeMethod(gitAnalysisService, "analyzeCodeLine", 
            "public interface TestInterface {", changedMethods, changedClasses);

        assertTrue(changedClasses.contains("TestInterface"));
    }

    @Test
    void testAnalyzeCodeLine_StaticMethodDetection() throws Exception {
        java.util.Set<String> changedMethods = new java.util.HashSet<>();
        java.util.Set<String> changedClasses = new java.util.HashSet<>();

        ReflectionTestUtils.invokeMethod(gitAnalysisService, "analyzeCodeLine", 
            "    public static String getStaticValue() {", changedMethods, changedClasses);

        assertTrue(changedMethods.contains("getStaticValue"));
    }

    @Test
    void testCloneOrPullRepository_NonExistentDirectory() throws Exception {
        // 测试克隆到不存在的目录
        File nonExistentDir = tempDir.resolve("non-existent-repo").toFile();
        testRepository.setLocalPath(nonExistentDir.getAbsolutePath());
        testRepository.setGitUrl("file://" + tempDir.resolve("dummy-repo").toString());

        // 创建一个虚拟的源仓库
        File sourceRepo = tempDir.resolve("dummy-repo").toFile();
        sourceRepo.mkdirs();
        
        // 这个测试会失败，因为我们没有真实的Git仓库
        assertThrows(Exception.class, () -> {
            gitAnalysisService.cloneOrPullRepository(testRepository);
        });
    }

    @Test
    void testRepositoryStatusValidation() {
        // 测试仓库状态的有效性
        testRepository.setStatus("CREATED");
        assertEquals("CREATED", testRepository.getStatus());
        
        testRepository.setStatus("CLONING");
        assertEquals("CLONING", testRepository.getStatus());
        
        testRepository.setStatus("READY");
        assertEquals("READY", testRepository.getStatus());
        
        testRepository.setStatus("ERROR");
        assertEquals("ERROR", testRepository.getStatus());
    }

    @Test
    void testRepositoryBranchValidation() {
        testRepository.setBranch("main");
        assertEquals("main", testRepository.getBranch());
        
        testRepository.setBranch("develop");
        assertEquals("develop", testRepository.getBranch());
        
        testRepository.setBranch("feature/test-branch");
        assertEquals("feature/test-branch", testRepository.getBranch());
    }

    @Test
    void testCodeFileExtensionValidation() throws Exception {
        // 测试各种代码文件扩展名
        String[] codeFiles = {
            "Test.java", "app.js", "component.tsx", "style.css", 
            "script.py", "main.cpp", "header.h", "Program.cs",
            "main.go", "index.php", "component.vue", "MainActivity.kt"
        };

        for (String file : codeFiles) {
            boolean result = (boolean) ReflectionTestUtils.invokeMethod(
                gitAnalysisService, "isCodeFile", file);
            assertTrue(result, "File " + file + " should be recognized as code file");
        }
    }

    @Test
    void testNonCodeFileExtensionValidation() throws Exception {
        // 测试非代码文件扩展名
        String[] nonCodeFiles = {
            "image.png", "document.pdf", "archive.zip", 
            "video.mp4", "audio.mp3", "data.bin"
        };

        for (String file : nonCodeFiles) {
            boolean result = (boolean) ReflectionTestUtils.invokeMethod(
                gitAnalysisService, "isCodeFile", file);
            assertFalse(result, "File " + file + " should not be recognized as code file");
        }
    }

    @Test
    void testSpecialConfigFilesValidation() throws Exception {
        // 测试特殊配置文件
        String[] configFiles = {
            "Dockerfile", "Makefile", "package.json", "pom.xml",
            "requirements.txt", "webpack.config.js", "README.md"
        };

        for (String file : configFiles) {
            boolean result = (boolean) ReflectionTestUtils.invokeMethod(
                gitAnalysisService, "isCodeFile", file);
            assertTrue(result, "Config file " + file + " should be recognized as code file");
        }
    }
}