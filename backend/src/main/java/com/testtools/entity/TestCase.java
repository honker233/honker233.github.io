package com.testtools.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_case")
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long repositoryId;
    
    @Column(nullable = false)
    private String caseName;
    
    @Column(length = 1000)
    private String caseDescription;
    
    @Column(length = 50)
    private String caseType = "UNIT_TEST";
    
    @Column(length = 500)
    private String filePath;
    
    @Column
    private String className;
    
    @Column
    private String methodName;
    
    @Column(length = 500)
    private String tags;
    
    @Column(length = 500)
    private String coveredModules;
    
    @Column
    private Integer priority = 1;
    
    @Column
    private LocalDateTime createdTime = LocalDateTime.now();
    
    @Column
    private LocalDateTime updatedTime = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long repositoryId) { this.repositoryId = repositoryId; }
    
    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    
    public String getCaseDescription() { return caseDescription; }
    public void setCaseDescription(String caseDescription) { this.caseDescription = caseDescription; }
    
    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public String getCoveredModules() { return coveredModules; }
    public void setCoveredModules(String coveredModules) { this.coveredModules = coveredModules; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}