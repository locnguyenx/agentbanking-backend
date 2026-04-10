# Orchestrator's workflow screen
to show the details of all orchestrator workflows

**Navigation:** Add "Orchestrator Workflows to sidebar Layout, before the "Transaction Resolution"

List all workflows, using results from api: /api/v1/transactions/{workflowId}/status & other info
Example:
Response (200 OK):
{
  "status": "COMPLETED",
  "workflowId": "IDEM-uuid-123",
  "transactionId": "TXN-uuid-456",
  "amount": 500.00,
  "customerFee": 1.00,
  "referenceNumber": "PAYNET-REF-789",
  "completedAt": "2026-04-05T14:30:00+08:00"
  "agentId":
  "agentCode":
  "username":
}

Response (200 OK) — Failed:
{
  "status": "FAILED",
  "workflowId": "IDEM-uuid-123",
  "error": {
    "code": "ERR_INSUFFICIENT_FUNDS",
    "message": "Customer account balance too low.",
    "action_code": "DECLINE"
  }
}

