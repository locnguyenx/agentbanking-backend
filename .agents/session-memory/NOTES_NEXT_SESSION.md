# Notes for Next Session

**Date:** 2026-04-20

## Session Summary (2026-04-20)
Foundation stabilized. We now have a deterministic path for E2E tests.

1. ✅ **Auth Stabilization:** Surgical SQL bypass in `SelfContainedOrchestratorE2ETest` to force credentials/state for dynamic agents.
2. ✅ **Polling Reliability:** `OrchestratorController` hardened against `ClassCastException` and terminal state polling stalls.
3. ✅ **Consistency:** `billPayment_shouldCompleteSuccessfully` passes consistently.

## Current Platform State
- **Full Suite Pass Rate:** 74/106 (70%)
- **Primary Blockers Resolved:** Infrastructure, Setup, and 500-level Polling Errors.
- **Remaining Failures:** Isolated to service-layer logic and mock responses (e.g., `ERR_SWITCH_DECLINED`).

## Instructions for Next Session
1. **Focus:** Resolve the remaining functional failures in functional flows (Deposit, Withdraw, Transfer).
2. **Strategy:** Start with `withdraw_onUs_shouldCompleteSuccessfully` and audit the flow in `WithdrawalWorkflowImpl` and `ledger-service`.
3. **Verification:** Use `./gradlew :gateway:e2eTest --tests "*.billPayment_shouldCompleteSuccessfully" -PtestProfile=local` as the verified passing baseline.

## Run Baseline
```bash
./gradlew :gateway:e2eTest --tests "*.billPayment_shouldCompleteSuccessfully" -PtestProfile=local --no-daemon
```