# Walkthrough: Backend Regression Debugging Data

This document provides the exact payloads and responses for the recent backend regressions to assist the backend team in debugging.

## 1. Regression: 404 Not Found (Quote)

**Request:**
- **URL:** `POST http://localhost:8080/api/v1/transactions/quote`
- **Headers:**
  - `Content-Type: application/json`
  - `Authorization: Bearer <VALID_JWT>`
- **Body:**
```json
{
  "serviceCode": "CASH_WITHDRAWAL",
  "amount": 100,
  "agentId": "78cbde90-232a-48a1-878e-0bed6ff52301",
  "fundingSource": "CARD_EMV"
}
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-04-07T04:19:37.350+00:00",
  "status": 404,
  "error": "Not Found",
  "path": "/internal/transactions/quote"
}
```
*Note: The `path` field reveals that the Gateway is attempting to route to an internal endpoint that does not exist or has been renamed.*

---

## 2. Regression: 500 Internal Server Error (Start Transaction)

**Request:**
- **URL:** `POST http://localhost:8080/api/v1/transactions`
- **Headers:**
  - `Content-Type: application/json`
  - `Authorization: Bearer <VALID_JWT>`
- **Body:**
```json
{
  "transactionType": "CASH_WITHDRAWAL",
  "agentId": "78cbde90-232a-48a1-878e-0bed6ff52301",
  "amount": 100.00
}
```

**Response (500 Internal Server Error):**
```json
{
  "status": "FAILED",
  "error": {
    "code": "ERR_SYS_INTERNAL",
    "message": "An unexpected error occurred",
    "action_code": "RETRY",
    "trace_id": null,
    "timestamp": "2026-04-07T04:21:06.810943580Z"
  }
}
```

---

## Technical Audit
- **Identity**: The `agentId` is correctly sent as the backend-issued UUID.
- **Authentication**: The JWT is valid and correctly extracted from the [AuthRepository](file:///Users/me/myprojects/agentbanking-channel/lib/features/auth/repositories/auth_repository.dart#6-99) login.
- **Payload Format**: The `amount` is sent as a numeric value for `/transactions` (as per changelog) and as a string/integer for `/quote` (as per openapi).
