# Test Strategy Template

**Purpose:** Standard testing patterns for this project. Reference in design specs.

**For debugging process:** Use `superpowers:systematic-debugging` skill.

---

## 1. Merge Failure Prevention

| # | Failure | Prevention |
|---|---------|------------|
| 1 | Stale Gradle cache | Run `./gradlew clean build` before merge |
| 2 | Duplicate exception handlers | One per module in `controller` package |
| 3 | Gradle syntax errors | Avoid unless strictly needed |
| 4 | Test mock mismatches | Follow conventions below |

---

## 2. Backend Test Conventions

### Verify ALL Fields
```java
// WRONG - only checks one field
assertEquals("Updated", response.getName());

// CORRECT - verify every field
assertEquals("Updated", saved.getName());
assertEquals("Technology", saved.getIndustry());
```

### Error Codes
```java
// WRONG
assertEquals("PROD001", response.getErrorCode());

// CORRECT - reference constant
assertEquals(ProductNotFoundException.ERROR_CODE, response.getErrorCode());
```

### Mock Void Methods
```java
// WRONG
when(service.voidMethod(...)).thenThrow(...);

// CORRECT
doThrow(new Exception()).when(service).voidMethod(...);
```

### HTTP Status Codes
| Method | Success | Failure |
|--------|---------|---------|
| POST (create) | 201 | 400/409 |
| GET | 200 | 404 |
| PUT | 200 | 400/404 |
| DELETE | 204 | 404 |

---

## 3. Frontend Test Conventions

### API Response Shape
```typescript
// WRONG - assumed shape
{ customerType: 'INDIVIDUAL', firstName: 'John' }

// CORRECT - matches actual API
{ type: 'INDIVIDUAL', name: 'John Doe' }
```

### Test Both Modes
```typescript
describe('CustomerForm', () => {
  describe('Create Mode', () => { /* ... */ });
  describe('Edit Mode', () => { /* ... */ });  // Required!
});
```

---

## 4. Integration Tests (Required Pattern)

### Database Tests
```java
@SpringBootTest(classes = BankingApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = AFTER_CLASS)  // If using @MockBean
class MyTest extends AbstractIntegrationTest {

    @Autowired
    private MyRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAllInBatch();
    }
}
```

### Controller Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyControllerTest {
    @Autowired
    private MockMvc mockMvc;
    // DON'T use @WebMvcTest - causes mock conflicts
}
```

---

## 5. Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| Flyway checksum mismatch | Add `flyway.clean-on-validation-error: true` to `application-test.yml`. **NEVER rename applied migrations.** |
| Duplicate mock definition | Don't mock internal services. Only mock external adapters. |
| FK constraint violation | Delete in dependency order: child → parent |
| Seed data interfering | Add explicit `@BeforeEach` cleanup |

### application-test.yml (Required)
```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:15-alpine:///test
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    clean-on-validation-error: true
    enabled: true
```

---

## 6. Pre-Merge Checklist

- [ ] `./gradlew clean build` passes (clean, never incremental)
- [ ] Tests extend `AbstractIntegrationTest` for database tests
- [ ] `@BeforeEach` cleanup for all repositories
- [ ] Mock external adapters only (not internal services)
- [ ] All request/response fields verified
- [ ] Error codes reference `Exception.ERROR_CODE` constants
- [ ] TOTP tests disabled (timing-sensitive)

---

## 7. Related Documents

- Debugging: `superpowers:systematic-debugging` skill
- TDD: `superpowers:test-driven-development` skill
- Lesson: `docs/lesson-learned/2026-03-24-test-infrastructure-flyway-mock-conflicts.md`
- Report: `docs/superpowers/reports/test-infra-fix-report.md`
