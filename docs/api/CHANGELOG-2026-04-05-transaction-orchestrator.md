# API Changelog - Transaction Orchestrator Endpoints

**Date:** 2026-04-05  
**Version:** 1.1.0 (Minor - New endpoints added)  
**Service:** orchestrator-service  
**Breaking Changes:** No

---

## Summary

Added new transaction orchestration endpoints to replace legacy per-transaction-type endpoints.
The new endpoints use Temporal SAGA patterns for durable execution and automatic compensation.

---

## New Endpoints

### 1. POST `/api/v1/transactions`

**Purpose:** Start a new transaction workflow (withdrawal, deposit, bill payment, or DuitNow transfer)

**Request:**
```json
{
  "transactionType": "CASH_WITHDRAWAL",
  "agentId": "uuid",
  "amount": 100.00,
  "idempotencyKey": "optional-unique-key",
  "pan": "card-number",
  "pinBlock": "encrypted-pin",
  "customerCardMasked": "411111******1111",
  "geofenceLat": 3.1390,
  "geofenceLng": 101.6869,
  "agentTier": "TIER_1"
}
```

**Response (200):**
```json
{
  "status": "PENDING",
  "workflowId": "unique-workflow-id",
  "pollUrl": "/api/v1/transactions/unique-workflow-id/status"
}
```

**Required Fields by Transaction Type:**

| Field | CASH_WITHDRAWAL | CASH_DEPOSIT | BILL_PAYMENT | DUITNOW_TRANSFER |
|-------|----------------|--------------|--------------|------------------|
| `transactionType` | ✅ | ✅ | ✅ | ✅ |
| `agentId` | ✅ | ✅ | ✅ | ✅ |
| `amount` | ✅ | ✅ | ✅ | ✅ |
| `pan` | ✅ | | | |
| `pinBlock` | ✅ | | | |
| `destinationAccount` | | ✅ | | |
| `billerCode` | | | ✅ | |
| `ref1` | | | ✅ | |
| `proxyType` | | | | ✅ |
| `proxyValue` | | | | ✅ |

**Error Responses:**
- 400: Validation error (missing required fields, invalid format)
- 401: Unauthorized
- 500: Internal server error

---

### 2. GET `/api/v1/transactions/{workflowId}/status`

**Purpose:** Poll the status of a transaction workflow

**Path Parameters:**
- `workflowId` (string, required): The workflow ID returned from the start transaction response

**Response (200):**
```json
{
  "status": "COMPLETED",
  "workflowId": "unique-workflow-id",
  "transactionType": "CASH_WITHDRAWAL",
  "amount": 100.00,
  "customerFee": 2.00,
  "referenceNumber": "EXT-REF-123",
  "errorCode": null,
  "errorMessage": null,
  "actionCode": null,
  "completedAt": "2026-04-05T17:00:00Z"
}
```

**Status Values:**
- `PENDING` - Workflow not yet started
- `RUNNING` - Workflow in progress
- `COMPLETED` - Transaction completed successfully
- `FAILED` - Transaction failed
- `COMPENSATING` - SAGA compensation in progress
- `UNKNOWN` - Status cannot be determined

**Error Responses:**
- 401: Unauthorized
- 404: Workflow not found

---

### 3. POST `/api/v1/transactions/{workflowId}/force-resolve`

**Purpose:** Admin operation to manually resolve a stuck or failed workflow

**Path Parameters:**
- `workflowId` (string, required): The workflow ID to force resolve

**Request:**
```json
{
  "action": "RETRY",
  "reason": "Manual intervention required",
  "adminId": "admin-uuid"
}
```

**Response (200):**
```json
{
  "status": "SUCCESS",
  "message": "Force resolve signal sent for workflow: unique-workflow-id"
}
```

**Action Values:**
- `RETRY` - Retry the failed step
- `ABORT` - Cancel the workflow and trigger compensation

**Error Responses:**
- 400: Invalid action or workflow not found
- 401: Unauthorized
- 403: Forbidden - Admin access required

---

## Deprecated Endpoints

The following endpoints are now **deprecated** and will be removed in a future version.
Channel apps should migrate to the new unified endpoint.

| Deprecated Endpoint | Replacement |
|--------------------|-------------|
| `POST /api/v1/withdrawal` | `POST /api/v1/transactions` with `transactionType: CASH_WITHDRAWAL` |
| `POST /api/v1/deposit` | `POST /api/v1/transactions` with `transactionType: CASH_DEPOSIT` |
| `POST /api/v1/bill/pay` | `POST /api/v1/transactions` with `transactionType: BILL_PAYMENT` |
| `POST /api/v1/billpayment/jompay` | `POST /api/v1/transactions` with `transactionType: BILL_PAYMENT` |
| `POST /api/v1/topup` | `POST /api/v1/transactions` with `transactionType: BILL_PAYMENT` |
| `POST /api/v1/ewallet/withdraw` | `POST /api/v1/transactions` with `transactionType: CASH_WITHDRAWAL` |
| `POST /api/v1/transfer/duitnow` | `POST /api/v1/transactions` with `transactionType: DUITNOW_TRANSFER` |

**Deprecation Timeline:**
- **2026-04-05:** Endpoints marked as deprecated in OpenAPI spec
- **2026-06-01:** Deprecation warnings added to API responses (planned)
- **2026-09-01:** Endpoints removed (tentative)

---

## Migration Guide for Channel Apps

### Before (Legacy Endpoints)
```
POST /api/v1/withdraw
POST /api/v1/deposit
POST /api/v1/bill/pay
POST /api/v1/transfer/duitnow
```

### After (New Unified Endpoint)
```
POST /api/v1/transactions
```

### Changes Required:

1. **Replace multiple endpoints with single endpoint:**
   - All transaction types now use `POST /api/v1/transactions`
   - Specify transaction type via `transactionType` field in request body

2. **Update request format:**
   - Use `transactionType` enum instead of endpoint path
   - `agentId` must be UUID format (not string)
   - `amount` is now a number (not string)

3. **Update response handling:**
   - Response now returns `workflowId` and `pollUrl`
   - Use `pollUrl` to check transaction status
   - Initial status is always `PENDING`

4. **Implement polling:**
   - Poll `GET /api/v1/transactions/{workflowId}/status` for completion
   - Recommended polling interval: 2-5 seconds
   - Stop polling when status is `COMPLETED` or `FAILED`

5. **Handle new status values:**
   - `COMPENSATING` - SAGA is rolling back, wait for final status
   - `UNKNOWN` - Rare case, contact support

---

## New Schema Definitions

### TransactionType Enum
```yaml
type: string
enum: [CASH_WITHDRAWAL, CASH_DEPOSIT, BILL_PAYMENT, DUITNOW_TRANSFER]
```

### TransactionStartRequest
- `transactionType` (required): Type of transaction
- `agentId` (required): UUID of the agent
- `amount` (required): Transaction amount (number)
- `idempotencyKey` (optional): Unique key for deduplication
- Type-specific fields as documented above

### TransactionStartResponse
- `status`: Always "PENDING" initially
- `workflowId`: Unique identifier for polling
- `pollUrl`: URL to check status

### TransactionStatusResponse
- `status`: Current workflow status
- `workflowId`: Workflow identifier
- `transactionType`: Type of transaction
- `amount`: Transaction amount
- `customerFee`: Fee charged
- `referenceNumber`: External reference
- `errorCode`: Error code if failed
- `errorMessage`: Human-readable error
- `actionCode`: Recommended action (DECLINE/RETRY/REVIEW)
- `completedAt`: Completion timestamp

### ForceResolveRequest
- `action` (required): RETRY or ABORT
- `reason`: Reason for force resolve
- `adminId` (required): Admin performing action

---

## Backward Compatibility

- Legacy endpoints remain functional during transition period
- New endpoints are additive, no breaking changes to existing endpoints
- Channel apps can migrate at their own pace
- Deprecation notices will be issued before legacy endpoint removal

---

## Testing

### Example: Cash Withdrawal
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionType": "CASH_WITHDRAWAL",
    "agentId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "amount": 100.00,
    "pan": "4111111111111111",
    "pinBlock": "encrypted_pin_block",
    "customerCardMasked": "411111******1111",
    "geofenceLat": 3.1390,
    "geofenceLng": 101.6869,
    "agentTier": "TIER_1"
  }'
```

### Example: Check Status
```bash
curl http://localhost:8080/api/v1/transactions/<workflowId>/status \
  -H "Authorization: Bearer <token>"
```

---

## Notes

- All financial calculations are performed server-side
- Idempotency is enforced via `idempotencyKey` field
- Temporal workflows provide automatic retry and compensation
- No changes to authentication or authorization requirements
- GPS coordinates are validated for geofencing compliance
