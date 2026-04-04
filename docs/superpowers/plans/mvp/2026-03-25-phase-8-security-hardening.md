# Phase 8: Security Hardening Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development.

**Goal:** Security hardening — EFM, geofencing, encryption audit, compliance checks.

---

## Tasks

### Task 1: EFM (Event Forward Monitoring)
- [ ] Set up Kafka producers for transaction events
- [ ] Create EFM dashboard integration points
- [ ] Commit

### Task 2: Geofencing Enforcement
**NFR Requirements:** NFR-4.2

- [ ] Implement 100m radius GPS check
- [ ] Handle GPS unavailable scenarios (ERR_GPS_UNAVAILABLE)
- [ ] Commit

### Task 3: Encryption Audit
**NFR Requirements:** NFR-3.1 to NFR-3.5

- [ ] Audit all logs for PII leakage
- [ ] Verify PAN masking
- [ ] Verify MyKad encryption at rest
- [ ] Commit

### Task 4: Compliance Reporting
- [ ] Generate compliance reports
- [ ] Add audit trail exports
- [ ] Commit