#!/bin/bash
#
# BDD Section 4: Cash Deposit E2E Tests
# Tests cash deposit functionality with various scenarios

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 4: Cash Deposit"

# ============================================================================
# BDD-D01: Cash Deposit Tests
# ============================================================================

subsection "BDD-D01: Successful cash deposit"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$AGENT_TOKEN" "{
  \"amount\": 500.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"1234567890\",
  \"customerName\": \"Test Customer\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "Successful deposit" "200" "$(get_status "$response")"
assert_json_field "Deposit status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

subsection "BDD-D01-EC-01: Invalid account"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$AGENT_TOKEN" "{
  \"amount\": 500.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"INVALID\",
  \"customerName\": \"Test Customer\"
}")
assert_status "Invalid account" "400" "$(get_status "$response")"
assert_json_field "Error for invalid account" "$(get_body "$response")" ".error.code" "ERR_VAL_INVALID_ACCOUNT"

subsection "BDD-D01-EC-02: Zero amount"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$AGENT_TOKEN" "{
  \"amount\": 0,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"1234567890\",
  \"customerName\": \"Test Customer\"
}")
assert_status "Zero amount validation" "400" "$(get_status "$response")"
assert_json_field "Error for zero amount" "$(get_body "$response")" ".error.code" "ERR_VAL_AMOUNT_ZERO"

subsection "BDD-D01-EC-03: Negative amount"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$AGENT_TOKEN" "{
  \"amount\": -100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"1234567890\",
  \"customerName\": \"Test Customer\"
}")
assert_status "Negative amount validation" "400" "$(get_status "$response")"
assert_json_field "Error for negative amount" "$(get_body "$response")" ".error.code" "ERR_VAL_AMOUNT_NEGATIVE"

subsection "BDD-D01-EC-04: Float cap exceeded"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$AGENT_TOKEN" "{
  \"amount\": 999999.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"1234567890\",
  \"customerName\": \"Test Customer\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "Float cap exceeded" "402" "$(get_status "$response")"
assert_json_field "Error for float cap exceeded" "$(get_body "$response")" ".error.code" "ERR_BIZ_FLOAT_CAP_EXCEEDED"

# ============================================================================
# BDD-D02: Card-based Deposit Tests
# ============================================================================

subsection "BDD-D02: Card-based deposit"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$AGENT_TOKEN" "{
  \"amount\": 300.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "Card-based deposit" "200" "$(get_status "$response")"
assert_json_field "Card deposit status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

print_summary "Cash Deposit Tests"