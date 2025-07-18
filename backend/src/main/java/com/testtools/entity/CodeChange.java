package com.testtools.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_change")
public class CodeChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long repositoryId;
    
    @Column(nullable = false, length = 100)
    private String commitId;
    
    @Column(nullable = false, length = 500)
    private String filePath;
    
    @Column(nullable = false, length = 50)
    private String changeType;
    
    @Column(length = 2000)
    private String changedMethods;
    
    @Column(length = 2000)
    private String changedClasses;
    
    @Column(length = 500)
    private String modulePath;
    
    @Column
    private Integer linesAdded = 0;
    
    @Column
    private Integer linesDeleted = 0;
    
    @Column
    private LocalDateTime createdTime = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long repositoryId) { this.repositoryId = repositoryId; }
    
    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    
    public String getChangedMethods() { return changedMethods; }
    public void setChangedMethods(String changedMethods) { this.changedMethods = changedMethods; }
    
    public String getChangedClasses() { return changedClasses; }
    public void setChangedClasses(String changedClasses) { this.changedClasses = changedClasses; }
    
    public String getModulePath() { return modulePath; }
    public void setModulePath(String modulePath) { this.modulePath = modulePath; }
    
    public Integer getLinesAdded() { return linesAdded; }
    public void setLinesAdded(Integer linesAdded) { this.linesAdded = linesAdded; }
    
    public Integer getLinesDeleted() { return linesDeleted; }
    public void setLinesDeleted(Integer linesDeleted) { this.linesDeleted = linesDeleted; }
    
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}