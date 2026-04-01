# Testing Guidelines

## Test stack

* Unit tests: JUnit 5 + Mockito
* Architecture tests: ArchUnit (enforce hexagonal rules)
* Integration tests: Spring Boot Test + Testcontainers (PostgreSQL)
* BDD scenarios in `*-bdd.md` are the acceptance criteria

## Integration test

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

## Troubleshooting

See @docs/lessons-learned/*.md for lessons learned
