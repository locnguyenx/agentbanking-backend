## Testing Rule & Testcontainers Rationale

### Rule: "NEVER write tests that test mocked behavior"

**From AGENTS.md (.agents/rules/testing-debugging.md):**
- Internal services should NOT be mocked - tested via real Feign calls

### Why Testcontainers Required

After removing `@MockBean` from integration tests, tests fail with `ConnectException` when external Docker services are unavailable. The solution uses Testcontainers with **real service images**, NOT mocks:

- `LedgerIntegrationTest` now uses `GenericContainer` for rules-service and switch-adapter-service
- Tests make real HTTP calls via Feign clients
- Service URLs injected via `@DynamicPropertySource`
- Each test execution starts fresh containers in isolation

**Key principle:** We're testing real service behavior, not mocks. Testcontainers provides real service instances that respond to actual HTTP requests.

### Test Implementation

```java
static GenericContainer<?> rulesService = new GenericContainer<>(
    DockerImageName.parse("agentbanking/rules-service:latest")
).withNetwork(NETWORK)
 .withNetworkAliases("rules-service")
 .withExposedPorts(8081)
 .withEnv("SPRING_PROFILES_ACTIVE", "test")
 // ...
 .waitingFor(Wait.forHttp("/actuator/health"));

static {
    rulesService.start();
}

@DynamicPropertySource
static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("rules-service.url", () -> "http://rules-service:8081");
}
```

### What Still Needs to Happen

Docker images must be built before tests can run:
```bash
docker build -t agentbanking/rules-service:latest ./services/rules-service
docker build -t agentbanking/switch-adapter-service:latest ./services/switch-adapter-service
docker build -t agentbanking/onboarding-service:latest ./services/onboarding-service
docker build -t agentbanking/ledger-service:latest ./services/ledger-service
docker build -t agentbanking/auth-iam-service:latest ./services/auth-iam-service
docker build -t agentbanking/gateway:latest ./gateway
```

Then run tests:
```bash
./gradlew :services:ledger-service:test --tests "com.agentbanking.ledger.integration.LedgerIntegrationTest"
```