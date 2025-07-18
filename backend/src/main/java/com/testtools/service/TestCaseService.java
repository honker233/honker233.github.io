package com.testtools.service;

import com.testtools.entity.TestCase;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

@Service
public class TestCaseService {
    
    private static final Pattern TEST_METHOD_PATTERN = Pattern.compile(
        "@Test.*?\\s+(public|private|protected)?\\s*void\\s+(\\w+)\\s*\\("
    );
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "public\\s+class\\s+(\\w+)"
    );
    
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "package\\s+([\\w\\.]+);"
    );

    public List<TestCase> parseTestCasesFromFile(MultipartFile file, Long repositoryId) throws IOException {
        List<TestCase> testCases = new ArrayList<>();
        
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }
        
        String fileExtension = getFileExtension(fileName);
        
        switch (fileExtension.toLowerCase()) {
            case "java":
                testCases.addAll(parseJavaTestFile(file, repositoryId));
                break;
            case "csv":
                testCases.addAll(parseCsvTestFile(file, repositoryId));
                break;
            case "xmind":
                testCases.addAll(parseXMindTestFile(file, repositoryId));
                break;
            case "xlsx":
            case "xls":
                testCases.addAll(parseExcelTestFile(file, repositoryId));
                break;
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileExtension + ". Supported: .java, .csv, .xmind, .xlsx, .xls");
        }
        
        return testCases;
    }

    private List<TestCase> parseJavaTestFile(MultipartFile file, Long repositoryId) throws IOException {
        List<TestCase> testCases = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            String fileContent = content.toString();
            String className = extractClassName(fileContent);
            String packageName = extractPackageName(fileContent);
            
            Matcher matcher = TEST_METHOD_PATTERN.matcher(fileContent);
            int testCount = 0;
            while (matcher.find()) {
                String methodName = matcher.group(2);
                testCount++;
                
                TestCase testCase = new TestCase();
                testCase.setRepositoryId(repositoryId);
                testCase.setCaseName(methodName);
                testCase.setCaseType("UNIT_TEST");
                testCase.setFilePath(file.getOriginalFilename());
                testCase.setClassName(className);
                testCase.setMethodName(methodName);
                testCase.setCoveredModules(packageName);
                testCase.setPriority(1);
                testCase.setCreatedTime(LocalDateTime.now());
                testCase.setUpdatedTime(LocalDateTime.now());
                
                String description = extractTestDescription(fileContent, methodName);
                testCase.setCaseDescription(description);
                
                testCases.add(testCase);
            }
            
            System.out.println("Parsed " + testCount + " test methods from " + file.getOriginalFilename());
        }
        
        return testCases;
    }

    private List<TestCase> parseCsvTestFile(MultipartFile file, Long repositoryId) throws IOException {
        List<TestCase> testCases = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return testCases;
            }
            
            String[] headers = headerLine.split(",");
            int nameIndex = findColumnIndex(headers, "name", "caseName", "testName");
            int descIndex = findColumnIndex(headers, "description", "desc");
            int typeIndex = findColumnIndex(headers, "type", "caseType");
            int classIndex = findColumnIndex(headers, "class", "className");
            int methodIndex = findColumnIndex(headers, "method", "methodName");
            int moduleIndex = findColumnIndex(headers, "module", "coveredModules");
            int priorityIndex = findColumnIndex(headers, "priority");
            
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                String[] values = line.split(",");
                
                if (values.length > Math.max(nameIndex, 0)) {
                    TestCase testCase = new TestCase();
                    testCase.setRepositoryId(repositoryId);
                    testCase.setCaseName(getValue(values, nameIndex));
                    testCase.setCaseDescription(getValue(values, descIndex));
                    testCase.setCaseType(getValue(values, typeIndex, "MANUAL_TEST"));
                    testCase.setFilePath(file.getOriginalFilename());
                    testCase.setClassName(getValue(values, classIndex));
                    testCase.setMethodName(getValue(values, methodIndex));
                    testCase.setCoveredModules(getValue(values, moduleIndex));
                    testCase.setPriority(getIntValue(values, priorityIndex, 1));
                    testCase.setCreatedTime(LocalDateTime.now());
                    testCase.setUpdatedTime(LocalDateTime.now());
                    
                    testCases.add(testCase);
                }
            }
            
            System.out.println("Parsed " + lineCount + " test cases from CSV: " + file.getOriginalFilename());
        }
        
        return testCases;
    }

    private String extractClassName(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown";
    }

    private String extractPackageName(String content) {
        Matcher matcher = PACKAGE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "default";
    }

    private String extractTestDescription(String content, String methodName) {
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(methodName)) {
                for (int j = i - 1; j >= Math.max(0, i - 5); j--) {
                    String line = lines[j].trim();
                    if (line.startsWith("//") || line.startsWith("*")) {
                        return line.replaceAll("^[/*\\s]+", "");
                    }
                }
                break;
            }
        }
        return "Test case for " + methodName;
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    private int findColumnIndex(String[] headers, String... possibleNames) {
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().toLowerCase();
            for (String name : possibleNames) {
                if (header.equals(name.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String getValue(String[] values, int index) {
        return getValue(values, index, "");
    }

    private String getValue(String[] values, int index, String defaultValue) {
        if (index >= 0 && index < values.length) {
            return values[index].trim();
        }
        return defaultValue;
    }

    private Integer getIntValue(String[] values, int index, int defaultValue) {
        if (index >= 0 && index < values.length) {
            try {
                return Integer.parseInt(values[index].trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private List<TestCase> parseXMindTestFile(MultipartFile file, Long repositoryId) throws IOException {
        List<TestCase> testCases = new ArrayList<>();
        
        System.out.println("Starting XMind parsing for file: " + file.getOriginalFilename());
        System.out.println("File size: " + file.getSize() + " bytes");
        
        try (ZipInputStream zipIn = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            boolean foundContentXml = false;
            
            while ((entry = zipIn.getNextEntry()) != null) {
                System.out.println("Found zip entry: " + entry.getName());
                
                if ("content.xml".equals(entry.getName())) {
                    foundContentXml = true;
                    System.out.println("Processing content.xml...");
                    testCases.addAll(parseXMindContent(zipIn, repositoryId, file.getOriginalFilename()));
                    break;
                }
            }
            
            if (!foundContentXml) {
                throw new IOException("Invalid XMind file: content.xml not found. This might not be a valid XMind file.");
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing XMind file: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to parse XMind file: " + e.getMessage() + ". Please ensure this is a valid XMind file.", e);
        }
        
        System.out.println("Parsed " + testCases.size() + " test cases from XMind: " + file.getOriginalFilename());
        return testCases;
    }

    private List<TestCase> parseXMindContent(InputStream xmlStream, Long repositoryId, String fileName) 
            throws DocumentException {
        List<TestCase> testCases = new ArrayList<>();
        
        try {
            SAXReader reader = new SAXReader();
            // 禁用外部实体以防止XXE攻击
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            Document document = reader.read(xmlStream);
            Element root = document.getRootElement();
            
            System.out.println("Root element: " + root.getName());
            System.out.println("Root namespace: " + root.getNamespaceURI());
            
            // XMind的主题结构：直接查找sheet元素
            List<Element> sheets = new ArrayList<>();
            
            // 直接在根元素查找sheet
            if ("sheet".equals(root.getName())) {
                sheets.add(root);
            } else {
                sheets.addAll(root.elements("sheet"));
            }
            
            System.out.println("Found " + sheets.size() + " sheets in root");
            
            if (sheets.isEmpty()) {
                // 查找workbook下的sheet
                Element workbook = root.element("workbook");
                if (workbook != null) {
                    sheets.addAll(workbook.elements("sheet"));
                    System.out.println("Found " + sheets.size() + " sheets via workbook");
                }
            }
            
            // 如果还是没有找到，尝试递归查找
            if (sheets.isEmpty()) {
                findSheetsRecursively(root, sheets);
                System.out.println("Found " + sheets.size() + " sheets via recursive search");
            }
            
            for (Element sheet : sheets) {
                Element rootTopic = sheet.element("topic");
                if (rootTopic != null) {
                    System.out.println("Processing sheet with root topic: " + 
                        (rootTopic.element("title") != null ? rootTopic.element("title").getTextTrim() : "[no title]"));
                    parseTopicRecursively(rootTopic, "", testCases, repositoryId, fileName);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing XMind XML content: " + e.getMessage());
            e.printStackTrace();
            throw new DocumentException("Failed to parse XMind XML content: " + e.getMessage(), e);
        }
        
        return testCases;
    }

    private void parseTopicRecursively(Element topic, String parentPath, List<TestCase> testCases, 
                                     Long repositoryId, String fileName) {
        Element titleElement = topic.element("title");
        if (titleElement == null) return;
        
        String title = titleElement.getTextTrim();
        String currentPath = parentPath.isEmpty() ? title : parentPath + " -> " + title;
        
        // 检查是否是测试用例节点（通常包含"测试"、"用例"、"Test"、"Case"等关键词）
        if (isTestCaseNode(title)) {
            TestCase testCase = new TestCase();
            testCase.setRepositoryId(repositoryId);
            testCase.setCaseName(title);
            testCase.setCaseDescription(extractDescription(topic, currentPath));
            testCase.setCaseType("XMIND_TEST");
            testCase.setFilePath(fileName);
            testCase.setClassName(extractClassNameFromPath(currentPath));
            testCase.setMethodName(extractMethodName(title));
            testCase.setCoveredModules(parentPath);
            testCase.setPriority(extractPriority(topic));
            testCase.setCreatedTime(LocalDateTime.now());
            testCase.setUpdatedTime(LocalDateTime.now());
            
            testCases.add(testCase);
        }
        
        // 递归处理子节点
        Element children = topic.element("children");
        if (children != null) {
            List<Element> childTopics = children.elements("topics");
            for (Element childTopicGroup : childTopics) {
                List<Element> childList = childTopicGroup.elements("topic");
                for (Element childTopic : childList) {
                    parseTopicRecursively(childTopic, currentPath, testCases, repositoryId, fileName);
                }
            }
        }
    }

    private boolean isTestCaseNode(String title) {
        if (title == null) return false;
        String lowerTitle = title.toLowerCase();
        return lowerTitle.contains("测试") || lowerTitle.contains("用例") || 
               lowerTitle.contains("test") || lowerTitle.contains("case") ||
               lowerTitle.matches(".*\\d+\\..*") || // 数字编号
               lowerTitle.matches(".*tc\\d+.*"); // TC编号
    }

    private String extractDescription(Element topic, String path) {
        // 尝试从notes中获取描述
        Element notes = topic.element("notes");
        if (notes != null) {
            Element plain = notes.element("plain");
            if (plain != null) {
                String noteText = plain.getTextTrim();
                if (!noteText.isEmpty()) {
                    return noteText;
                }
            }
        }
        
        // 如果没有notes，使用路径作为描述
        return "XMind测试用例: " + path;
    }

    private String extractClassNameFromPath(String path) {
        // 从路径中提取可能的类名
        String[] parts = path.split(" -> ");
        if (parts.length > 1) {
            return parts[parts.length - 2].replaceAll("[^a-zA-Z0-9]", "");
        }
        return "XMindTestClass";
    }

    private String extractMethodName(String title) {
        // 清理标题作为方法名
        return title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
    }

    private Integer extractPriority(Element topic) {
        // 尝试从标记或优先级属性中提取优先级
        Element markers = topic.element("markers");
        if (markers != null) {
            List<Element> markerList = markers.elements("marker");
            for (Element marker : markerList) {
                String markerId = marker.attributeValue("marker-id");
                if (markerId != null) {
                    if (markerId.contains("priority-1") || markerId.contains("star-red")) {
                        return 3; // 高优先级
                    } else if (markerId.contains("priority-2") || markerId.contains("star-orange")) {
                        return 2; // 中优先级
                    }
                }
            }
        }
        return 1; // 默认低优先级
    }
    
    private List<TestCase> parseExcelTestFile(MultipartFile file, Long repositoryId) throws IOException {
        List<TestCase> testCases = new ArrayList<>();
        // 这里可以添加Excel解析逻辑，暂时作为CSV处理
        System.out.println("Excel file uploaded, treating as CSV format for now: " + file.getOriginalFilename());
        return parseCsvTestFile(file, repositoryId);
    }
    
    @SuppressWarnings("unchecked")
    private void findSheetsRecursively(Element element, List<Element> sheets) {
        if ("sheet".equals(element.getName())) {
            sheets.add(element);
        }
        List<Element> children = element.elements();
        for (Element child : children) {
            findSheetsRecursively(child, sheets);
        }
    }
}