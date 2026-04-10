
# Services testing
## Problem
Services were rebuilt from code that had missing configuration, exposing pre-existing bugs.

## Root Causes
1. Missing bean wiring: StpDecisionService, ReconciliationService were added to code but not registered in DomainServiceConfig
2. Missing annotations: DiscrepancyCaseRepositoryAdapter had no @Repository, BillerService had no @Service
3. Configuration mismatch: Feign clients referenced properties not in application.yaml
4. Dependency conflict: ledger-service packaged onboarding-service jar, causing Flyway version collision
5. Missing component scan: EfmEventPublisher in common package not scanned

## Why It Worked Before
Old Docker images were built from an earlier commit. New code was added in commit 342e310 without wiring to Spring.

## Lessons Learned
1. Build ≠ runs: Code compiles but Spring won't start without proper wiring
2. New classes need registration: Every @Service, @Repository, @Component must be scannable
3. Cross-service dependencies are fragile: ledger-service depending on onboarding-service caused Flyway conflict
4. Test Docker builds after every commit: Don't assume "it compiled, it works"
5. Don't touch working services when fixing unrelated issues (like OpenAPI specs)

## How to detect these issues?

Let me think about alternatives:
1. Docker build - Yes, but slow and heavy
2. Local Gradle build + manual start - Faster but still manual
3. Integration tests with Testcontainers - Can catch some issues but not all
4. Gradle bootRun task - Can test Spring context loading
5. Static analysis / custom checks - Could catch config mismatches

Let me suggest practical alternatives that are faster than full Docker build.

Not necessarily. Faster alternatives:
|	Method |	What It Detects |	Speed |
|	./gradlew bootRun |	Missing beans, config errors, Flyway conflicts |	~10s per service |
|	./gradlew bootTestRun |	Spring context loading failures |	~15s per service |
|	Spring Boot test with @SpringBootTest |	Full context startup |	~30s |
|	Docker build + run |	Everything |	~60s per service |

**Recommended workflow:**
```bash
# Fastest - test Spring context loads
./gradlew :services:ledger-service:bootRun --no-daemon &
sleep 10
curl -f http://localhost:8082/actuator/health || echo "FAILED"
kill %1
```

Or add to CI pipeline:
```bash
# In .github/workflows or CI script
./gradlew :services:*:bootJar
# Then test each service starts
for svc in rules ledger onboarding biller; do
  timeout 30 java -jar services/${svc}-service/build/libs/*.jar &
  sleep 15
  curl -f http://localhost:808*/actuator/health || exit 1
  kill %1
done
```
## Script guide

**1. Test Script** (scripts/verify-services-startup.sh)
- Tests all 6 services sequentially
- Uses unique ports to avoid conflicts
- Validates health endpoint after startup
- Shows error logs if service fails
- Cleans up processes on exit
**2. Updated AGENTS.md** (Law X)
- Enforces startup validation before committing
- Provides both local and Docker options

**Usage:**
```bash
# Test all services
./scripts/verify-services-startup.sh
# Test specific services
./scripts/verify-services-startup.sh rules-service ledger-service
```
> **Note:** Script requires PostgreSQL running locally. For full environment, use Docker instead.

# Integration tests for services

## **Integration tests for EXTERNAL APIs**

Test class: ExternalApiIntegrationTest.java

All 24 integration tests passed:
External API Tests:
- withdrawal ✓
- deposit ✓
- balance-inquiry ✓
- agent-balance ✓
- kyc-verify ✓
- kyc-biometric ✓
- bill-pay ✓
- topup ✓
- duitnow ✓
- retail-sale ✓
- pin-purchase ✓
- retail-cashback ✓
- ewallet-withdraw ✓
- ewallet-topup ✓
- essp-purchase ✓
Backoffice Tests:
- dashboard ✓
- agents-list ✓
- transactions ✓
- settlement ✓
- kyc-review-queue ✓
- audit-logs ✓
API Docs Tests:
- openapi-specs ✓
- openapi-specs-trailing-slash ✓

**To run tests:**

```bash
# Start services
docker-compose --profile infra --profile backend --profile gateway up -d
# Run tests
./gradlew :gateway:test --tests "ExternalApiIntegrationTest"
```
## Current issue with server integration tests

Investigate `ledger service`:
**Why tests didn't catch this issue:**
1. Unit tests (LedgerServiceTest.java) mock at the repository interface level:
      when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
      The mock doesn't verify if the method is appropriate for the transaction context - it just returns whatever is configured.
2. Integration tests (LedgerIntegrationTest.java) use @SpringBootTest with @MockBean - they mock external services but also mock the repository behavior, so no real JPA transaction is tested.
3. No real endpoint test - There's no test that calls /internal/balance/{agentId} with a real database to verify transaction behavior.
The gap: Tests mock at the wrong level. They test that the service calls the repository, but don't test that the repository call is compatible with the transaction context (@Transactional(readOnly = true)).
Recommendation: Add an integration test that tests the actual endpoint without mocking the repository:
@Test
void getBalance_endpoint_returnsAgentBalance() {
    // Uses real database, tests actual transaction behavior
    ResponseEntity<Map> response = restTemplate.getForEntity(
        "/internal/balance/{agentId}", Map.class, AGENT_ID);
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsKey("balance");
}

## Infra dependencies for integration test

- Default option: user Testcontainers for Postgresql + Redis + Kafka
- Fall back to Postgresql container (i.e in Windows):

**Usage:**
- ./gradlew test → uses tc profile (Testcontainers, default)
- ./gradlew test -PtestProfile=local → uses test profile (localhost Docker Compose)

```bash
# Fallback (localhost Docker Compose)
docker compose up -d
./gradlew test -PtestProfile=local
```
---

## Docker for Integration Testing

### Overview

Integration tests use **TestContainers** which automatically manages Docker containers for test dependencies:
- **PostgreSQL**: Started automatically per test class
- **Redis**: Started automatically per test class  
- **Kafka**: Started automatically per test class

The only exception is **Temporal** (workflow engine) which requires manual Docker setup.

### Required Docker Services

| Service | Purpose | Port | Reason |
|---------|---------|------|--------|
| Temporal | Workflow engine | 7233 | Not available as TestContainers |

### Starting Temporal for Testing

```bash
# Start Temporal (required for orchestrator-service tests)
docker compose up -d temporal temporal-postgres
# or with ui
docker compose up -d temporal temporal-postgres temporal-ui

# Verify Temporal is running
docker ps | grep temporal
```

### TestContainers (Automatic)

Most infrastructure is handled automatically:

```java
// common/src/testFixtures/java/com/agentbanking/common/test/AbstractIntegrationTest.java
static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine");
static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
```

**Note:** TestContainers requires Docker to be running. If tests fail with "Cannot connect to Docker daemon", ensure Docker Desktop is running.

### Running Integration Tests

```bash
# Run all integration tests (TestContainers + Temporal must be running)
./gradlew test

# Run specific service tests
./gradlew :services:orchestrator-service:test

# Run orchestrator tests only (requires Temporal)
./gradlew :services:orchestrator-service:test --tests "*Orchestrator*"

# Run tests with coverage
./gradlew test jacocoTestReport

# Run e2e integration tests and automatically runs cleanup first via cleanE2eTestData
# e2e tests required all services running in docker containers
./gradlew :gateway:e2eTest --no-daemon
# can runt only SelfContainedOrchestratorE2ETest to test transaction
```

### Test Profile Configuration

Tests use the `tc` (TestContainers) profile:

```yaml
# services/*/src/test/resources/application-tc.yaml
spring:
  main:
    allow-bean-definition-overriding: true
  jpa:
    hibernate:
      ddl-auto: none
  flyway:
    enabled: true
    locations: filesystem:src/main/resources/db/migration
```

### Troubleshooting Docker for Tests

```bash
# Check if Docker is running
docker info

# Verify Temporal is running
docker ps | grep temporal

# View Temporal logs
docker logs agentbanking-backend-temporal-1

# Check Temporal UI (for debugging workflows)
open http://localhost:8082
```

### Clean Up

```bash
# Remove Temporal containers
docker compose down temporal temporal-postgres temporal-ui

---