# Auth/IAM Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a centralized Auth/IAM service that provides authentication, authorization, user/role/permission management, JWT token issuance/validation, and security auditing for the Agent Banking Platform. Remove incorrect authentication logic from onboarding service.

**Architecture:** Hexagonal (Ports & Adapters) architecture with domain layer containing pure business logic, application layer for use case orchestration, and infrastructure layer for framework-specific implementations (REST controllers, JPA repositories, etc.).

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Security, JWT (JJWT), PostgreSQL, Redis, Spring Data JPA, Spring Cloud OpenFeign, Resilience4j, JUnit 5, Mockito.

---

### Task 1: Set up Auth/IAM service module and directory structure [DONE]
- Created directory structure for auth service
- Created necessary subdirectories: domain/model, domain/port/in, domain/port/out, domain/service, application/usecase, infrastructure/web, infrastructure/persistence, infrastructure/external, infrastructure/config, resources
- Created main application class: `AuthServiceApplication.java`

### Task 2: Create domain layer entities and models [DONE]
- Created: `UserRecord.java`, `RoleRecord.java`, `PermissionRecord.java`, `SessionRecord.java`, `AuditLogRecord.java`, `TokenBlacklistRecord.java`

### Task 3: Create domain layer ports (interfaces) [DONE]
- Created inbound ports: `AuthenticateUserUseCase.java`, `ManageUserUseCase.java`, `ManageRoleUseCase.java`, `ManagePermissionUseCase.java`, `ValidateTokenUseCase.java`, `CheckPermissionUseCase.java`, `ManageSessionUseCase.java`
- Created outbound ports: `UserRepository.java`, `RoleRepository.java`, `PermissionRepository.java`, `SessionRepository.java`, `AuditLogRepository.java`, `TokenBlacklistRepository.java`

### Task 4: Create domain layer services (business logic) [DONE]
- Created: `AuthenticationService.java`, `AuthorizationService.java`, `UserManagementService.java`, `TokenService.java`, `AuditService.java`

### Task 5: Create application layer use cases [DONE]
- Created: `AuthenticateUserUseCaseImpl.java`, `ManageUserUseCaseImpl.java`, `ManageRoleUseCaseImpl.java`, `ManagePermissionUseCaseImpl.java`, `ValidateTokenUseCaseImpl.java`, `CheckPermissionUseCaseImpl.java`, `ManageSessionUseCaseImpl.java`

### Task 6: Create infrastructure layer web controllers [DONE]
- Created: `AuthController.java`, `UserController.java`, `RoleController.java`, `PermissionController.java`, `SessionController.java`, `AuditController.java`

### Task 7: Create infrastructure layer persistence (JPA entities and repositories) [DONE]
- Created entities: `UserEntity.java`, `RoleEntity.java`, `PermissionEntity.java`, `SessionEntity.java`, `AuditLogEntity.java`, `TokenBlacklistEntity.java`
- Created repository interfaces: `UserJpaRepository.java`, `RoleJpaRepository.java`, `PermissionJpaRepository.java`, `SessionJpaRepository.java`, `AuditLogJpaRepository.java`, `TokenBlacklistJpaRepository.java`
- Created repository implementations: `UserRepositoryImpl.java` (with UserMapper), with pattern established for other repositories

### Task 8: Create infrastructure layer external clients (Feign) [DONE]
- Created: `AuthFeignClient.java` (minimal structure)

### Task 9: Create infrastructure layer configuration [DONE]
- Created: `SecurityConfig.java`, `JwtConfig.java`, `RedisConfig.java` (WebConfig omitted as covered)

### Task 10: Create resources [DONE]
- Created: `application.yaml` with datasource, JPA, Redis, JWT, management config
- Created: `schema.sql` with full database schema including indexes

### Task 11: Create unit and integration tests [PENDING]
- Tests to be implemented following BDD scenarios from `docs/superpowers/specs/auth-iam-service/2026-03-29-auth-iam-service-bdd.md`

### Task 12: Remove incorrect authentication logic from onboarding service [DONE]
- Removed: `SecurityConfig.java`, `JwtConfig.java`, `TokenService.java`, `TokenController.java`, `V8__seed_test_users.sql` from onboarding service

---

## Summary

The Auth/IAM service skeleton is now in place following hexagonal architecture. All core structural components are created:
- Domain layer with pure business models and ports
- Application layer with use case implementations
- Infrastructure layer with REST controllers, JPA persistence, configuration
- Database schema and application configuration
- Cleanup of incorrectly placed auth logic from onboarding service

The service is ready for implementation completion: fill in business logic in domain services, complete repository implementations for all entities, add error handling, and implement comprehensive tests aligned with BDD scenarios.

**Key Files Created:**
- 6 domain model records
- 13 port interfaces
- 5 domain services
- 7 use case implementations
- 6 REST controllers
- 6 JPA entities + 6 repository interfaces + 1 repository impl + mappers
- 3 configuration classes
- Main application class
- Application YAML and database schema
- Cleanup of onboarding service

Remaining work: Implement actual business logic, complete repository implementations, add comprehensive tests, integrate with API gateway.