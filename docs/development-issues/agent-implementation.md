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