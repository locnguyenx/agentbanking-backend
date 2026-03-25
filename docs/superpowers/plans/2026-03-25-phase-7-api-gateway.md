# Phase 7: API Gateway Hardening Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development.

**Goal:** Hardening Spring Cloud Gateway — JWT validation, rate limiting, circuit breakers, OpenAPI finalization.

**Tech Stack:** Spring Cloud Gateway, Spring Security, Resilience4j

---

## Tasks

### Task 1: Gateway Setup
- [ ] Configure Spring Cloud Gateway with routes to all backend services
- [ ] Set up service discovery (or static config)
- [ ] Commit

### Task 2: JWT Validation
**FR Requirements:** FR-12.2

- [ ] Implement JWT filter with token validation
- [ ] Extract agentId from token claims
- [ ] Implement token refresh logic
- [ ] Commit

### Task 3: Rate Limiting
**NFR Requirements:** NFR-2.2

- [ ] Add rate limiter (Redis-based)
- [ ] Configure per-agent-tier limits
- [ ] Commit

### Task 4: Circuit Breakers
**NFR Requirements:** NFR-2.2

- [ ] Add Resilience4j circuit breaker to all downstream routes
- [ ] Configure fallback responses
- [ ] Commit

### Task 5: OpenAPI Finalization
**FR Requirements:** FR-12.3

- [ ] Validate all routes against docs/api/openapi.yaml
- [ ] Add OpenAPI generated docs endpoint
- [ ] Commit