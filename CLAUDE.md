# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Smart Test Case Selection Tool** - an intelligent system that analyzes Git code changes and recommends relevant test cases based on multi-dimensional matching algorithms. The application consists of a Spring Boot backend and Vue 3 frontend with H2 in-memory database.

## Essential Commands

### Environment Setup & Running
```bash
# One-command startup (includes environment checks)
./start.sh

# Stop all services  
./stop.sh

# Manual backend startup (from backend/ directory)
mvn spring-boot:run

# Manual frontend startup (from frontend/ directory)
npm run dev
```

### Development Commands
```bash
# Backend build and test
cd backend
mvn clean package                    # Build with tests
mvn clean package -DskipTests       # Build without tests
mvn test                            # Run all tests
mvn test -Dtest=RepositoryControllerTest  # Run specific test class
mvn test -Dtest="*Repository*Test"   # Run tests matching pattern

# Frontend development
cd frontend
npm install                         # Install dependencies
npm run build                       # Production build
npm run preview                     # Preview production build

# Check logs
tail -f backend.log                 # Backend logs
tail -f frontend.log                # Frontend logs
```

### Database Management
- **H2 Console**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:testcase_db`
- **Username**: `sa` / **Password**: (empty)

## Architecture & Key Concepts

### Multi-Layered Spring Boot Architecture
The backend follows a standard Spring Boot layered architecture:

- **Controller Layer** (`/controller/`) - REST API endpoints with @CrossOrigin for frontend communication
- **Service Layer** (`/service/`) - Business logic including Git analysis and test case recommendation algorithms  
- **Repository Layer** (`/repository/`) - JPA data access with custom query methods
- **Entity Layer** (`/entity/`) - JPA entities with validation annotations

### Core Domain Model
Three main entities form the foundation:
- **GitRepository** - Git repository metadata with status management (CREATED → CLONING → READY → ERROR)
- **TestCase** - Test case information supporting multiple formats (Java @Test, CSV, XMind, Excel)
- **CodeChange** - Git diff analysis results with file-level change tracking

### Intelligent Recommendation Algorithm
The recommendation system uses weighted scoring:
- **Module matching** (40%) - Package path and module structure similarity
- **Class matching** (30%) - Class name similarity and inheritance relationships  
- **Method matching** (20%) - Method name patterns and test method detection
- **File path matching** (10%) - File path similarity scoring

### File Processing Pipeline
Test cases support multiple input formats:
1. **Java source files** - Automatic @Test annotation parsing with method extraction
2. **CSV files** - Structured format with configurable column mapping
3. **XMind files** - Mind map parsing with ZIP extraction and XML processing
4. **Excel files** - Currently processed as CSV format

### Git Integration Architecture
- **JGit Library** - Pure Java Git implementation for repository operations
- **Workspace Management** - Local repository cloning in `./workspace/` directory
- **Diff Analysis** - Commit-to-commit comparison with detailed change tracking
- **Code Pattern Recognition** - Regex-based detection of classes, methods, and code structures

## Important Configuration

### Application Properties (`backend/src/main/resources/application.yml`)
```yaml
app:
  git:
    workspace: ./workspace          # Git repository storage location
  testcase:
    upload-path: ./uploads         # Test case file uploads

spring:
  servlet:
    multipart:
      max-file-size: 50MB          # File upload size limit
      max-request-size: 50MB
```

### Database Configuration
- Uses H2 in-memory database with `create-drop` strategy
- Data persists only during application runtime
- All tables auto-created from JPA entities on startup

### Cross-Origin Configuration
All controllers use `@CrossOrigin(origins = "*")` for frontend communication on different ports.

## Key Service Classes

### GitAnalysisService
Handles all Git operations:
- Repository cloning/pulling via JGit
- Commit difference analysis between two commit SHAs
- Code file type detection (supports 30+ file extensions)
- Method and class extraction from diff content
- Module path analysis for package-based matching

### TestCaseRecommendationService  
Core recommendation engine:
- Multi-dimensional scoring algorithm implementation
- Test case to code change matching logic
- Priority-based result sorting and filtering
- Support for different recommendation strategies

### TestCaseService
Handles test case file processing:
- Multi-format parser (Java, CSV, XMind, Excel)
- @Test annotation extraction from Java source
- XMind ZIP file processing with XML parsing
- CSV format validation and column mapping

## Development Patterns

### Error Handling Strategy
- Controllers return appropriate HTTP status codes
- Service layer throws descriptive exceptions
- Git operations handle authentication and network failures
- File processing includes format validation and size limits

### Testing Architecture
Comprehensive test coverage across all layers:
- **Unit tests** - Service and utility logic with mocking
- **Integration tests** - @DataJpaTest for repository layer
- **Web layer tests** - @WebMvcTest for controller endpoints
- **End-to-end tests** - Full application stack testing

### Entity Status Management
GitRepository entities follow a defined lifecycle:
1. **CREATED** - Initial state after repository creation
2. **CLONING/UPDATING** - During Git operations  
3. **READY** - Successfully cloned and available for analysis
4. **ERROR** - Failed operations with error state preservation

### File Upload Security
- File type validation based on extensions and content analysis
- Size limitations enforced at Spring Boot level
- XMind files processed with XXE attack prevention
- Temporary file cleanup after processing

## Frontend Integration Points

### API Endpoints Structure
- `/api/repositories/*` - Repository management operations
- `/api/testcases/*` - Test case upload and management  
- `/api/analysis/*` - Code change analysis triggers
- `/api/recommendations/*` - Test case recommendation requests

### File Upload Endpoints
- Support multipart/form-data for file uploads
- Return structured JSON responses with validation errors
- Progress tracking for large file processing operations

## Special Considerations

### Workspace Directory Management
- Git repositories cloned to `./workspace/{repository-name}/`
- Directory cleanup handled automatically on repository deletion
- Concurrent access protection for Git operations

### Memory Management
- H2 in-memory database requires sufficient heap space for large datasets
- Git repository size should be monitored for performance
- Large file uploads may require JVM memory tuning

### XMind File Processing Complexity
XMind files are ZIP archives containing XML. The parsing process:
1. Extract ZIP entries to find `content.xml`
2. Parse XML with DOM4J and Jaxen for XPath support
3. Recursively traverse topic hierarchy to extract test cases
4. Handle various XMind format versions and structures

This architecture provides a robust foundation for intelligent test case selection based on code change analysis, with comprehensive testing and multiple input format support.