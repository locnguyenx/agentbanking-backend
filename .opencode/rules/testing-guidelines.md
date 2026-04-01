# Testing Guidelines

Project Rules for Testing & Testcontainers Configuration

## Test stack

* Unit tests: JUnit 5 + Mockito
* Architecture tests: ArchUnit (enforce hexagonal rules)
* Integration tests: Spring Boot Test + Testcontainers (PostgreSQL)
* BDD scenarios in `*-bdd.md` are the acceptance criteria

## Testing Strategy
- **Integration Tests:** Use Testcontainers to spin up real dependencies (e.g., PostgreSQL, Redis, Kafka).
- **Testcontainers Usage:** NEVER use manual Docker commands to start dependent services (like Redis or Postgres). Always use Testcontainers to manage the container lifecycle (start/stop).
- **Reuse:** Enable Testcontainers reuse (`TC_REUSE=true` or similar) to improve performance.

**EXCEPTION:** may have issue in using Testcontainers on Windows, in this case we can use docker containers for required infra, but need to do test data cleanup

## Environment & Docker Setup
- **Docker Socket:** Ensure the Docker socket (`/var/run/docker.sock`) is accessible to Testcontainers.
- **MacOS Fix:** If running locally on Mac, ensure `/var/run/docker.sock` maps correctly to `$HOME/.docker/run/docker.sock`.
- **CI/CD:** <to be done>

## Commands
- **Run all tests:** <to be done>
- **Run integration tests only:** <to be done>

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

## Known Issues & Troubleshooting
- If tests fail with "cannot connect to Docker daemon", verify that Testcontainers has access to the local Docker engine.

See @docs/lessons-learned/*.md for lessons learned
