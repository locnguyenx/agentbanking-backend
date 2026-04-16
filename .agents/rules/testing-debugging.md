---
trigger: glob
globs: **/src/test/**/*
---
# Testing, Issue tracking & Debugging Rules
## Testing

- ALL TEST FAILURES ARE YOUR RESPONSIBILITY, even if they're not your fault. The Broken Windows theory is real.
- Never delete a test because it's failing. Instead, raise the issue with Loc. 
- Tests MUST comprehensively cover ALL functionality. 
- YOU MUST NEVER write tests that "test" mocked behavior. If you notice tests that test mocked behavior instead of real logic, you MUST stop and warn Loc about them.
- YOU MUST NEVER implement mocks in end to end tests. We always use real data and real APIs.
- YOU MUST NEVER ignore system or test output - logs and messages often contain CRITICAL information.
- Test output MUST BE PRISTINE TO PASS. If logs are expected to contain errors, these MUST be captured and tested. If a test is intentionally triggering an error, we *must* capture and validate that the error output is as we expect

## Issue tracking

- You MUST use your TodoWrite tool to keep track of what you're doing 
- You MUST NEVER discard tasks from your TodoWrite todo list without Loc's explicit approval

## Systematic Debugging Process

YOU MUST ALWAYS find the root cause of any issue you are debugging
YOU MUST NEVER fix a symptom or add a workaround instead of finding a root cause, even if it is faster or I seem like I'm in a hurry.

YOU MUST follow this debugging framework for ANY technical issue:

### Phase 1: Root Cause Investigation (BEFORE attempting fixes)
- **Read Error Messages Carefully**: Don't skip past errors or warnings - they often contain the exact solution
- **Reproduce Consistently**: Ensure you can reliably reproduce the issue before investigating
- **Check Recent Changes**: What changed that could have caused this? Git diff, recent commits, etc.

### Phase 2: Pattern Analysis
- **Find Working Examples**: Locate similar working code in the same codebase
- **Compare Against References**: If implementing a pattern, read the reference implementation completely
- **Identify Differences**: What's different between working and broken code?
- **Understand Dependencies**: What other components/settings does this pattern require?

### Phase 3: Hypothesis and Testing
1. **Form Single Hypothesis**: What do you think is the root cause? State it clearly
2. **Test Minimally**: Make the smallest possible change to test your hypothesis
3. **Verify Before Continuing**: Did your test work? If not, form new hypothesis - don't add more fixes
4. **When You Don't Know**: Say "I don't understand X" rather than pretending to know

### Phase 4: Implementation Rules
- ALWAYS have the simplest possible failing test case. If there's no test framework, it's ok to write a one-off test script.
- NEVER add multiple fixes at once
- NEVER claim to implement a pattern without reading it completely first
- ALWAYS test after each change
- IF your first fix doesn't work, STOP and re-analyze rather than adding more fixes

# Testing Guidelines for this Project

## Test stack

* Unit tests: JUnit 5 + Mockito
* Architecture tests: ArchUnit (enforce hexagonal rules)
* Integration tests: Spring Boot Test + Testcontainers
* End to end tests: Spring Boot Test + real backend using docker compose
* BDD scenarios in `*-bdd.md` are the acceptance criteria

## Testing Strategy
- **Integration Tests:** Use Testcontainers to spin up real dependencies (e.g., PostgreSQL, Redis, Kafka).
**EXCEPTION:** 
  - may have issue in using Testcontainers on Windows, in this case we can use docker containers for required infra, but need to do test data cleanup
  - **Temporal** (workflow engine) is not available in Testcontainers, but requires manual Docker setup.
- **End to end tests:** use real backend with docker compose

- **Current setup:**
- Most E2E tests use BaseIntegrationTest → connect to real docker-compose services
- ExternalApiIntegrationTest uses BaseGatewayIntegrationTest → uses TestContainers/H2
- `-PtestProfile=local` now properly switches to real Docker Compose databases

### Required Docker Services
Temporal is required for orchestrator-service tests and gateway e2eTest

| Service | Purpose | Port | Reason |
|---------|---------|------|--------|
| Temporal | Workflow engine | 7233 | Not available as TestContainers |

**Starting Temporal for Testing**
```bash
# Start Temporal
docker compose up -d temporal temporal-postgres
# or with ui
docker compose up -d temporal temporal-postgres temporal-ui

# Verify Temporal is running
docker ps | grep temporal
```

### TestContainers (Automatic)

- **Testcontainers Usage:** NEVER use manual Docker commands to start dependent services (like Redis or Postgres). Always use Testcontainers to manage the container lifecycle (start/stop).
- **Reuse:** Enable Testcontainers reuse (`TC_REUSE=true` or similar) to improve performance.

- Most infrastructure is handled automatically:

```java
// common/src/testFixtures/java/com/agentbanking/common/test/AbstractIntegrationTest.java
static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine");
static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
```

**Note:** TestContainers requires Docker to be running. If tests fail with "Cannot connect to Docker daemon", ensure Docker Desktop is running.

## Test Profile Configuration

* Tests use the `tc` (TestContainers) profile:

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

* Tests use the `local` (real backend services) profile:
  application-local.yaml - Local profile with real backend services
  Verified tests work with real databases via -PtestProfile=local

**Using profile in integration test**
Usage:
- ./gradlew test → uses tc profile (Testcontainers, default)
- ./gradlew test -PtestProfile=local → uses test profile (localhost Docker Compose)

---

## Backoffice UI Testing

Refer to @docs/superpowers/specs/2026-04-02-backoffice-test-architecture.md

## Integration test rules

The integration test must test the actual endpoint without mocking the repository, to test that the repository call is compatible with the transaction contex

Example:
```java
@Test
void getBalance_endpoint_returnsAgentBalance() {
    // Uses real database, tests actual transaction behavior
    ResponseEntity<Map> response = restTemplate.getForEntity(
        "/internal/balance/{agentId}", Map.class, AGENT_ID);
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsKey("balance");
}
```

## Environment & Docker Setup
- **Docker Socket:** Ensure the Docker socket (`/var/run/docker.sock`) is accessible to Testcontainers.
- **MacOS Fix:** If running locally on Mac, ensure `/var/run/docker.sock` maps correctly to `$HOME/.docker/run/docker.sock`.
- **CI/CD:** <to be done>
- Start infra docker containers (required for e2e tests):
```bash
# all services
docker compose --profile all up -d
# All containers except frontend
docker compose --profile infra --profile backend --profile gateway --profile mocks up -d
```

## Test Commands
- **Running Integration Tests**

```bash
# Run all integration tests (TestContainers + Temporal must be running)
./gradlew test

# Run specific service tests
./gradlew :services:orchestrator-service:test

# Run orchestrator tests only (requires Temporal)
./gradlew :services:orchestrator-service:test --tests "*Orchestrator*"

# Run tests with coverage
./gradlew test jacocoTestReport
```
- **Integration tests for EXTERNAL APIs:**
```bash
# Run tests
./gradlew :gateway:test --tests "ExternalApiIntegrationTest"
```
- **Run gateway e2e integration tests:**
```bash
# Run e2e integration tests and automatically runs cleanup first via cleanE2eTestData
# e2e tests required all services running in docker containers
./gradlew :gateway:e2eTest --no-daemon
# can runt only SelfContainedOrchestratorE2ETest to test transaction
```

## Known Issues & Troubleshooting
### Testcontainers
- If tests fail with "cannot connect to Docker daemon", verify that Testcontainers has access to the local Docker engine.

### Troubleshooting Docker for Tests
- Commands:
```bash
# Verify Temporal is running
docker ps | grep temporal

# View Temporal logs
docker logs agentbanking-backend-temporal-1
```
- When do troubleshooting with docker containers, whenever a fix is done, remember to rebuild the container with `--no-cache` option: 
  Example: 
  ```bash
  docker compose build --no-cache backoffice
  ```

**Clean Up**
```bash
# Remove Temporal containers
docker compose down temporal temporal-postgres temporal-ui
```
### Issue with API gateway
- When the gateway's API returns a 500 error:
  1. Firstly, get the mapping info in gateway configuration (@gateway/src/main/resources/application.yaml)
  2. Verify that the mapped internal api is working properly
  3. If internal api is ok, check if the mapping rewrite setting works.

### Data inconsistence issue
In case the Data inconsistence issue happens with a high numbers of transactions, and you're stuck in debugging. You should try reproduce the issue for analysis.

We may need to reset the system to have a fresh env that is ready for testing:
```bash
./gradlew resetSystem
```
This gives you a fresh system with:
- ✅ No transaction/workflow data
- ✅ No demo users/agents
- ✅ Only essential auth foundation (admin + roles + permissions)

---

See @docs/lessons-learned/*.md for lessons learned, useful in trouble shooting