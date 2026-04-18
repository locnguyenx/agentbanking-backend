# Test Architecture Design

## Overview

This document describes the testing architecture for the Agent Banking backend microservices.

## Test Classification

| Test Type | Description | Infrastructure | Internal Services | Use Case |
|----------|-------------|----------------|------------------|----------|
| **Unit Test** | Pure logic, no external dependencies | All mocked | Mocked | Business logic validation |
| **Component Test** | Real infra (DB/Redis/Kafka) + mocked internal services | Testcontainers | @MockBean | Controller/API testing |
| **Integration Test** | Real infra + real internal service calls | Testcontainers + docker compose | Real HTTP calls | Service-to-service |
| **E2E Test** | Full stack, all real services | docker compose | Real services | End-to-end scenarios |

## Current Implementation

### Component Tests (Testcontainers)

All service tests use:
- **PostgreSQL**: via Testcontainers
- **Redis**: via Testcontainers  
- **Kafka**: via Testcontainers
- **Internal services**: mocked via @MockBean

#### Services with Component Tests

| Service | Test Class | Profile |
|---------|-----------|---------|
| ledger-service | `LedgerComponentTest`, `LedgerControllerComponentTest` | tc |
| onboarding-service | `OnboardingControllerComponentTest` | tc |
| rules-service | `RulesControllerComponentTest` | tc |
| biller-service | `BillerControllerComponentTest` | tc |
| auth-iam-service | `AuthControllerComponentTest` | tc |

### Integration Tests

Gateway IntegrationTest classes remain as IntegrationTest (will use true integration testing with docker compose for real backend services).

## Naming Conventions

```
*UnitTest           - Unit tests (if any)
*ComponentTest     - Component/slice tests with Testcontainers
*IntegrationTest   - True integration tests (real services)
*E2ETest          - End-to-end tests
```

## Running Tests

### Component Tests

```bash
# Run all component tests
./gradlew componentTest

# Run individual service component tests
./gradlew :services:ledger-service:test --tests "com.agentbanking.ledger.component.*"
./gradlew :services:onboarding-service:test --tests "com.agentbanking.onboarding.component.*"
./gradlew :services:rules-service:test --tests "com.agentbanking.rules.component.*"
./gradlew :services:biller-service:test --tests "com.agentbanking.biller.component.*"
./gradlew :services:auth-iam-service:test --tests "com.agentbanking.auth.component.*"
```

### E2E Tests

```bash
./gradlew :gateway:cleanE2eTestData :gateway:e2eTest
```

## Design Decisions

### 1. Why Component Tests Instead of True Integration Tests?

**Reason:** Docker resource constraints in development/CI environments.

Running multiple microservices with Testcontainers simultaneously exhausts Docker resources:
- Each service needs PostgreSQL + Redis + Kafka
- 5 services × 3 containers = 15 containers
- Docker OOM kills occur in constrained environments

**Solution:** Component tests with mocked internal services provide fast, reliable testing while still validating:
- Database persistence
- Redis caching
- Kafka messaging
- HTTP contract with mocked downstream services

### 2. Testcontainers Profile (tc)

Uses `@ActiveProfiles("tc")` for Testcontainers-based testing:
- PostgreSQL 16-alpine
- Redis 7-alpine
- Kafka (confluentinc/cp-kafka:7.5.0)

### 3. Sequential Test Execution

The `componentTest` task runs each service's tests sequentially to avoid Docker resource exhaustion.

### 4. Why Not Spring Cloud Contract?

Spring Cloud Contract (WireMock) would require:
- Running all microservices in test mode
- Similar Docker resource issues
- Additional complexity for setup

Consider implementing when:
- CI environment has sufficient resources
- Contract testing between services is required

## Test Base Class

```java
@SpringBootTest
@ActiveProfiles("tc")
public abstract class AbstractIntegrationTest {
    // Shared Testcontainers instances
    public static final PostgreSQLContainer<?> postgres = ...
    public static final GenericContainer<?> redis = ...
    public static final KafkaContainer kafka = ...
}
```

## Future Improvements

1. **True Integration Tests**: After Docker resource issues resolved:
   - Use docker compose for backend services
   - Remove @MockBean from IntegrationTest classes

2. **Contract Testing**: Consider Spring Cloud Contract when:
   - More resources available
   - Service contracts need formal verification

3. **Test Reporting**: Generate test reports with traceability matrices

## Troubleshooting

### Docker Resource Issues

If tests fail with connection errors or container issues:

```bash
# Clean Docker
docker system prune -a

# Run fewer services at once
./gradlew :services:ledger-service:test --tests "com.agentbanking.ledger.component.*"
```

### Flaky Tests

Kafka tests may be flaky in constrained environments. Run individually:

```bash
./gradlew :services:ledger-service:test --tests "com.agentbanking.ledger.component.LedgerComponentTest"
```