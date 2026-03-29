# Docker Startup Fixes - 2026-03-28

## Problem

Services failed to start after rebuilding Docker containers, exposing pre-existing bugs in code that was added but not properly wired.

## Root Causes

### 1. Missing Bean Wiring (rules-service, ledger-service, biller-service)
- `StpDecisionService` added to code but not in `DomainServiceConfig`
- `ReconciliationService` added to code but not in `DomainServiceConfig`
- `BillerService` domain class had no `@Service` and no config bean

### 2. Missing Annotations on Adapters (ledger-service)
- `DiscrepancyCaseRepositoryAdapter` missing `@Repository` annotation
- Spring couldn't discover the adapter implementing the port interface

### 3. Configuration Mismatch (ledger-service)
- Feign clients referenced `${switch-adapter.url}` and `${switch-adapter-service.url}`
- `application.yaml` only had `rules-service.url`
- Docker environment variables didn't match property names

### 4. Cross-Service Dependency Conflict (ledger-service)
- `ledger-service` depended on `onboarding-service` for `AgentRecord`
- Both services packaged in same jar, causing Flyway migration version conflict (V1 duplicates)
- Fixed by: moving ledger migrations to `db/migration/ledger` subdirectory

### 5. Missing Component Scan (ledger-service)
- `EfmEventPublisher` in `common` package wasn't scanned
- `@SpringBootApplication` only scans `com.agentbanking.ledger`
- Fixed by adding `@ComponentScan(basePackages = {"com.agentbanking.ledger", "com.agentbanking.common"})`

## Why It Worked Before

Old Docker images were built from an earlier commit (`438d128`) where:
- Domain services had simpler constructors
- `DomainServiceConfig` matched the constructors
- Feign clients didn't reference undefined properties

New code in commit `342e310` added classes without proper Spring wiring.

## Fixes Applied

| Service | File | Fix |
|---------|------|-----|
| rules-service | `DomainServiceConfig.java` | Added `StpDecisionService` bean |
| ledger-service | `DomainServiceConfig.java` | Added `ReconciliationService` bean |
| ledger-service | `LedgerServiceApplication.java` | Added `@ComponentScan` for common package |
| ledger-service | `DiscrepancyCaseRepositoryAdapter.java` | Added `@Repository` annotation |
| ledger-service | `application.yaml` | Added Feign URL properties |
| ledger-service | `build.gradle` | Changed `onboarding-service` to `compileOnly` |
| ledger-service | Flyway migrations | Moved to `db/migration/ledger` subdirectory |
| biller-service | `BillerService.java` | Added `@Service` annotation |

## Lessons Learned

1. **Compilation ≠ Working**: Code compiles but Spring won't start without proper bean wiring
2. **New classes need registration**: Every `@Service`, `@Repository`, `@Component` must be discoverable
3. **Config must match code**: Feign URL properties must exist in `application.yaml`
4. **Cross-service dependencies cause conflicts**: Shared Flyway migrations break when jars merge
5. **Test Docker builds after significant changes**: Don't assume local `./gradlew build` proves Docker works
6. **Don't touch unrelated services when fixing issues**: Fixes cascaded across multiple services unnecessarily

## ArchUnit Limitations

ArchUnit CANNOT detect:
- Missing Spring bean registrations
- Incorrect application.yaml configuration
- Runtime dependency injection failures

ArchUnit CAN detect:
- Domain layer importing Spring/JPA classes
- Infrastructure adapters not implementing domain ports
- Controllers bypassing service layer

## Prevention

- Add CI step that starts all Docker containers and verifies health
- Add pre-commit hook that validates `application.yaml` against Feign client annotations
- When adding new domain service, always add bean to `DomainServiceConfig` in same commit
