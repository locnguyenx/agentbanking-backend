# E2E Test Failure Report

**Generated:** 2026-03-30

## Summary

| Status Code | Count | Description |
|-------------|-------|-------------|
| 500 | 9 | Server error (payload mismatch) |
| 400 | 6 | Bad request (API contract mismatch) |

**Total Failed Tests:** 15

---

## Progress Update

**JWT Authentication Issue - RESOLVED**

The JWT authentication issue has been fixed. The following changes were made:

1. **Gateway OAuth2 Conflict Fixed** - The `spring-boot-starter-oauth2-resource-server` auto-configuration was conflicting with our custom JwtAuthFilter. Fixed by setting `jwt.secret: ""` in gateway application.yaml.

2. **Ledger-service SecurityConfig Added** - Added SecurityConfig to permit all internal requests (ledger-service was getting Spring Security transitively from onboarding-service dependency).

3. **Onboarding-service SecurityConfig Added** - Added SecurityConfig to permit internal and backoffice endpoints.

---

## Failed Tests by Status Code

### 500 - Server Error (9 tests)

These tests fail because the external API payload format doesn't match what the internal API expects. The gateway routes requests without transforming the payload.

| Test Name | Method | Endpoint | Expected | Issue |
|-----------|--------|---------|----------|-------|
| Successful withdrawal | POST | /api/v1/withdrawal | 200 | Payload mismatch |
| Successful deposit | POST | /api/v1/deposit | 200 | Payload mismatch |
| Timeout for reversal | POST | /api/v1/withdrawal | 504 | Payload mismatch |
| Retail sale | POST | /api/v1/retail/sale | 200 | Payload mismatch |
| Retail sale for settlement | POST | /api/v1/retail/sale | 200 | Payload mismatch |
| Smurfing detection | POST | /api/v1/deposit | 403 | Payload mismatch |
| Get agent balance | GET | /api/v1/ledger/balance/ | 200 | Payload mismatch |
| Route to balance service | GET | /api/v1/balance-inquiry | 200 | Payload mismatch |
| STP withdrawal | POST | /api/v1/withdrawal | 200 | Payload mismatch |

**Root Cause:** The external API contract differs from internal API:
- External sends: `{"customerCard": "4111...", "customerPin": "1234", "location": {...}}`
- Internal expects: `{"agentId": "uuid", "customerCardMasked": "4111...", "geofenceLat": 3.139, ...}`

### 400 - Bad Request (6 tests)

These endpoints exist but return 400 errors due to request payload issues.

| Test Name | Method | Endpoint | Expected | Issue |
|-----------|--------|---------|----------|-------|
| Fee config for Micro agent | POST | /api/v1/rules/fees | 201 | Enum value mismatch |
| JomPAY payment | POST | /api/v1/billpayment/jompay | 200 | Payload mismatch |
| Sarawak Pay withdrawal | POST | /api/v1/ewallet/withdraw | 200 | Payload mismatch |
| Ghost investigation | POST | /api/v1/backoffice/discrepancy/DCASE_001/maker-action | 200 | Payload mismatch |
| Create agent | POST | /api/v1/backoffice/agents | 201 | Payload mismatch |
| Micro-Agent onboarding | POST | /api/v1/onboarding/submit-application | 200 | Payload mismatch |

---

## Previously Fixed Issues

### 401 - Authentication Required (RESOLVED - Was 13 tests)

The JWT validation was fixed. Tests that previously returned 401 now return 500 (payload issues) or pass.

### 404 - Not Implemented (RESOLVED - Was 5 tests)

The following endpoints were implemented:

| Service | Endpoint | Method | Status |
|---------|----------|--------|--------|
| rules-service | /api/v1/rules/fees | POST | Implemented |
| ledger-service | /api/v1/ledger/balance/{agentId} | GET | Implemented |
| onboarding-service | /api/v1/onboarding/verify-mykad | POST | Implemented |
| biller-service | /api/v1/billpayment/jompay | POST | Implemented |
| onboarding-service | /api/v1/onboarding/submit-application | POST | Implemented |

---

## Required Actions

### 1. Fix Payload Transformation

To fix the 500 errors, the gateway needs to transform external API payloads to internal API format. Two options:

**Option A: Add Gateway Filters**
Add request transformation filters in the gateway to convert:
- `customerCard` → `customerCardMasked`
- `customerPin` → Remove (not needed internally)
- `location.latitude` → `geofenceLat`
- `location.longitude` → `geofenceLng`
- Add `agentId` from JWT `agent_id` claim

**Option B: Update Internal APIs**
Modify backend services to accept the external API contract.

### 2. Fix Remaining 400 Errors

- **Fee config**: Use correct enum values (`FIXED` not `FLAT`, `MICRO` not `Micro`)
- **Other endpoints**: Need payload transformation or DTO updates

---

## Test Execution

```bash
# Run all E2E tests
bash scripts/e2e-tests/run-all-e2e-tests.sh --skip-docker

# Regenerate tokens first
bash scripts/e2e-tests/seed-test-data.sh

# View failed tests
cat /tmp/e2e_failed_tests.txt
```
