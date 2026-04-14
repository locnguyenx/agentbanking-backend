# Error Code Mapping: BDD Spec vs Implementation

**Date:** 2026-04-14

## Overview

The BDD specifications use simplified error codes, but the actual implementation uses a categorized naming scheme with prefixes:
- `ERR_VAL_xxx` - Validation errors
- `ERR_BIZ_xxx` - Business logic errors
- `ERR_EXT_xxx` - External system errors
- `ERR_AUTH_xxx` - Authentication errors
- `ERR_SYS_xxx` - System errors

## Mapping Table

| BDD Spec Error Code | Implementation Error Code | ErrorCodes Constant | Status |
|---------------------|---------------------------|---------------------|--------|
| `ERR_VELOCITY_COUNT_EXCEEDED` | `ERR_BIZ_VELOCITY_COUNT_EXCEEDED` | `ErrorCodes.ERR_VELOCITY_COUNT_EXCEEDED` | ✅ Aligned |
| `ERR_VELOCITY_AMOUNT_EXCEEDED` | `ERR_BIZ_VELOCITY_AMOUNT_EXCEEDED` | `ErrorCodes.ERR_VELOCITY_AMOUNT_EXCEEDED` | ✅ Aligned |
| `ERR_INSUFFICIENT_FLOAT` | `ERR_BIZ_INSUFFICIENT_FLOAT` | `ErrorCodes.ERR_INSUFFICIENT_FLOAT` | ✅ Aligned |
| `ERR_INSUFFICIENT_FUNDS` | `ERR_EXT_SWITCH_DECLINED` | `ErrorCodes.ERR_SWITCH_DECLINED` | ⚠️ Different |
| `ERR_NETWORK_TIMEOUT` | Not yet defined | TBD | ❌ Missing |
| `ERR_FEE_CONFIG_NOT_FOUND` | `ERR_BIZ_FEE_CONFIG_NOT_FOUND` | `ErrorCodes.ERR_FEE_CONFIG_NOT_FOUND` | ✅ Aligned |
| `ERR_FEE_CONFIG_EXPIRED` | `ERR_BIZ_FEE_CONFIG_EXPIRED` | `ErrorCodes.ERR_FEE_CONFIG_EXPIRED` | ✅ Aligned |
| `ERR_UNSUPPORTED_TRANSACTION_TYPE` | Not yet defined | TBD | ❌ Missing |
| `ERR_INVALID_ACCOUNT` | Not yet defined | TBD | ❌ Missing |
| `ERR_INVALID_BILLER_REF` | `ERR_VAL_INVALID_BILLER_REF` | `ErrorCodes.ERR_INVALID_BILLER_REF` | ✅ Aligned |
| `ERR_BILLER_TIMEOUT` | `ERR_EXT_BILLER_UNAVAILABLE` | `ErrorCodes.ERR_BILLER_UNAVAILABLE` | ⚠️ Different |
| `ERR_PROXY_NOT_FOUND` | `ERR_BIZ_PROXY_NOT_FOUND` | `ErrorCodes.ERR_BIZ_PROXY_NOT_FOUND` | ✅ Aligned |
| `ERR_INVALID_MYKAD_FORMAT` | `ERR_VAL_INVALID_MYKAD_FORMAT` | `ErrorCodes.ERR_INVALID_MYKAD_FORMAT` | ✅ Aligned |
| `ERR_INVALID_AMOUNT` | `ERR_VAL_INVALID_AMOUNT` | `ErrorCodes.ERR_INVALID_AMOUNT` | ✅ Aligned |
| `ERR_FLOAT_CAP_EXCEEDED` | `ERR_BIZ_FLOAT_CAP_EXCEEDED` | `ErrorCodes.ERR_FLOAT_CAP_EXCEEDED` | ✅ Aligned |
| `ERR_LIMIT_EXCEEDED` | `ERR_BIZ_LIMIT_EXCEEDED` | `ErrorCodes.ERR_LIMIT_EXCEEDED` | ✅ Aligned |
| `ERR_COUNT_LIMIT_EXCEEDED` | `ERR_BIZ_COUNT_LIMIT_EXCEEDED` | `ErrorCodes.ERR_COUNT_LIMIT_EXCEEDED` | ✅ Aligned |
| `ERR_GEOFENCE_VIOLATION` | `ERR_BIZ_GEOFENCE_VIOLATION` | `ErrorCodes.ERR_GEOFENCE_VIOLATION` | ✅ Aligned |
| `ERR_GPS_UNAVAILABLE` | `ERR_BIZ_GPS_UNAVAILABLE` | `ErrorCodes.ERR_GPS_UNAVAILABLE` | ✅ Aligned |
| `ERR_AGENT_DEACTIVATED` | `ERR_BIZ_AGENT_DEACTIVATED` | `ErrorCodes.ERR_AGENT_DEACTIVATED` | ✅ Aligned |
| `ERR_INVALID_PIN` | `ERR_AUTH_INVALID_PIN` | `ErrorCodes.ERR_INVALID_PIN` | ✅ Aligned |
| `ERR_INVALID_CARD` | `ERR_AUTH_INVALID_CARD` | `ErrorCodes.ERR_INVALID_CARD` | ✅ Aligned |
| `ERR_AGENT_FLOAT_NOT_FOUND` | `ERR_SYS_AGENT_FLOAT_NOT_FOUND` | `ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND` | ✅ Aligned |
| `ERR_BIOMETRIC_MISMATCH` | Not yet defined | TBD | ❌ Missing |
| `ERR_VELOCITY_EXCEEDED` | Not used | Not defined | ❌ Not in use |

## Missing Error Codes (Required by BDD but not in ErrorCodes.java)

These error codes are referenced in BDD specs but not yet defined in the ErrorCodes registry:

1. `ERR_NETWORK_TIMEOUT` - For switch/network timeout scenarios (BDD-WF-EC-W02, BDD-SR-02)
2. `ERR_UNSUPPORTED_TRANSACTION_TYPE` - For unsupported transaction types (BDD-TO-06)
3. `ERR_INVALID_ACCOUNT` - For invalid deposit account (BDD-WF-EC-D01)
4. `ERR_BIOMETRIC_MISMATCH` - For biometric verification failures (BDD-W02-EC-01)
5. `ERR_BILLER_TIMEOUT` - For biller system timeouts (BDD-B01-EC-02)
6. `ERR_AGGREGATOR_TIMEOUT` - For telco aggregator timeouts (BDD-T01-EC-02)
7. `ERR_WALLET_INSUFFICIENT` - For insufficient e-wallet balance (BDD-WAL-01-EC-01)
8. `ERR_ESSP_SERVICE_UNAVAILABLE` - For eSSP/BSN system unavailability (BDD-ESSP-01-EC-01)
9. `ERR_QR_PAYMENT_TIMEOUT` - For QR payment timeouts (BDD-WF-EC-RS02)
10. `ERR_RTP_DECLINED` - For Request-to-Pay declines (BDD-WF-EC-RS03)
11. `ERR_PIN_INVENTORY_DEPLETED` - For PIN inventory depletion (BDD-WF-EC-PIN01)
12. `ERR_PIN_GENERATION_FAILED` - For PIN generation failures (BDD-WF-EC-PIN02)

## Recommendations

1. Add missing error codes to `ErrorCodes.java`
2. Update BDD specs to use full implementation error codes (with `ERR_BIZ_`, `ERR_VAL_`, etc. prefixes)
3. Ensure all tests use constants from `ErrorCodes.java` instead of hardcoded strings
4. Add ArchUnit test to verify no hardcoded error codes in tests

## Test Status

The tests in `VelocityCheckServiceTest.java` are **CORRECT** - they match the implementation:
- Test uses: `"ERR_BIZ_VELOCITY_COUNT_EXCEEDED"`
- Implementation returns: `ErrorCodes.ERR_VELOCITY_COUNT_EXCEEDED` = `"ERR_BIZ_VELOCITY_COUNT_EXCEEDED"`
- BDD spec says: `ERR_VELOCITY_COUNT_EXCEEDED` (simplified)

**Conclusion:** Tests are aligned with implementation. BDD spec uses simplified names for readability.
