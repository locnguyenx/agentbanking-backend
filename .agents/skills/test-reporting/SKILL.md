---
name: test-reporting
description: Generate test reports and traceability matrices after TDD completion
---

## Overview
After `test-driven-development`, `executing-plans`, or `subagent-driven-development` completes a feature, generate test report:

**Triggers**: After commits containing tests, when user asks "test report", "coverage", "traceability", or at end of `executing-plans`.

## Checklist
- [ ] Scan for BRD (`docs/superpowers/specs/*brd.md`)
- [ ] Scan for BDD specs (`docs/superpowers/specs/*bdd.md`) 
- [ ] Find test files (`test_*.py`, `*_test.py`)
- [ ] Parse test results from latest commits/chat
- [ ] Generate traceability matrix
- [ ] Calculate coverage metrics
- [ ] Save reports to `docs/reports/YYYY-MM-DD-feature/`
- [ ] Present summary + file links

## Process

```
Context → Find BRD/BDD → Parse Tests → Build Matrix → Generate Report → Present
  ↓
└─ User approves → Archive in docs/reports/
```

## Implementation Steps

### 1. **Context Analysis**
- find BRD, BDD in `docs/superpowers/specs/`
- find output from unit tests

### 2. **Parse Artifacts**
- BRD: Extract user stories / requirements
- BDD: Extract Gherkin scenarios
- Tests: Parse test functions matching scenario names
- Results: Parse test output from
    - Backend tests: XML files in `build/test-results/test/`
    - Frontend test: HTML report in `coverage/` directory

### 3. Generate Test Report

1. **Executive Summary**: Summary of test results by BDD scenario
2. **Traceability Matrix**: BRD → BDD → Test → Code coverage links

```
| BRD Story | BDD Scenario | Test Function | Status | Test Coverage |
|-----------|--------------|---------------|--------|----------|
| US-001 Login | Successful login | test_successful_login | PASS | 100% |
| US-001 Login | Invalid creds | test_invalid_credentials | PASS | 100% |
```

3. **Coverage Report**: 

Parse test results for requirement coverage

4. **Test Summary**
```
1. Feature: User Authentication
Tests: 8 total, 7 passed, 1 skipped
Coverage: 92% (linked to BRD stories)
Files: docs/reports/2026-03-20-auth/
```

## File Outputs
`docs/reports/YYYY-MM-DD-[feature]-test-report.md`
