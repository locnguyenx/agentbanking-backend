# Phase 6: Biller Service Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development.

**Goal:** Build Biller Service — utility payments, JomPAY, telco top-ups, biller webhooks.

**Tech Stack:** Java 21, Spring Boot 3.x, PostgreSQL

---

## File Structure

```
services/biller-service/
├── build.gradle
├── src/main/java/com/agentbanking/biller/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── BillerConfig.java
│   │   │   ├── BillPayment.java
│   │   │   └── BillerType.java
│   │   └── service/BillerValidationService.java
│   └── infrastructure/
│       └── web/BillerController.java
└── src/test/
```

---

## Tasks

### Task 1: Project Setup
- [ ] Scaffold Spring Boot project
- [ ] Commit

### Task 2: Biller Validation
**BDD Scenarios:** BDD-B01 (JomPAY), BDD-B01-EC-01 (invalid ref), BDD-T01 (top-up)

**BRD Requirements:** FR-7.5 (Ref-1 validation), FR-8.3 (phone validation)

- [ ] Write validation service
- [ ] Write tests
- [ ] Commit

### Task 3: Bill Payment Use Cases
**BRD Requirements:** FR-7.1 to FR-7.4, FR-8.1 to FR-8.2

- [ ] Implement JomPAY, ASTRO, TM, EPF payment flows
- [ ] Implement CELCOM, M1 top-up flows
- [ ] Write tests
- [ ] Commit

### Task 4: Webhook Handler
**BRD Requirements:** Biller webhooks

- [ ] Create webhook endpoints for async callbacks
- [ ] Write tests
- [ ] Commit