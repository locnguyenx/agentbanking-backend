---
name: end-to-end-testing
description: Use when testing all external APIs through the API gateway with real JWT tokens, covering all BDD scenarios with different user roles
---

# End-to-End API Gateway Testing

## Overview

Tests all 179 BDD scenarios through the API gateway using real JWT tokens from the auth-iam-service. Covers all agent banking platform services.

**Core principle:** Test the real system end-to-end, not mocks. Get real tokens from the auth system for each role.

## Quick Reference

```bash
# Full test suite (179 scenarios)
./scripts/e2e-tests/run-all-e2e-tests.sh

# Skip Docker if already running
./scripts/e2e-tests/run-all-e2e-tests.sh --skip-docker

# Run specific BDD section
./scripts/e2e-tests/01-rules-fee-engine.sh

# Seed test data only
./scripts/e2e-tests/seed-test-data.sh
```

## Test Structure

```
scripts/e2e-tests/
├── common.sh                  # Shared helpers (assert_*, api_call, etc.)
├── run-all-e2e-tests.sh       # Master orchestrator
├── seed-test-data.sh          # Creates users, agents, gets tokens
├── 01-rules-fee-engine.sh     # BDD-R01 to R04 (16 tests)
├── 02-ledger-float.sh         # BDD-L01 to L04 (13 tests)
├── 03-cash-withdrawal.sh      # BDD-W01 to W02 (11 tests)
├── 04-cash-deposit.sh         # BDD-D01 to D02 (9 tests)
├── 05-ekyc-onboarding.sh      # BDD-O01 to O05 (14 tests)
├── 06-bill-payments.sh        # BDD-B01 to B04 (8 tests)
├── 07-prepaid-topup.sh        # BDD-T01 to T02 (6 tests)
├── 08-duitnow-jompay.sh       # BDD-DNOW-01 to DNOW-03 (9 tests)
├── 09-ewallet-essp.sh         # BDD-WAL/ESSP (8 tests)
├── 10-agent-onboarding.sh     # BDD-A01 to A02 (7 tests)
├── 11-reversals-disputes.sh   # BDD-V01 to V03 (9 tests)
├── 12-merchant-services.sh    # BDD-M01 to M03 (11 tests)
├── 13-aml-fraud-velocity.sh   # BDD-EFM01 to EFM04 (7 tests)
├── 14-eod-settlement.sh       # BDD-SM01 to SM04 (9 tests)
├── 15-discrepancy-resolution.sh # BDD-DR01 to DR03 (9 tests)
├── 16-api-gateway.sh          # BDD-G01 to G02 (7 tests)
├── 17-backoffice.sh           # BDD-BO01 to BO06 (11 tests)
└── 18-stp-processing.sh       # BDD-S01 to S03 (6 tests)
```

## BDD Coverage Matrix

| Section | BDD IDs | Tests | Description |
|---------|---------|-------|-------------|
| 1. Rules & Fee Engine | R01-R04 | 16 | Fees, limits, velocity |
| 2. Ledger & Float | L01-L04 | 13 | Balance, settlement |
| 3. Cash Withdrawal | W01-W02 | 11 | ATM, MyKad, geofence |
| 4. Cash Deposit | D01-D02 | 9 | Cash, card deposits |
| 5. e-KYC & Onboarding | O01-O05 | 14 | MyKad, biometric |
| 6. Bill Payments | B01-B04 | 8 | JomPAY, ASTRO, TM, EPF |
| 7. Prepaid Top-up | T01-T02 | 6 | CELCOM, M1 |
| 8. DuitNow & JomPAY | DNOW | 9 | Transfers, ON/OFF-US |
| 9. e-Wallet & eSSP | WAL/ESSP | 8 | Sarawak Pay, BSN |
| 10. Agent Onboarding | A01-A02 | 7 | Micro, Standard |
| 11. Reversals & Disputes | V01-V03 | 9 | Auto reversal, disputes |
| 12. Merchant Services | M01-M03 | 11 | Retail, PIN, cash-back |
| 13. AML/Fraud | EFM | 7 | Smurfing, geofence |
| 14. EOD Settlement | SM | 9 | Settlement, CBS |
| 15. Discrepancy | DR | 9 | Maker-Checker |
| 16. API Gateway | G | 7 | Auth, routing, rate limit |
| 17. Backoffice | BO | 11 | Agent mgmt, dashboard |
| 18. STP Processing | S | 6 | Auto-process, fallback |
| **Total** | | **179** | |

## Test Roles

| Role | Username | Token Variable |
|------|----------|----------------|
| IT_ADMIN | admin | `$ADMIN_TOKEN` |
| AGENT | agent001 | `$AGENT_TOKEN` |
| BANK_OPERATOR | operator001 | `$OPERATOR_TOKEN` |
| AUDITOR | auditor001 | `$AUDITOR_TOKEN` |
| TELLER | teller001 | `$TELLER_TOKEN` |
| MAKER | maker001 | `$MAKER_TOKEN` |
| CHECKER | checker001 | `$CHECKER_TOKEN` |
| COMPLIANCE | compliance001 | `$COMPLIANCE_TOKEN` |
| SUPERVISOR | supervisor001 | `$SUPERVISOR_TOKEN` |

## Common.sh Functions

```bash
# Assertions
assert_status "name" "200" "$status"
assert_contains "name" "$body" "pattern"
assert_json_field "name" "$body" ".field" "value"
assert_json_field_exists "name" "$body" ".field"
assert_json_field_number "name" "$body" ".field"

# HTTP
response=$(api_call "POST" "/endpoint" "$TOKEN" '{"json": "body"}')
status=$(get_status "$response")
body=$(get_body "$response")

# Utilities
idempotency_key=$(generate_uuid)
```

## Adding Tests

1. Find the appropriate section script (e.g., `03-cash-withdrawal.sh`)
2. Add test following existing pattern
3. Use `assert_*` functions for verification
4. Run section to verify: `./scripts/e2e-tests/03-cash-withdrawal.sh`
5. Run full suite to verify integration: `./scripts/e2e-tests/run-all-e2e-tests.sh`

## Common Issues

| Issue | Solution |
|-------|----------|
| 401 on all endpoints | Run `seed-test-data.sh` first |
| Service not ready | Check `docker compose logs <service>` |
| Token expired | Re-run `seed-test-data.sh` |
| jq not found | `apt-get install jq` or `brew install jq` |
| uuidgen not found | Install `uuid-runtime` or use fallback |
