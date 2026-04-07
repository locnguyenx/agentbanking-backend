# BDD Specification: Channel API Alignment

**Version:** 1.0
**Date:** 2026-04-04
**Status:** Draft
**Module:** rules-service, biller-service, onboarding-service
**BRD Reference:** `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-brd.md`

Each BDD scenario is tagged with exactly one `@US` (User Story) and one or more `@FR` (Functional Requirements) for atomic traceability.

---

## 1. Transaction Quote (US-001, FR-001)

```gherkin
Feature: Transaction Quote

  @US-001 @FR-001.1 @FR-001.3 @FR-001.4
  Scenario: Get quote for cash withdrawal - Happy Path
    Given an authenticated agent with ACTIVE status
    When the agent requests a quote for serviceCode "CASH_WITHDRAWAL" with amount "100.00" and fundingSource "CARD_EMV"
    Then the response status is 200
    And the response contains quoteId, amount "100.00", fee, total, and commission
    And all monetary fields are returned as strings

  @US-001 @FR-001.3
  Scenario: Get quote with missing required field - Edge Case
    Given an authenticated agent
    When the agent requests a quote without providing the amount field
    Then the response status is 400
    And the error code is a validation error

  @US-001 @FR-001.3
  Scenario: Get quote with invalid funding source - Edge Case
    Given an authenticated agent
    When the agent requests a quote with fundingSource "INVALID_TYPE"
    Then the response status is 400
    And the error code indicates invalid enum value

  @US-001 @FR-001.6
  Scenario: Get quote without authentication - Edge Case
    Given no authentication token is provided
    When a quote request is sent to POST /api/v1/transactions/quote
    Then the response status is 401
```

---

## 2. DuitNow Proxy Enquiry (US-002, FR-002)

```gherkin
Feature: DuitNow Proxy Enquiry

  @US-002 @FR-002.1 @FR-002.3 @FR-002.4
  Scenario: Resolve valid mobile proxy - Happy Path
    Given an authenticated agent
    And a valid DuitNow proxy exists for "60123456789" with type "MOBILE"
    When the agent sends GET /api/v1/transfer/proxy/enquiry with proxyId "60123456789" and proxyType "MOBILE"
    Then the response status is 200
    And the response contains the registered account holder name

  @US-002 @FR-002.3
  Scenario: Resolve proxy with missing proxyId - Edge Case
    Given an authenticated agent
    When the agent sends GET /api/v1/transfer/proxy/enquiry without proxyId parameter
    Then the response status is 400

  @US-002 @FR-002.3
  Scenario: Resolve proxy with missing proxyType - Edge Case
    Given an authenticated agent
    When the agent sends GET /api/v1/transfer/proxy/enquiry without proxyType parameter
    Then the response status is 400

  @US-002 @FR-002.7
  Scenario: Resolve non-existent proxy - Edge Case
    Given an authenticated agent
    When the agent sends GET /api/v1/transfer/proxy/enquiry for a proxyId that does not exist
    Then the response status is 404
    And the error indicates proxy not found

  @US-002 @FR-002.6
  Scenario: Proxy enquiry without authentication - Edge Case
    Given no authentication token is provided
    When a proxy enquiry request is sent
    Then the response status is 401
```

---

## 3. Compliance Status (US-003, FR-003)

```gherkin
Feature: Compliance Status Check

  @US-003 @FR-003.1 @FR-003.3
  Scenario: Check unlocked agent status - Happy Path
    Given an authenticated agent with no compliance issues
    When the agent sends GET /api/v1/compliance/status
    Then the response status is 200
    And the response contains status "UNLOCKED"

  @US-003 @FR-003.3 @FR-003.4
  Scenario: Check locked agent status - Happy Path
    Given an authenticated agent with an AML compliance hold
    When the agent sends GET /api/v1/compliance/status
    Then the response status is 200
    And the response contains status "LOCKED"
    And the response includes a reason for the lock

  @US-003 @FR-003.5
  Scenario: Check status without authentication - Edge Case
    Given no authentication token is provided
    When a compliance status check is sent
    Then the response status is 401
```

---

## 4. OpenAPI Spec Quality (US-004, FR-004, FR-005, FR-006)

```gherkin
Feature: OpenAPI Spec Quality

  @US-004 @FR-004.1
  Scenario: All monetary fields use string type - Edge Case
    Given the openapi.yaml specification at docs/api/openapi.yaml
    When all schema properties representing monetary values are inspected
    Then none use type "number"
    And all use type "string"

  @US-004 @FR-005.1 @FR-005.2
  Scenario: All channel endpoints have security definition - Edge Case
    Given the openapi.yaml specification
    When all /api/v1/* endpoints are inspected
    Then each has a security requirement referencing bearerAuth
    And components.securitySchemes defines bearerAuth as Bearer JWT

  @US-004 @FR-006.1 @FR-006.2
  Scenario: All endpoints have error response schemas - Edge Case
    Given the openapi.yaml specification
    When all endpoints are inspected
    Then each has at least a 400 response with ErrorResponse schema
    And each has a 401 response with ErrorResponse schema

  @US-004 @FR-006.3
  Scenario: No wildcard content types - Edge Case
    Given the openapi.yaml specification
    When all response content types are inspected
    Then no endpoint uses '*/*' as content type
    And all use application/json
```

---

## 5. Traceability Matrix

### User Story → BDD Scenario Coverage

| User Story | FR(s) | BDD Scenario(s) | Classification |
|------------|-------|-----------------|----------------|
| US-001 | FR-001.1, FR-001.3, FR-001.4, FR-001.6 | S1.1, S1.2, S1.3, S1.4 | Happy Path / Edge Case |
| US-002 | FR-002.1, FR-002.3, FR-002.4, FR-002.6, FR-002.7 | S2.1, S2.2, S2.3, S2.4, S2.5 | Happy Path / Edge Case |
| US-003 | FR-003.1, FR-003.3, FR-003.4, FR-003.5 | S3.1, S3.2, S3.3 | Happy Path / Edge Case |
| US-004 | FR-004.1, FR-005.1, FR-005.2, FR-006.1, FR-006.2, FR-006.3 | S4.1, S4.2, S4.3, S4.4 | Edge Case |

### Requirement Coverage Summary

| Requirement | Covered By Scenario(s) |
|-------------|----------------------|
| FR-001.1 | S1.1 |
| FR-001.3 | S1.1, S1.2, S1.3 |
| FR-001.4 | S1.1 |
| FR-001.6 | S1.4 |
| FR-002.1 | S2.1 |
| FR-002.3 | S2.1, S2.2, S2.3 |
| FR-002.4 | S2.1 |
| FR-002.6 | S2.5 |
| FR-002.7 | S2.4 |
| FR-003.1 | S3.1 |
| FR-003.3 | S3.1, S3.2 |
| FR-003.4 | S3.2 |
| FR-003.5 | S3.3 |
| FR-004.1 | S4.1 |
| FR-005.1 | S4.2 |
| FR-005.2 | S4.2 |
| FR-006.1 | S4.3 |
| FR-006.2 | S4.3 |
| FR-006.3 | S4.4 |
