# Integration Testing Guide

This guide explains how to run and maintain the integration and E2E testing suite for the Agent Banking Platform.

## Test Architecture

The platform uses a multi-layered testing approach:
1. **Unit Tests**: JUnit 5 + Mockito for individual component logic.
2. **Integration Tests**: Spring Boot Test + Testcontainers for service-level integration with real databases and messaging.
3. **End-to-End (E2E) Tests**: Full system verification using Docker Compose services.

## Prerequisites

- **Docker Desktop**: Must be running.
- **MacOS Native DNS**: For local Mac execution, ensure the following dependency is in `gateway/build.gradle`:
  ```gradle
  testImplementation 'io.netty:netty-resolver-dns-native-macos:4.1.109.Final:osx-aarch_64'
  ```
- **Temporal**: Must be running in Docker.

## Running Tests

### 1. Start Infrastructure
```bash
docker compose --profile infra --profile backend up -d
```

### 2. Run Integration Tests (Testcontainers)
```bash
./gradlew test
```

### 3. Run E2E Tests (Real Services)
Ensure all services are healthy, then run:
```bash
./gradlew :gateway:e2eTest -PtestProfile=local
# Contract Tests 
./gradlew :gateway:e2eTest --tests "OpenApiContractE2ETest"
```

> [!TIP]
> Use `-PtestProfile=local` to force tests to use the real Docker Compose services instead of Testcontainers.

### 4. Resetting the Environment
If data becomes inconsistent, use the reset task:
```bash
./gradlew resetSystem
```

## Debugging Failures

### HTML Reports
Test reports are generated at:
- `gateway/build/reports/tests/e2eTest/index.html`
- `services/[service-name]/build/reports/tests/test/index.html`

### Common Issues
- **Connection Refused**: Verify the service is running and the port mapping (e.g., 18082 for ledger) is correct.
- **DNS Resolution (Mac)**: Use `127.0.0.1` instead of `localhost` in `WebTestClient` configuration if resolution fails.
- **JWT Errors**: Ensure the `JWT_SECRET` in `application.yaml` matches the one expected by the gateway.

## Adding New BDD Scenarios
1. Define the scenario in `*-bdd.md`.
2. Implement the test method in `SelfContainedOrchestratorE2ETest.java`.
3. Annotate with `@Test` and use `BDD-` prefix for display name consistency.