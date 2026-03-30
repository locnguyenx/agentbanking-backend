# Findings during request code review

## Issue 1
- Core hexagonal architecture is violated — domain contains JPA annotations and services use EntityManager directly

**Suggestions to Enforce**
1. CI Pipeline Enforcement
Add to build.gradle or CI:
```
// Fail build if domain layer has JPA
task verifyHexagonalArchitecture(type: Test) {
    doFirst {
        // Run ArchUnit tests or fail
    }
}
```
build.dependsOn verifyHexagonalArchitecture
2. Pre-commit Hook
Add .git/hooks/pre-commit:
```
#!/bin/bash
./gradlew :services:rules-service:test --tests "*HexagonalArchitectureTest*" || exit 1
```
3. Add "Architecture Check" to AGENTS.md
Make the enforcement section more visible with "BEFORE ANY PR: Run architecture tests"
4. Scaffold Template
Create a template for new domain models that enforces correct pattern
5. IDE Plugin
Add Checkstyle or SpotBugs rules for this
---

# APT Gateway issue

Current test results:
- Before: 13 x 401 (Auth), 3 x 400 (Bad request)
- After: 9 x 500 (Server error), 6 x 400 (Bad request)
Remaining issues (not authentication):
The 9 x 500 and 6 x 400 errors are API payload mismatches between external and internal APIs:
- External API sends: {"customerCard": "4111...", "customerPin": "1234", "location": {...}}
- Internal API expects: {"agentId": "uuid", "customerCardMasked": "4111...", "geofenceLat": 3.139, "geofenceLng": 101.68, ...}
These are design issues where the gateway routes requests without transforming the payload. This would require either:
1. Adding request/response transformation filters in the gateway
2. Updating the backend services to accept the external API contract
Would you like me to continue fixing the payload mismatches, or is this progress sufficient for now?

Issue with openapi.yaml:
request & response schema don't explicit define all properties, and request schema don't have required properties. this cause issues for implementation of external consumers

# User Swagger UI local
```bash
 npx open-swagger-ui --open openapi.yaml
 ```