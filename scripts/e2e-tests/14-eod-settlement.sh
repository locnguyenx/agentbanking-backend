#!/bin/bash
#
# BDD Section 14: EOD Settlement E2E Tests
# Tests end-of-day settlement calculations, CBS file generation, and reconciliation

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 14: EOD Settlement"

# ============================================================================
# BDD-SM01: Positive Settlement (Bank Owes Agent)
# ============================================================================

subsection "BDD-SM01: Positive settlement (bank owes agent)"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/sale" "$AGENT_TOKEN" "{
  \"amount\": 250.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\"
}")
assert_status "Retail sale for settlement" "200" "$(get_status "$response")"

IDEMPOTENCY_KEY=$(generate_uuid)
settle_response=$(api_call "POST" "/api/v1/backoffice/settlement" "$CHECKER_TOKEN" "{
  \"agentId\": \"AGENT_001\",
  \"settlementDate\": \"$(date +%Y-%m-%d)\",
  \"trigger\": \"EOD\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Settlement trigger" "200" "$(get_status "$settle_response")"
assert_json_field "Settlement direction" "$(get_body "$settle_response")" ".direction" "BANK_OWES_AGENT"
assert_json_field_exists "Settlement amount" "$(get_body "$settle_response")" ".netAmount"
assert_json_field_exists "Settlement ID" "$(get_body "$settle_response")" ".settlementId"

# ============================================================================
# BDD-SM01-EC-01: Negative Settlement (Agent Owes Bank)
# ============================================================================

subsection "BDD-SM01-EC-01: Negative settlement (agent owes bank)"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$AGENT_TOKEN" "{
  \"amount\": 1000.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerMykad\": \"880101101234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "Deposit for negative settlement" "200" "$(get_status "$response")"

IDEMPOTENCY_KEY=$(generate_uuid)
settle_response=$(api_call "POST" "/api/v1/backoffice/settlement" "$CHECKER_TOKEN" "{
  \"agentId\": \"AGENT_002\",
  \"settlementDate\": \"$(date +%Y-%m-%d)\",
  \"trigger\": \"EOD\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Negative settlement trigger" "200" "$(get_status "$settle_response")"
assert_json_field "Settlement direction" "$(get_body "$settle_response")" ".direction" "AGENT_OWES_BANK"

# ============================================================================
# BDD-SM01-EC-02: Pending Transactions Block Settlement
# ============================================================================

subsection "BDD-SM01-EC-02: Pending transactions block settlement"
IDEMPOTENCY_KEY=$(generate_uuid)
api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"TIMEOUT\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}" > /dev/null 2>&1

IDEMPOTENCY_KEY=$(generate_uuid)
settle_response=$(api_call "POST" "/api/v1/backoffice/settlement" "$CHECKER_TOKEN" "{
  \"agentId\": \"AGENT_003\",
  \"settlementDate\": \"$(date +%Y-%m-%d)\",
  \"trigger\": \"EOD\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
actual_status=$(get_status "$settle_response")
if [ "$actual_status" = "409" ]; then
    assert_json_field "Pending txn error" "$(get_body "$settle_response")" ".error.code" "ERR_BIZ_PENDING_TRANSACTIONS"
else
    assert_status "Pending transactions block" "409" "$actual_status"
fi

# ============================================================================
# BDD-SM01-EC-03: Float Top-ups Excluded
# ============================================================================

subsection "BDD-SM01-EC-03: Float top-ups excluded"
IDEMPOTENCY_KEY=$(generate_uuid)
api_call "POST" "/internal/ledger/float/fund" "$ADMIN_TOKEN" "{
  \"agentId\": \"AGENT_001\",
  \"amount\": 5000.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}" > /dev/null 2>&1

IDEMPOTENCY_KEY=$(generate_uuid)
settle_response=$(api_call "POST" "/api/v1/backoffice/settlement" "$CHECKER_TOKEN" "{
  \"agentId\": \"AGENT_001\",
  \"settlementDate\": \"$(date +%Y-%m-%d)\",
  \"trigger\": \"EOD\",
  \"includeFloatTopups\": false,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Settlement without top-ups" "200" "$(get_status "$settle_response")"
assert_json_field "Float excluded" "$(get_body "$settle_response")" ".floatTopupsExcluded" "true"

# ============================================================================
# BDD-SM01-MER: Retail Sales in Settlement
# ============================================================================

subsection "BDD-SM01-MER: Retail sales in settlement"
IDEMPOTENCY_KEY=$(generate_uuid)
api_call "POST" "/api/v1/retail/sale" "$AGENT_TOKEN" "{
  \"amount\": 120.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\"
}" > /dev/null 2>&1

IDEMPOTENCY_KEY=$(generate_uuid)
settle_response=$(api_call "POST" "/api/v1/backoffice/settlement" "$CHECKER_TOKEN" "{
  \"agentId\": \"AGENT_001\",
  \"settlementDate\": \"$(date +%Y-%m-%d)\",
  \"trigger\": \"EOD\",
  \"includeRetail\": true,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Settlement with retail" "200" "$(get_status "$settle_response")"
assert_json_field_exists "Retail total" "$(get_body "$settle_response")" ".retailSalesTotal"

# ============================================================================
# BDD-SM02: CBS File Generation
# ============================================================================

subsection "BDD-SM02: CBS file generation"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/settlement" "$CHECKER_TOKEN" "{
  \"agentId\": \"AGENT_001\",
  \"settlementDate\": \"$(date +%Y-%m-%d)\",
  \"trigger\": \"CBS_EXPORT\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "CBS file generation" "200" "$(get_status "$response")"
assert_json_field_exists "File reference" "$(get_body "$response")" ".cbsFileReference"
assert_json_field_exists "File path" "$(get_body "$response")" ".cbsFilePath"

# ============================================================================
# BDD-SM02-EC-01: No Transactions for Agent
# ============================================================================

subsection "BDD-SM02-EC-01: No transactions for agent"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/settlement" "$CHECKER_TOKEN" "{
  \"agentId\": \"AGENT_NO_TXN\",
  \"settlementDate\": \"$(date +%Y-%m-%d)\",
  \"trigger\": \"EOD\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "No transactions settlement" "200" "$(get_status "$response")"
assert_json_field "Zero settlement" "$(get_body "$response")" ".netAmount" "0.00"

# ============================================================================
# BDD-SM03: Successful Reconciliation
# ============================================================================

subsection "BDD-SM03: Successful reconciliation"
response=$(api_call "POST" "/api/v1/backoffice/settlement/reconcile" "$CHECKER_TOKEN" "{
  \"settlementId\": \"SETTLE_001\",
  \"action\": \"RECONCILE\"
}")
assert_status "Reconciliation" "200" "$(get_status "$response")"
assert_json_field "Reconciliation status" "$(get_body "$response")" ".status" "RECONCILED"
assert_json_field "Match count" "$(get_body "$response")" ".matched" "true"

# ============================================================================
# BDD-SM03-EC-01: Ghost Transaction Detected
# ============================================================================

subsection "BDD-SM03-EC-01: Ghost transaction detected"
response=$(api_call "POST" "/api/v1/backoffice/settlement/reconcile" "$CHECKER_TOKEN" "{
  \"settlementId\": \"SETTLE_GHOST\",
  \"action\": \"RECONCILE\"
}")
assert_status "Ghost reconciliation" "200" "$(get_status "$response")"
assert_json_field "Discrepancy found" "$(get_body "$response")" ".status" "DISCREPANCY_FOUND"
assert_json_field_exists "Ghost transactions" "$(get_body "$response")" ".ghostTransactions"

print_summary "EOD Settlement Tests"
