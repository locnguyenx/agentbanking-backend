
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
**Would you like me to add such a test?**
Thinking: The user wants me to add integration tests for all endpoints in the ledger service. Let me first understand all the endpoints available in the LedgerController, then create comprehensive integration tests.
→ Read services\ledger-service\src\main\java\com\agentbanking\ledger\infrastructure\web\LedgerController.java 
Thinking: Now I need to create a comprehensive integration test that tests all these endpoints. Let me first check the existing test structure to understand how to add new tests.
→ Read services\ledger-service\src\test\java\com\agentbanking\ledger\integration\LedgerIntegrationTest.java 
Thinking: Now I need to create comprehensive REST controller tests for all ledger service endpoints. Let me look at the DTOs first to understand the request structure:
✱ Glob "*.java" in services\ledger-service\src\main\java\com\agentbanking\ledger\infrastructure\web\dto (3 matches)
Thinking: Let me read the DTOs to understand their structure.
→ Read services\ledger-service\src\main\java\com\agentbanking\ledger\infrastructure\web\dto\WithdrawalRequest.java 
→ Read services\ledger-service\src\main\java\com\agentbanking\ledger\infrastructure\web\dto\DepositRequest.java 
Thinking: Now I'll create a comprehensive REST controller test that tests all endpoints. I'll use MockMvc to test all the endpoints.

# API Gateway Integration testing

E2E Testing Infrastructure Complete!

Here's how to run the E2E tests:

**Created:**
- common.sh - Shared test helpers (assert_*, api_call, etc.)
- 18 BDD section scripts (01-18)
- Updated seed-test-data.sh for all service data
- Updated run-all-e2e-tests.sh master orchestrator

**Quick Start**
```bash
# Option 1: Full automated test (starts Docker, seeds data, runs tests)
./scripts/e2e-tests/run-all-e2e-tests.sh
# Option 2: Manual steps
# 1. Start services
docker compose --profile all up -d
# 2. Wait for services to be healthy (check http://localhost:8087/actuator/health)
# 3. Seed test data
./scripts/e2e-tests/seed-test-data.sh
# 4. Run BDD tests for auth-iam
./scripts/e2e-tests/bdd-e2e-tests.sh
# 5. Run single section:
./scripts/e2e-tests/01-rules-fee-engine.sh
```