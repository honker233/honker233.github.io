package com.testtools.service;

import com.testtools.entity.CodeChange;
import com.testtools.entity.GitRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitAnalysisService {
    
    @Value("${app.git.workspace:./workspace}")
    private String workspaceDir;
    
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "\\s*(public|private|protected|static|\\s)*\\s+[\\w\\<\\>\\[\\]]+\\s+(\\w+)\\s*\\([^\\)]*\\)\\s*\\{?"
    );
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "\\s*(public|private|protected|static|abstract|final|\\s)*\\s*(class|interface|enum)\\s+(\\w+)"
    );

    public Git cloneOrPullRepository(GitRepository repository) throws GitAPIException, IOException {
        File localPath = new File(repository.getLocalPath());
        
        if (localPath.exists() && new File(localPath, ".git").exists()) {
            Git git = Git.open(localPath);
            git.pull().call();
            System.out.println("Repository pulled: " + repository.getName());
            return git;
        } else {
            if (localPath.exists()) {
                deleteDirectory(localPath);
            }
            localPath.mkdirs();
            
            Git git = Git.cloneRepository()
                    .setURI(repository.getGitUrl())
                    .setDirectory(localPath)
                    .setBranch(repository.getBranch())
                    .call();
            System.out.println("Repository cloned: " + repository.getName());
            return git;
        }
    }

    public List<CodeChange> analyzeCodeChanges(GitRepository repository, String fromCommit, String toCommit) 
            throws GitAPIException, IOException {
        
        List<CodeChange> codeChanges = new ArrayList<>();
        File repoPath = new File(repository.getLocalPath());
        
        System.out.println("Analyzing code changes for repository: " + repository.getName());
        System.out.println("From commit: " + fromCommit + ", To commit: " + toCommit);
        System.out.println("Repository path: " + repoPath.getAbsolutePath());
        
        if (!repoPath.exists() || !new File(repoPath, ".git").exists()) {
            throw new IllegalArgumentException("Repository not found or not a git repository: " + repoPath.getAbsolutePath());
        }
        
        try (Git git = Git.open(repoPath)) {
            // 先获取最新的代码
            try {
                git.fetch().call();
                System.out.println("Repository fetched successfully");
            } catch (Exception e) {
                System.out.println("Warning: Failed to fetch repository: " + e.getMessage());
            }
            
            ObjectId fromCommitId = git.getRepository().resolve(fromCommit);
            ObjectId toCommitId = git.getRepository().resolve(toCommit);
            
            if (fromCommitId == null) {
                throw new IllegalArgumentException("Invalid fromCommit: " + fromCommit + ". Make sure the commit exists.");
            }
            if (toCommitId == null) {
                throw new IllegalArgumentException("Invalid toCommit: " + toCommit + ". Make sure the commit exists.");
            }
            
            if (fromCommitId.equals(toCommitId)) {
                System.out.println("Warning: From and to commits are the same");
                return codeChanges;
            }
            
            System.out.println("Resolved commits - From: " + fromCommitId.getName() + ", To: " + toCommitId.getName());
            
            List<DiffEntry> diffs = git.diff()
                    .setOldTree(prepareTreeParser(git, fromCommitId))
                    .setNewTree(prepareTreeParser(git, toCommitId))
                    .call();
            
            System.out.println("Found " + diffs.size() + " diff entries");
            
            for (DiffEntry diff : diffs) {
                CodeChange codeChange = analyzeFileDiff(repository, diff, git, toCommit);
                if (codeChange != null) {
                    codeChanges.add(codeChange);
                    System.out.println("Analyzed file: " + diff.getNewPath() + " (" + diff.getChangeType() + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Error analyzing code changes: " + e.getMessage());
            throw e;
        }
        
        System.out.println("Analysis complete. Found " + codeChanges.size() + " code changes");
        return codeChanges;
    }

    private CodeChange analyzeFileDiff(GitRepository repository, DiffEntry diff, Git git, String commitId) 
            throws IOException {
        
        String filePath = diff.getNewPath();
        if (filePath.equals("/dev/null")) {
            filePath = diff.getOldPath();
        }
        
        if (!isCodeFile(filePath)) {
            return null;
        }
        
        CodeChange codeChange = new CodeChange();
        codeChange.setRepositoryId(repository.getId());
        codeChange.setCommitId(commitId);
        codeChange.setFilePath(filePath);
        codeChange.setChangeType(diff.getChangeType().name());
        codeChange.setModulePath(extractModulePath(filePath));
        codeChange.setCreatedTime(LocalDateTime.now());
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DiffFormatter formatter = new DiffFormatter(out)) {
            
            formatter.setRepository(git.getRepository());
            formatter.format(diff);
            
            String diffText = out.toString(StandardCharsets.UTF_8);
            analyzeDiffContent(codeChange, diffText);
            
        } catch (Exception e) {
            System.err.println("Failed to analyze diff for file: " + filePath + " - " + e.getMessage());
        }
        
        return codeChange;
    }

    private void analyzeDiffContent(CodeChange codeChange, String diffText) {
        Set<String> changedMethods = new HashSet<>();
        Set<String> changedClasses = new HashSet<>();
        int linesAdded = 0;
        int linesDeleted = 0;
        
        String[] lines = diffText.split("\n");
        
        for (String line : lines) {
            if (line.startsWith("+") && !line.startsWith("+++")) {
                linesAdded++;
                analyzeCodeLine(line.substring(1), changedMethods, changedClasses);
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                linesDeleted++;
                analyzeCodeLine(line.substring(1), changedMethods, changedClasses);
            }
        }
        
        codeChange.setChangedMethods(String.join(",", changedMethods));
        codeChange.setChangedClasses(String.join(",", changedClasses));
        codeChange.setLinesAdded(linesAdded);
        codeChange.setLinesDeleted(linesDeleted);
    }

    private void analyzeCodeLine(String line, Set<String> changedMethods, Set<String> changedClasses) {
        Matcher methodMatcher = METHOD_PATTERN.matcher(line);
        if (methodMatcher.find()) {
            String methodName = methodMatcher.group(2);
            if (methodName != null && !methodName.isEmpty()) {
                changedMethods.add(methodName);
            }
        }
        
        Matcher classMatcher = CLASS_PATTERN.matcher(line);
        if (classMatcher.find()) {
            String className = classMatcher.group(3);
            if (className != null && !className.isEmpty()) {
                changedClasses.add(className);
            }
        }
    }

    private String extractModulePath(String filePath) {
        String[] parts = filePath.split("/");
        if (parts.length > 2) {
            return parts[0] + "/" + parts[1];
        }
        return parts[0];
    }

    private boolean isCodeFile(String filePath) {
        if (filePath == null) return false;
        
        String lowerPath = filePath.toLowerCase();
        
        // 代码文件扩展名
        String[] codeExtensions = {
            ".java", ".js", ".ts", ".tsx", ".jsx", ".py", ".cpp", ".c", ".h", ".cs", ".go", ".php",
            ".html", ".css", ".scss", ".sass", ".less", ".vue", ".kt", ".swift", ".rb", ".scala", 
            ".groovy", ".rs", ".dart", ".r", ".m", ".mm", ".sql", ".pl", ".sh", ".bat", ".ps1",
            ".xml", ".json", ".yaml", ".yml", ".properties", ".conf", ".cfg", ".ini", ".toml"
        };
        
        for (String ext : codeExtensions) {
            if (lowerPath.endsWith(ext)) {
                return true;
            }
        }
        
        // 检查常见的配置文件和脚本文件（没有扩展名的）
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String lowerFileName = fileName.toLowerCase();
        
        String[] specialFiles = {
            "dockerfile", "makefile", "cmakelists.txt", "build.gradle", "pom.xml", 
            "package.json", "composer.json", "requirements.txt", "setup.py", 
            "gulpfile.js", "gruntfile.js", "webpack.config.js", "rollup.config.js",
            "readme", "license", "changelog", "contributing", "gitignore", "gitattributes"
        };
        
        for (String special : specialFiles) {
            if (lowerFileName.contains(special)) {
                return true;
            }
        }
        
        return false;
    }

    private AbstractTreeIterator prepareTreeParser(Git git, ObjectId commitId) throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit commit = walk.parseCommit(commitId);
            RevTree tree = walk.parseTree(commit.getTree().getId());
            
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            
            walk.dispose();
            return treeParser;
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            Files.walk(directory.toPath())
                    .sorted((a, b) -> b.compareTo(a))
                    .map(java.nio.file.Path::toFile)
                    .forEach(File::delete);
        }
    }

    public String getLatestCommitId(GitRepository repository) throws GitAPIException, IOException {
        try (Git git = Git.open(new File(repository.getLocalPath()))) {
            Iterable<RevCommit> commits = git.log().setMaxCount(1).call();
            for (RevCommit commit : commits) {
                return commit.getName();
            }
        }
        return null;
    }
}