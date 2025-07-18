package com.testtools.controller;

import com.testtools.entity.GitRepository;
import com.testtools.repository.GitRepositoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {
    
    @Autowired
    private GitRepositoryRepo gitRepositoryRepo;
    
    @GetMapping("/browse/{repositoryId}")
    public ResponseEntity<?> browseRepository(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "") String path) {
        try {
            Optional<GitRepository> repoOpt = gitRepositoryRepo.findById(repositoryId);
            if (!repoOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Repository not found");
            }
            
            GitRepository repository = repoOpt.get();
            if (!"READY".equals(repository.getStatus())) {
                return ResponseEntity.badRequest().body("Repository is not ready");
            }
            
            File repoDir = new File(repository.getLocalPath());
            if (!repoDir.exists()) {
                return ResponseEntity.badRequest().body("Repository directory not found");
            }
            
            File targetDir = path.isEmpty() ? repoDir : new File(repoDir, path);
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                return ResponseEntity.badRequest().body("Directory not found");
            }
            
            // 确保路径在仓库目录内（安全检查）
            if (!targetDir.getCanonicalPath().startsWith(repoDir.getCanonicalPath())) {
                return ResponseEntity.badRequest().body("Invalid path");
            }
            
            List<Map<String, Object>> items = new ArrayList<>();
            
            // 如果不是根目录，添加返回上级目录的选项
            if (!path.isEmpty()) {
                Map<String, Object> parentItem = new HashMap<>();
                parentItem.put("name", "..");
                parentItem.put("type", "directory");
                parentItem.put("path", getParentPath(path));
                parentItem.put("size", 0L);
                parentItem.put("lastModified", 0L);
                items.add(parentItem);
            }
            
            File[] files = targetDir.listFiles();
            if (files != null) {
                Arrays.sort(files, (a, b) -> {
                    // 目录排在前面
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                
                for (File file : files) {
                    // 跳过隐藏文件和.git目录
                    if (file.getName().startsWith(".")) continue;
                    
                    Map<String, Object> item = new HashMap<>();
                    String relativePath = path.isEmpty() ? file.getName() : path + "/" + file.getName();
                    
                    item.put("name", file.getName());
                    item.put("type", file.isDirectory() ? "directory" : "file");
                    item.put("path", relativePath);
                    item.put("size", file.isDirectory() ? 0L : file.length());
                    item.put("lastModified", file.lastModified());
                    
                    if (file.isFile()) {
                        item.put("extension", getFileExtension(file.getName()));
                        item.put("isCodeFile", isCodeFile(file.getName()));
                    }
                    
                    items.add(item);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("currentPath", path);
            result.put("items", items);
            result.put("repositoryName", repository.getName());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to browse repository: " + e.getMessage());
        }
    }
    
    @GetMapping("/content/{repositoryId}")
    public ResponseEntity<?> getFileContent(
            @PathVariable Long repositoryId,
            @RequestParam String filePath) {
        try {
            Optional<GitRepository> repoOpt = gitRepositoryRepo.findById(repositoryId);
            if (!repoOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Repository not found");
            }
            
            GitRepository repository = repoOpt.get();
            File repoDir = new File(repository.getLocalPath());
            File targetFile = new File(repoDir, filePath);
            
            if (!targetFile.exists() || !targetFile.isFile()) {
                return ResponseEntity.badRequest().body("File not found");
            }
            
            // 安全检查
            if (!targetFile.getCanonicalPath().startsWith(repoDir.getCanonicalPath())) {
                return ResponseEntity.badRequest().body("Invalid file path");
            }
            
            // 检查文件大小，避免读取过大的文件
            if (targetFile.length() > 1024 * 1024) { // 1MB
                return ResponseEntity.badRequest().body("File too large to display");
            }
            
            // 检查是否是文本文件
            if (!isTextFile(targetFile.getName())) {
                return ResponseEntity.badRequest().body("File is not a text file");
            }
            
            String content = Files.readString(targetFile.toPath());
            
            Map<String, Object> result = new HashMap<>();
            result.put("filePath", filePath);
            result.put("fileName", targetFile.getName());
            result.put("content", content);
            result.put("size", targetFile.length());
            result.put("lastModified", targetFile.lastModified());
            result.put("extension", getFileExtension(targetFile.getName()));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to read file: " + e.getMessage());
        }
    }
    
    @GetMapping("/stats/{repositoryId}")
    public ResponseEntity<?> getRepositoryStats(@PathVariable Long repositoryId) {
        try {
            Optional<GitRepository> repoOpt = gitRepositoryRepo.findById(repositoryId);
            if (!repoOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Repository not found");
            }
            
            GitRepository repository = repoOpt.get();
            File repoDir = new File(repository.getLocalPath());
            
            if (!repoDir.exists()) {
                return ResponseEntity.badRequest().body("Repository directory not found");
            }
            
            Map<String, Object> stats = new HashMap<>();
            
            // 统计文件信息
            AtomicStats atomicStats = new AtomicStats();
            countFiles(repoDir, atomicStats);
            
            stats.put("totalFiles", atomicStats.totalFiles);
            stats.put("totalDirectories", atomicStats.totalDirectories);
            stats.put("codeFiles", atomicStats.codeFiles);
            stats.put("totalSize", atomicStats.totalSize);
            stats.put("repositoryPath", repoDir.getAbsolutePath());
            
            // 文件类型统计
            Map<String, Integer> fileTypes = new HashMap<>();
            countFileTypes(repoDir, fileTypes);
            stats.put("fileTypes", fileTypes);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to get repository stats: " + e.getMessage());
        }
    }
    
    private void countFiles(File dir, AtomicStats stats) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(".")) continue;
                
                if (file.isDirectory()) {
                    stats.totalDirectories++;
                    countFiles(file, stats);
                } else {
                    stats.totalFiles++;
                    stats.totalSize += file.length();
                    if (isCodeFile(file.getName())) {
                        stats.codeFiles++;
                    }
                }
            }
        }
    }
    
    private void countFileTypes(File dir, Map<String, Integer> fileTypes) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(".")) continue;
                
                if (file.isDirectory()) {
                    countFileTypes(file, fileTypes);
                } else {
                    String ext = getFileExtension(file.getName());
                    fileTypes.merge(ext.isEmpty() ? "no-extension" : ext, 1, Integer::sum);
                }
            }
        }
    }
    
    private String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : "";
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }
    
    private boolean isCodeFile(String fileName) {
        String ext = getFileExtension(fileName);
        return Arrays.asList("java", "js", "ts", "py", "cpp", "c", "h", "cs", "go", "php", 
                           "html", "css", "vue", "jsx", "tsx", "kt", "swift", "rb", "scala", "groovy")
                .contains(ext);
    }
    
    private boolean isTextFile(String fileName) {
        String ext = getFileExtension(fileName);
        return Arrays.asList("java", "js", "ts", "py", "cpp", "c", "h", "cs", "go", "php",
                           "html", "css", "vue", "jsx", "tsx", "kt", "swift", "rb", "scala", "groovy",
                           "txt", "md", "json", "xml", "yml", "yaml", "properties", "conf", "cfg",
                           "sql", "sh", "bat", "dockerfile", "gitignore", "readme")
                .contains(ext) || fileName.toLowerCase().contains("readme") || 
                fileName.toLowerCase().contains("license");
    }
    
    private static class AtomicStats {
        int totalFiles = 0;
        int totalDirectories = 0;
        int codeFiles = 0;
        long totalSize = 0;
    }
}