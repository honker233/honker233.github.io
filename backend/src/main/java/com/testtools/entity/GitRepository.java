package com.testtools.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "git_repository")
public class GitRepository {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Repository name cannot be blank")
    private String name;
    
    @Column(nullable = false, length = 500)
    @NotBlank(message = "Git URL cannot be blank")
    private String gitUrl;
    
    @Column(length = 500)
    private String localPath;
    
    @Column(length = 100)
    private String branch = "main";
    
    @Column(length = 100)
    private String lastCommitId;
    
    @Column(length = 50)
    private String status = "CREATED";
    
    @Column
    private LocalDateTime createdTime = LocalDateTime.now();
    
    @Column
    private LocalDateTime updatedTime = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getGitUrl() { return gitUrl; }
    public void setGitUrl(String gitUrl) { this.gitUrl = gitUrl; }
    
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    
    public String getLastCommitId() { return lastCommitId; }
    public void setLastCommitId(String lastCommitId) { this.lastCommitId = lastCommitId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}