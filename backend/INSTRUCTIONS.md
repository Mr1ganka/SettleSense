# Backend Development Guide

This document covers the Spring Boot backend for SettleSense.

---

## Overview

| Property | Value |
|----------|-------|
| Framework | Spring Boot 3.x |
| Language | Java 17 |
| Build Tool | Gradle (8.x) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |

---

## Prerequisites

- Java 17+ (JDK)
- PostgreSQL 16 (running via Docker or local)
- Gradle 8.x (optional, wrapper included)

Verify Java installation:
```powershell
java -version
```

Should output something like:
```
java version "17.0.x"
```

---

## Quick Start

### 1. Start Database

```powershell
docker compose -f ../docker/docker-compose.yml up -d
```

### 2. Run Backend

```powershell
cd backend
.\gradlew.bat bootRun
```

The application will start on `http://localhost:8080`.

On first run, Flyway automatically creates the database schema from migration files.

### 3. Verify

```powershell
# Check health endpoint
curl http://localhost:8080/api/health
```

---

## Development Commands

### Run Application

```powershell
# Standard run
.\gradlew.bat bootRun

# Debug mode (waits for debugger on port 5005)
.\gradlew.bat bootRun --debug
```

### Run Tests

```powershell
# Run all tests
.\gradlew.bat test

# Run specific test class
.\gradlew.bat test --tests "com.kelvin.settlesense.api.PhaseOneControllerTests"

# Run with coverage
.\gradlew.bat test jacocoTestReport
```

### Build

```powershell
# Build JAR file
.\gradlew.bat jar

# Build with dependencies
.\gradlew.bat bootJar

# Clean and build
.\gradlew.bat clean build
```

### Other Commands

```powershell
# Check dependencies
.\gradlew.bat dependencies

# View dependency tree
.\gradlew.bat dependencies --configuration runtimeClasspath

# List available tasks
.\gradlew.bat tasks

# Generate IDE files
.\gradlew.bat eclipse
.\gradlew.bat idea
```

---

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/kelvin/settlesense/
│   │   │   ├── SettleSenseApplication.java    # Main entry point
│   │   │   │
│   │   │   ├── api/                           # REST Controllers
│   │   │   │   ├── ActivityController.java
│   │   │   │   ├── ApiExceptionHandler.java
│   │   │   │   ├── BalanceController.java
│   │   │   │   ├── ExpenseController.java
│   │   │   │   ├── GroupController.java
│   │   │   │   ├── SettlementController.java
│   │   │   │   ├── SystemController.java
│   │   │   │   └── UserController.java
│   │   │   │
│   │   │   ├── config/                        # Configuration
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── TimeConfig.java
│   │   │   │
│   │   │   └── domain/                        # Business Logic
│   │   │       ├── model/                     # JPA Entities
│   │   │       │   ├── ActivityEvent.java
│   │   │       │   ├── BalanceProjection.java
│   │   │       │   ├── Expense.java
│   │   │       │   ├── ExpenseSplit.java
│   │   │       │   ├── Friendship.java
│   │   │       │   ├── Group.java
│   │   │       │   ├── GroupMember.java
│   │   │       │   ├── LedgerEntry.java
│   │   │       │   ├── Settlement.java
│   │   │       │   └── User.java
│   │   │       │   └── [status enums]
│   │   │       │
│   │   │       ├── repository/               # Data Access
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── GroupRepository.java
│   │   │       │   ├── ExpenseRepository.java
│   │   │       │   └── ...
│   │   │       │
│   │   │       └── service/                   # Business Services
│   │   │           ├── UserWorkflowService.java
│   │   │           ├── GroupWorkflowService.java
│   │   │           ├── ExpenseWorkflowService.java
│   │   │           ├── SplitCalculator.java
│   │   │           └── ...
│   │   │
│   │   └── resources/
│   │       ├── application.properties         # App configuration
│   │       ├── application-test.properties    # Test config
│   │       └── db/migration/                   # Flyway migrations
│   │           ├── V1__create_app_metadata.sql
│   │           └── V2__create_phase_1_domain_model.sql
│   │
│   └── test/
│       └── java/com/kelvin/settlesense/
│           ├── api/                           # Controller tests
│           │   └── PhaseOneControllerTests.java
│           └── domain/service/                # Service tests
│               ├── PhaseOneWorkflowIntegrationTests.java
│               ├── LedgerAndBalanceServiceTests.java
│               └── SplitCalculatorTests.java
│
├── build.gradle                    # Gradle build file
├── settings.gradle                # Gradle settings
├── gradlew                        # Unix wrapper script
├── gradlew.bat                    # Windows wrapper script
└── HELP.md                        # Gradle help
```

---

## Configuration

### Application Properties

Located in `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/settlesense
spring.datasource.username=settlesense
spring.datasource.password=settlesense
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# Server
server.port=8080

# Logging
logging.level.com.kelvin.settlesense=INFO
logging.level.org.springframework.web=INFO
```

### Test Properties

Located in `src/main/resources/application-test.properties`:

Uses in-memory H2 database for tests:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.flyway.enabled=false
```

---

## Adding New Features

### Adding a New Entity

1. **Create Entity** in `domain/model/`:
   ```java
   @Entity
   @Table(name = "my_entity")
   public class MyEntity {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       
       private String name;
       // getters, setters, constructors
   }
   ```

2. **Create Repository** in `domain/repository/`:
   ```java
   public interface MyEntityRepository extends JpaRepository<MyEntity, Long> {
   }
   ```

3. **Create Service** in `domain/service/`:
   ```java
   @Service
   public class MyEntityService {
       private final MyEntityRepository repository;
       
       public MyEntity create(String name) {
           return repository.save(new MyEntity(name));
       }
   }
   ```

4. **Create Controller** in `api/`:
   ```java
   @RestController
   @RequestMapping("/api/myentities")
   public class MyEntityController {
       private final MyEntityService service;
       
       @PostMapping
       public MyEntityResponse create(@RequestBody CreateRequest request) {
           return MyEntityResponse.from(service.create(request.name()));
       }
   }
   ```

### Adding a Database Migration

Create a new SQL file in `src/main/resources/db/migration/`:

```sql
-- V3__add_new_feature.sql
ALTER TABLE my_entity ADD COLUMN new_column VARCHAR(255);
```

Flyway will automatically apply the migration on next startup.

---

## Testing

### Running Tests

```powershell
# All tests
.\gradlew.bat test

# Specific test class
.\gradlew.bat test --tests "com.kelvin.settlesense.api.PhaseOneControllerTests"

# Tests in specific package
.\gradlew.bat test --tests "com.kelvin.settlesense.domain.service.*"
```

### Test Structure

Tests are organized in:
- `src/test/java/com/kelvin/settlesense/api/` - Controller integration tests
- `src/test/java/com/kelvin/settlesense/domain/service/` - Service unit tests

### Adding Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class MyControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldCreateEntity() throws Exception {
        mockMvc.perform(post("/api/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test"));
    }
}
```

---

## API Endpoints

### User API

```java
@RestController
@RequestMapping("/api/users")
class UserController {
    // POST /api/users - Register user
    // GET  /api/users - List users
    // GET  /api/users/{id} - Get user
    // PUT  /api/users/{id} - Update user
}
```

### Group API

```java
@RestController
@RequestMapping("/api/groups")
class GroupController {
    // POST   /api/groups - Create group
    // GET    /api/groups - List groups
    // GET    /api/groups/{id} - Get group
    // GET    /api/groups/{id}/members - List members
    // POST   /api/groups/{id}/members - Add member
    // POST   /api/groups/{id}/members/{userId}/leave - Leave
    // POST   /api/groups/{id}/members/{userId}/remove - Remove
    // POST   /api/groups/{id}/archive - Archive
}
```

### Expense API

```java
@RestController
@RequestMapping("/api")
class ExpenseController {
    // GET    /api/groups/{id}/expenses - List expenses
    // POST   /api/groups/{id}/expenses - Post expense
    // POST   /api/expenses/{id}/cancel - Cancel expense
}
```

### Settlement API

```java
@RestController
@RequestMapping("/api")
class SettlementController {
    // GET    /api/groups/{id}/settlements - List settlements
    // POST   /api/groups/{id}/settlements - Record settlement
    // POST   /api/settlements/{id}/cancel - Cancel settlement
}
```

### Balance API

```java
@RestController
@RequestMapping("/api")
class BalanceController {
    // GET /api/groups/{id}/balances - Get balances
    // GET /api/groups/{id}/settlement-suggestions - Get suggestions
}
```

### Activity API

```java
@RestController
@RequestMapping("/api")
class ActivityController {
    // GET /api/groups/{id}/activity - Get group activity feed
}
```

---

## Security

Current security configuration (`config/SecurityConfig.java`):

```java
@Configuration
class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/api/**").permitAll()
                .anyRequest().authenticated())
            .build();
    }
}
```

**Note**: CSRF is disabled for development. For production, enable CSRF and implement proper authentication.

---

## Troubleshooting

### Port 8080 Already in Use

Change port in `application.properties`:
```properties
server.port=8081
```

### Database Connection Failed

1. Check PostgreSQL is running: `docker ps`
2. Check credentials in `application.properties`
3. Verify database exists: `docker exec -it settlesense-postgres psql -U settlesense -l`

### Tests Failing

```powershell
# Clean and rerun
.\gradlew.bat clean test

# Run with verbose output
.\gradlew.bat test --info --stacktrace
```

### Flyway Migration Issues

```powershell
# Clean Flyway history (WARNING: deletes data)
docker exec -it settlesense-postgres psql -U settlesense -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

Then restart the application to reapply migrations.

---

## Known Limitations

- No authentication/authorization (Phase 2)
- No pagination on list endpoints (Phase 3)
- Single payer for expenses only (Phase 5)
- All split types implemented (EQUAL, EXACT, PERCENTAGE, SHARE)
- No expense editing - cancellation only (Phase 5)

---

## Future Enhancements

- JWT authentication
- Pagination and filtering
- Multi-payer expenses
- Extended split types (EXACT, PERCENTAGE, SHARE)
- Expense editing
- Activity logging improvements
- API versioning