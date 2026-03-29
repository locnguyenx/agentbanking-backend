
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

## **Integration tests for EXTERNAL APIs**

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
To run tests:
# Start services
docker-compose --profile infra --profile backend --profile gateway up -d
# Run tests
./gradlew :gateway:test --tests "ExternalApiIntegrationTest"

# Integration testing

**Summary: BDD-Aligned Integration Tests Created**

BddAlignedIntegrationTest test classe with ~160 test methods covering all 120 BDD scenarios plus additional RBAC tests. All tests compile successfully.

**Role Coverage**

Role	Token Generator Method	Test Coverage
Agent (MICRO)	getMicroAgentToken()	All transaction tests
Agent (STANDARD)	getStandardAgentToken()	All transaction tests
Agent (PREMIER)	getPremierAgentToken()	High-volume tests
Bank Operator	getBankOperatorToken()	Backoffice tests
IT Admin	getItAdminToken()	User management tests
Compliance Officer	getComplianceOfficerToken()	Terminal unfreeze tests
Maker	getMakerToken()	Dispute/discrepancy tests
Checker	getCheckerToken()	Approval tests
Supervisor	getSupervisorToken()	SLA escalation tests

## Summary

**Created Files**

| File	| Purpose	|
| mock-jwt-issuer/	| Mock JWT issuer service for testing	|
| mock-jwt-issuer/build.gradle	| Build configuration	|
| mock-jwt-issuer/src/main/java/com/agentbanking/mock/MockJwtIssuerApplication.java	| Spring Boot application	|
| mock-jwt-issuer/src/main/java/com/agentbanking/mock/MockJwtController.java	| JWT token generation endpoints	|
| mock-jwt-issuer/src/main/resources/application.yaml	| Configuration	|
| mock-jwt-issuer/Dockerfile	| Docker image	|
| docker-compose.test.yml	| Docker Compose override for testing	|
| gateway/src/test/java/com/agentbanking/gateway/integration/BddAlignedIntegrationTest.java	| BDD-aligned tests	|

**Modified Files**
| File	| Change	|
| gateway/build.gradle	| Excluded spring-boot-starter-web from common module	|
| gateway/src/main/java/com/agentbanking/gateway/config/SecurityConfig.java	| Added JWT decoder configuration	|
| gateway/src/main/java/com/agentbanking/gateway/filter/JwtAuthFilter.java	| Updated to validate JWT directly	|
| docker-compose.yml	| Added mock-jwt-issuer service	|

---

## Running Options

**Option 1: Production Mode (Real JWT Issuer)**
```bash
docker-compose --profile infra --profile backend --profile gateway up -d
```
- Gateway uses https://auth.agentbanking.com for JWT validation
- Real JWT tokens required

**Option 2: Testing Mode (Mock JWT Issuer)**
```bash
docker-compose --profile infra --profile backend --profile gateway --profile test up -d
```
- Gateway uses http://mock-jwt-issuer:8090 for JWT validation
- Mock JWT issuer available at http://localhost:8091

**Get Test Tokens**
```bash
# Agent token
curl -s 'http://localhost:8091/token/agent?agentId=AGT-01&tier=MICRO' | jq -r '.access_token'
# Bank operator token
curl -s 'http://localhost:8091/token/operator?userId=OPERATOR-01' | jq -r '.access_token'
# IT admin token
curl -s 'http://localhost:8091/token/admin?userId=ADMIN-01' | jq -r '.access_token'
# Maker token
curl -s 'http://localhost:8091/token/maker?userId=OFFICER-01' | jq -r '.access_token'
# Checker token
curl -s 'http://localhost:8091/token/checker?userId=OFFICER-02' | jq -r '.access_token'
```

**Run Tests**
```bash
# Run all tests (works with or without mock JWT issuer)
./gradlew :gateway:test
# Run specific test class
./gradlew :gateway:test --tests "BddAlignedIntegrationTest"
./gradlew :gateway:test --tests "ExternalApiIntegrationTest"
```