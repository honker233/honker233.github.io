package com.testtools.service;

import com.testtools.dto.TestCaseRecommendation;
import com.testtools.entity.CodeChange;
import com.testtools.entity.TestCase;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestCaseRecommendationService {
    
    // 权重配置
    private static final double MODULE_MATCH_WEIGHT = 0.4;
    private static final double CLASS_MATCH_WEIGHT = 0.3;
    private static final double METHOD_MATCH_WEIGHT = 0.2;
    private static final double FILE_PATH_WEIGHT = 0.1;
    
    // 影响级别阈值
    private static final double HIGH_IMPACT_THRESHOLD = 0.7;
    private static final double MEDIUM_IMPACT_THRESHOLD = 0.4;

    public List<TestCaseRecommendation> recommendTestCases(List<CodeChange> codeChanges, 
                                                           List<TestCase> testCases) {
        
        Map<TestCase, Double> scoreMap = new HashMap<>();
        Map<TestCase, List<String>> reasonMap = new HashMap<>();
        
        System.out.println("分析 " + codeChanges.size() + " 个代码变更和 " + testCases.size() + " 个测试用例");
        
        for (TestCase testCase : testCases) {
            double totalScore = 0.0;
            List<String> reasons = new ArrayList<>();
            
            for (CodeChange codeChange : codeChanges) {
                double changeScore = calculateMatchScore(testCase, codeChange, reasons);
                totalScore += changeScore;
            }
            
            if (totalScore > 0) {
                scoreMap.put(testCase, totalScore);
                reasonMap.put(testCase, reasons);
            }
        }
        
        List<TestCaseRecommendation> recommendations = scoreMap.entrySet().stream()
                .map(entry -> {
                    TestCase testCase = entry.getKey();
                    Double score = entry.getValue();
                    
                    TestCaseRecommendation recommendation = new TestCaseRecommendation();
                    recommendation.setTestCase(testCase);
                    recommendation.setMatchScore(Math.min(score, 1.0)); // 确保分数不超过1.0
                    recommendation.setMatchReason(String.join("; ", reasonMap.get(testCase)));
                    recommendation.setImpactLevel(determineImpactLevel(score));
                    
                    return recommendation;
                })
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(Collectors.toList());
        
        System.out.println("生成了 " + recommendations.size() + " 个测试用例推荐");
        return recommendations;
    }

    private double calculateMatchScore(TestCase testCase, CodeChange codeChange, List<String> reasons) {
        double score = 0.0;
        
        score += calculateModuleScore(testCase, codeChange, reasons);
        score += calculateClassScore(testCase, codeChange, reasons);
        score += calculateMethodScore(testCase, codeChange, reasons);
        score += calculateFilePathScore(testCase, codeChange, reasons);
        
        return score;
    }

    private double calculateModuleScore(TestCase testCase, CodeChange codeChange, List<String> reasons) {
        if (testCase.getCoveredModules() == null || codeChange.getModulePath() == null) {
            return 0.0;
        }
        
        String[] testModules = testCase.getCoveredModules().split(",");
        String changeModule = codeChange.getModulePath();
        
        for (String module : testModules) {
            module = module.trim();
            if (module.contains(changeModule) || changeModule.contains(module)) {
                reasons.add("模块匹配: " + module + " <-> " + changeModule);
                return MODULE_MATCH_WEIGHT;
            }
            
            // 包名层级匹配
            if (isPackageRelated(module, changeModule)) {
                reasons.add("包层级匹配: " + module + " <-> " + changeModule);
                return MODULE_MATCH_WEIGHT * 0.7;
            }
        }
        
        return 0.0;
    }

    private double calculateClassScore(TestCase testCase, CodeChange codeChange, List<String> reasons) {
        if (testCase.getClassName() == null || codeChange.getChangedClasses() == null) {
            return 0.0;
        }
        
        String testClass = testCase.getClassName();
        String[] changedClasses = codeChange.getChangedClasses().split(",");
        
        for (String changedClass : changedClasses) {
            changedClass = changedClass.trim();
            if (changedClass.isEmpty()) continue;
            
            if (testClass.equals(changedClass)) {
                reasons.add("类名完全匹配: " + testClass);
                return CLASS_MATCH_WEIGHT;
            }
            
            if (testClass.contains(changedClass) || changedClass.contains(testClass)) {
                reasons.add("类名包含匹配: " + testClass + " <-> " + changedClass);
                return CLASS_MATCH_WEIGHT * 0.8;
            }
            
            double similarity = calculateStringSimilarity(testClass, changedClass);
            if (similarity > 0.7) {
                reasons.add("类名相似匹配: " + testClass + " <-> " + changedClass + " (相似度: " + 
                          String.format("%.2f", similarity) + ")");
                return CLASS_MATCH_WEIGHT * similarity;
            }
        }
        
        return 0.0;
    }

    private double calculateMethodScore(TestCase testCase, CodeChange codeChange, List<String> reasons) {
        if (testCase.getMethodName() == null || codeChange.getChangedMethods() == null) {
            return 0.0;
        }
        
        String testMethod = testCase.getMethodName();
        String[] changedMethods = codeChange.getChangedMethods().split(",");
        
        for (String changedMethod : changedMethods) {
            changedMethod = changedMethod.trim();
            if (changedMethod.isEmpty()) continue;
            
            if (testMethod.equals(changedMethod)) {
                reasons.add("方法名完全匹配: " + testMethod);
                return METHOD_MATCH_WEIGHT;
            }
            
            if (isTestMethodForBusinessMethod(testMethod, changedMethod)) {
                reasons.add("测试方法匹配业务方法: " + testMethod + " -> " + changedMethod);
                return METHOD_MATCH_WEIGHT;
            }
            
            if (testMethod.contains(changedMethod) || changedMethod.contains(testMethod)) {
                reasons.add("方法名包含匹配: " + testMethod + " <-> " + changedMethod);
                return METHOD_MATCH_WEIGHT * 0.8;
            }
            
            double similarity = calculateStringSimilarity(testMethod, changedMethod);
            if (similarity > 0.6) {
                reasons.add("方法名相似匹配: " + testMethod + " <-> " + changedMethod + " (相似度: " + 
                          String.format("%.2f", similarity) + ")");
                return METHOD_MATCH_WEIGHT * similarity;
            }
        }
        
        return 0.0;
    }

    private double calculateFilePathScore(TestCase testCase, CodeChange codeChange, List<String> reasons) {
        if (testCase.getFilePath() == null || codeChange.getFilePath() == null) {
            return 0.0;
        }
        
        String testPath = testCase.getFilePath();
        String changePath = codeChange.getFilePath();
        
        String testPathWithoutExtension = removeFileExtension(testPath);
        String changePathWithoutExtension = removeFileExtension(changePath);
        
        if (testPathWithoutExtension.contains(changePathWithoutExtension) || 
            changePathWithoutExtension.contains(testPathWithoutExtension)) {
            reasons.add("文件路径匹配: " + testPath + " <-> " + changePath);
            return FILE_PATH_WEIGHT;
        }
        
        return 0.0;
    }

    private boolean isTestMethodForBusinessMethod(String testMethod, String businessMethod) {
        String lowerTestMethod = testMethod.toLowerCase();
        String lowerBusinessMethod = businessMethod.toLowerCase();
        
        return lowerTestMethod.contains("test" + lowerBusinessMethod) ||
               lowerTestMethod.contains(lowerBusinessMethod + "test") ||
               (lowerTestMethod.startsWith("test") && lowerTestMethod.contains(lowerBusinessMethod));
    }

    private boolean isPackageRelated(String package1, String package2) {
        String[] parts1 = package1.split("\\.");
        String[] parts2 = package2.split("\\.");
        
        int minLength = Math.min(parts1.length, parts2.length);
        int matchingParts = 0;
        
        for (int i = 0; i < minLength; i++) {
            if (parts1[i].equals(parts2[i])) {
                matchingParts++;
            } else {
                break;
            }
        }
        
        return matchingParts >= 2; // 至少有2层包名匹配
    }

    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = calculateLevenshteinDistance(s1, s2);
        return (maxLength - distance) / (double) maxLength;
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    private String removeFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filePath.substring(0, lastDotIndex);
        }
        return filePath;
    }

    private String determineImpactLevel(double score) {
        if (score >= HIGH_IMPACT_THRESHOLD) {
            return "HIGH";
        } else if (score >= MEDIUM_IMPACT_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}