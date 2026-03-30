#!/bin/bash
#
# BDD Section 7: Prepaid Top-Up E2E Tests
# Tests Celcom, M1, and card-based prepaid top-up scenarios

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 7: Prepaid Top-Up"

# ============================================================================
# BDD-T01: Celcom Top-Up Tests
# ============================================================================

subsection "BDD-T01: Celcom top-up"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/topup" "$AGENT_TOKEN" "{
  \"telco\": \"CELCOM\",
  \"phoneNumber\": \"60191234567\",
  \"amount\": 10.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Celcom top-up" "200" "$(get_status "$response")"
assert_json_field "Top-up status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"
assert_json_field "Telco is Celcom" "$(get_body "$response")" ".telco" "CELCOM"

subsection "BDD-T01-EC-01: Invalid phone number"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/topup" "$AGENT_TOKEN" "{
  \"telco\": \"CELCOM\",
  \"phoneNumber\": \"1234\",
  \"amount\": 10.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Invalid phone number" "400" "$(get_status "$response")"
assert_json_field "Error for invalid phone" "$(get_body "$response")" ".error.code" "ERR_VAL_INVALID_PHONE"

subsection "BDD-T01-EC-02: Aggregator timeout"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/topup" "$AGENT_TOKEN" "{
  \"telco\": \"CELCOM\",
  \"phoneNumber\": \"60199999999\",
  \"amount\": 10.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "504" ]; then
    assert_json_field "Aggregator timeout error" "$(get_body "$response")" ".error.code" "ERR_EXT_AGGREGATOR_TIMEOUT"
else
    assert_status "Aggregator timeout" "504" "$actual_status"
fi

subsection "BDD-T01-CARD: Card top-up"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/topup" "$AGENT_TOKEN" "{
  \"telco\": \"CELCOM\",
  \"phoneNumber\": \"60191234567\",
  \"amount\": 10.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\"
}")
assert_status "Card top-up" "200" "$(get_status "$response")"
assert_json_field "Card top-up status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-T02: M1 Top-Up Tests
# ============================================================================

subsection "BDD-T02: M1 top-up"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/topup" "$AGENT_TOKEN" "{
  \"telco\": \"M1\",
  \"phoneNumber\": \"6591234567\",
  \"amount\": 5.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "M1 top-up" "200" "$(get_status "$response")"
assert_json_field "M1 top-up status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field "Telco is M1" "$(get_body "$response")" ".telco" "M1"

subsection "BDD-T02-EC-01: Invalid phone number"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/topup" "$AGENT_TOKEN" "{
  \"telco\": \"M1\",
  \"phoneNumber\": \"1234\",
  \"amount\": 5.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Invalid M1 phone number" "400" "$(get_status "$response")"
assert_json_field "Error for invalid M1 phone" "$(get_body "$response")" ".error.code" "ERR_VAL_INVALID_PHONE"

print_summary "Prepaid Top-Up Tests"
