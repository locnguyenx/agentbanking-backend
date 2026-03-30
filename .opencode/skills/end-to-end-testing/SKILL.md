---
name: end-to-end-testing
description: Use when testing all external APIs through the API gateway with real JWT tokens, covering public and authenticated endpoints with different user roles
---

# End-to-End API Gateway Testing

## Overview

Tests all external APIs through the API gateway using real JWT tokens from the auth-iam-service. Covers public endpoints, authenticated endpoints with role-based access, and all BDD scenarios.

**Core principle:** Test the real system end-to-end, not mocks. Get real tokens from the auth system for each role.

## Quick Reference

| Command | Purpose |
|---------|---------|
| `./scripts/e2e-tests/run-all-e2e-tests.sh` | Run full E2E test suite |
| `./scripts/e2e-tests/seed-test-data.sh` | Create test users and get tokens |
| `./scripts/e2e-tests/run-e2e-tests.sh` | Basic gateway tests |
| `./scripts/e2e-tests/bdd-e2e-tests.sh` | BDD scenario tests |

## Test Structure

```
scripts/e2e-tests/
├── run-all-e2e-tests.sh    # Master orchestrator
├── seed-test-data.sh       # Creates users, roles, permissions, gets tokens
├── run-e2e-tests.sh        # Basic API gateway tests
└── bdd-e2e-tests.sh        # BDD scenario coverage
```

## Token Acquisition Flow

```bash
# 1. Seed creates admin via bootstrap endpoint
POST /auth/users/bootstrap → admin token

# 2. Admin creates other users
POST /auth/users (with admin token) → agent001, operator001, etc.

# 3. Each user gets their own token
POST /auth/token → role-specific token

# 4. Tests use role-specific tokens
GET /auth/users (with admin token) ✓
GET /auth/users (with agent token) ✗ (403 Forbidden)
```

## Test Roles

| Role | Username | Tests |
|------|----------|-------|
| IT_ADMIN | admin | User/role/permission CRUD |
| AGENT | agent001 | Transaction APIs |
| BANK_OPERATOR | operator001 | Backoffice operations |
| AUDITOR | auditor001 | Read-only audit logs |
| TELLER | teller001 | Branch transactions |

## BDD Coverage Matrix

| BDD Section | Scenarios | Test File |
|-------------|-----------|-----------|
| 1. User Management | Create, duplicate, lock, reset | bdd-e2e-tests.sh |
| 2. Authentication | Login, invalid creds, refresh | bdd-e2e-tests.sh |
| 3. Authorization | Roles, permissions, access control | bdd-e2e-tests.sh |
| 6. Integration | Health check, API docs | bdd-e2e-tests.sh |

## Running Tests

### Full Suite (Docker + Seed + Tests)
```bash
./scripts/e2e-tests/run-all-e2e-tests.sh
```

### Skip Docker (if already running)
```bash
./scripts/e2e-tests/run-all-e2e-tests.sh --skip-docker
```

### Manual Steps
```bash
# 1. Start services
docker compose --profile all up -d

# 2. Seed data
./scripts/e2e-tests/seed-test-data.sh

# 3. Run tests
./scripts/e2e-tests/bdd-e2e-tests.sh
```

## Adding New Tests

1. Add to `bdd-e2e-tests.sh` for BDD scenarios
2. Add to `run-e2e-tests.sh` for gateway routing
3. Update `seed-test-data.sh` if new roles/users needed

## Common Issues

| Issue | Solution |
|-------|----------|
| 401 on all endpoints | Run seed-test-data.sh first |
| Service not ready | Wait longer or check `docker compose logs` |
| Token expired | Re-run seed-test-data.sh |
