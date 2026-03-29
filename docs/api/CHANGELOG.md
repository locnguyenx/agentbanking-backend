# OpenAPI Changelog

## Version 1.0.0 (2026-03-29)

### Summary
Complete OpenAPI spec aligned with system design document (`2026-03-25-agent-banking-platform-design.md`).

### External API Endpoints

| Method | Path | Service | Status |
|--------|------|---------|--------|
| POST | /api/v1/withdrawal | Ledger | ✓ Implemented |
| POST | /api/v1/deposit | Ledger | ✓ Implemented |
| POST | /api/v1/balance-inquiry | Ledger | ✓ Implemented |
| GET | /api/v1/agent/balance | Ledger | ✓ Implemented |
| POST | /api/v1/kyc/verify | Onboarding | ✓ Implemented |
| POST | /api/v1/kyc/biometric | Onboarding | ✓ Implemented |
| POST | /api/v1/bill/pay | Biller | ✓ Implemented |
| POST | /api/v1/topup | Biller | ✓ Implemented |
| POST | /api/v1/transfer/duitnow | Switch | ✓ Implemented |
| POST | /api/v1/retail/sale | Ledger | ✓ Implemented |
| POST | /api/v1/retail/pin-purchase | Ledger | ✓ Implemented |
| POST | /api/v1/retail/cashback | Ledger | ✓ Implemented |
| POST | /api/v1/ewallet/withdraw | Biller | ✓ Implemented |
| POST | /api/v1/ewallet/topup | Biller | ✓ Implemented |
| POST | /api/v1/essp/purchase | Biller | ✓ Implemented |

### Backoffice Endpoints

| Method | Path | Service | Status |
|--------|------|---------|--------|
| GET | /api/v1/backoffice/dashboard | Ledger | ✓ Implemented |
| GET | /api/v1/backoffice/agents | Onboarding | ✓ Implemented |
| POST | /api/v1/backoffice/agents | Onboarding | ✓ Implemented |
| GET | /api/v1/backoffice/agents/{id} | Onboarding | ✓ Implemented |
| PUT | /api/v1/backoffice/agents/{id} | Onboarding | ✓ Implemented |
| DELETE | /api/v1/backoffice/agents/{id} | Onboarding | ✓ Implemented |
| GET | /api/v1/backoffice/transactions | Ledger | ✓ Implemented |
| GET | /api/v1/backoffice/settlement | Ledger | ✓ Implemented |
| GET | /api/v1/backoffice/kyc/review-queue | Onboarding | ✓ Implemented |
| POST | /api/v1/backoffice/discrepancy/{caseId}/maker-action | Ledger | ✓ Implemented |
| POST | /api/v1/backoffice/discrepancy/{caseId}/checker-approve | Ledger | ✓ Implemented |
| POST | /api/v1/backoffice/discrepancy/{caseId}/checker-reject | Ledger | ✓ Implemented |
| GET | /api/v1/backoffice/audit-logs | Onboarding | ✓ Implemented |

### New Features Added

1. **Gateway Aggregator**: Auto-generates OpenAPI spec from running services
2. **Spec Generation Script**: `scripts/generate-openapi-specs.sh`
3. **Swagger UI**: Available at `http://localhost:8080/swagger-ui.html`

### Implementation Details

- **Ledger Service**: Added springdoc dependency, PIN purchase endpoint
- **Onboarding Service**: Added springdoc dependency, audit-logs controller, AgentOnboardingService bean
- **Biller Service**: Added springdoc dependency
- **Gateway**: Added all design routes, aggregator path mappings

### Breaking Changes

None - this is the initial version aligned with system design.

### Migration Notes

For channel apps using the old spec:
- Path structure unchanged (all under `/api/v1/`)
- New endpoints added (pin-purchase, audit-logs)
- Request/response schemas enhanced with proper validation

### How to Generate New Spec

```bash
# Start all services
docker-compose --profile infra --profile backend --profile gateway up -d

# Generate spec
./scripts/generate-openapi-specs.sh
```

The generated spec is at `docs/api/openapi.yaml`.
