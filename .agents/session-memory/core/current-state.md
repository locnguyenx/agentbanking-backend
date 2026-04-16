# Session Memory - Current State

**Project:** Agent Banking Platform
**Last Update:** 2026-04-16

## 🎯 THE NOW

### Session Status: CLOSED

### Completed Fixes (2026-04-16 - Full Day Session)

**Issue 1 - Dashboard Agent Count Inconsistency:**
- Root cause: Dashboard API was counting transaction agents instead of registered agents
- Fixed: Dashboard API now queries onboarding service for accurate agent counts (22 total, 21 active, 1 suspended)

**Issue 2 - Agents Page Stats Inconsistency:**
- Root cause: Agent statistics calculated from paginated results (only first 20 agents)
- Fixed: Agents API now returns complete statistics (total, active, suspended, inactive counts)

**Issue 3 - Frontend Caching Issues:**
- Root cause: Browser caching old JavaScript after code updates
- Fixed: Added no-cache headers to nginx, updated React Query keys, manually deployed updated JavaScript

**Issue 4 - OpenAPI Spec Enum Mismatch:**
- Root cause: OpenAPI spec used incorrect enum values [TIER_1, TIER_2, TIER_3]
- Fixed: Updated to match production enum [MICRO, STANDARD, PREMIER]
- Remaining: Test files still contain old values (73 instances) - not critical for production

### Container Builds
- ✅ backoffice: 71f1d51705b1
- ✅ orchestrator-service: 812df446d421 (--no-cache)

---

## 🚧 PENDING ISSUES (Parked for Next Session)

1. **Root cause investigation for orphan case** - Need to investigate why case was created without workflow:
   - Case ID: b9d39b5b-bae4-4828-aaf9-cb6186107d8b
   - Investigate case creation flow in SaveResolutionCaseActivityImpl

---

## 📝 Notes
- All code compiles successfully
- Build verified with tests
- Session completed at 2026-04-15 22:35