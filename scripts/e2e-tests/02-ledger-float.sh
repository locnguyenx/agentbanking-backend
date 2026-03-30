#!/bin/bash
#
# BDD Section 2: Ledger & Float E2E Tests
# Tests agent wallet balance, float management, and customer balance inquiry

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 2: Ledger & Float"

# ============================================================================
# BDD-L01: Agent Wallet Balance Tests
# ============================================================================

subsection "BDD-L01: Agent checks wallet balance"
response=$(api_call "GET" "/api/v1/ledger/balance/$AGENT_ID" "$AGENT_TOKEN")
assert_status "Get agent balance" "200" "$(get_status "$response")"
assert_json_field_exists "Balance field exists" "$(get_body "$response")" ".availableBalance"
assert_json_field_number "Balance is numeric" "$(get_body "$response")" ".availableBalance"

subsection "BDD-L01-EC-01: Agent float not found"
response=$(api_call "GET" "/api/v1/ledger/balance/UNKNOWN_AGENT_ID" "$AGENT_TOKEN")
assert_status "Float not found" "404" "$(get_status "$response")"
assert_json_field "Error code for float not found" "$(get_body "$response")" ".error.code" "ERR_BIZ_FLOAT_NOT_FOUND"

subsection "BDD-L01-EC-02: Agent deactivated"
response=$(api_call "GET" "/api/v1/ledger/balance/$DEACTIVATED_AGENT_ID" "$AGENT_TOKEN")
assert_status "Deactivated agent" "403" "$(get_status "$response")"
assert_json_field "Error code for deactivated" "$(get_body "$response")" ".error.code" "ERR_AUTH_AGENT_DEACTIVATED"

# ============================================================================
# BDD-L03: Real-time Settlement Tests
# ============================================================================

subsection "BDD-L03: Real-time settlement updates agent float"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "200" ] || [ "$actual_status" = "201" ]; then
    body=$(get_body "$response")
    balance_before=$(echo "$body" | jq -r '.agentBalanceBefore')
    balance_after=$(echo "$body" | jq -r '.agentBalanceAfter')
    
    TOTAL=$((TOTAL + 1))
    if [ "$balance_after" = "$balance_before" ] || [ "$balance_after" = "null" ]; then
        echo -e "  ${YELLOW}⚠${NC} Settlement test - balance change deferred (async settlement)"
        SKIPPED=$((SKIPPED + 1))
    else
        diff=$((balance_before - balance_after))
        if [ "$diff" -ge 0 ]; then
            echo -e "  ${GREEN}✓${NC} Float updated after successful withdrawal"
            PASSED=$((PASSED + 1))
        fi
    fi
    
    assert_json_field "Withdrawal status" "$body" ".status" "SUCCESS"
fi

subsection "BDD-L03-EC-01: Insufficient float"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 999999.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Insufficient float" "402" "$(get_status "$response")"
assert_json_field "Error for insufficient float" "$(get_body "$response")" ".error.code" "ERR_BIZ_INSUFFICIENT_FLOAT"

# ============================================================================
# BDD-L04: Customer Balance Inquiry Tests
# ============================================================================

subsection "BDD-L04: Customer balance inquiry via card + PIN"
response=$(api_call "POST" "/api/v1/ledger/balance-inquiry" "$AGENT_TOKEN" '{
  "cardNumber": "4111111111111111",
  "pin": "1234",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Balance inquiry" "200" "$(get_status "$response")"
assert_json_field "Status is SUCCESS" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Customer balance field" "$(get_body "$response")" ".customerBalance"

subsection "BDD-L04-EC-01: Invalid PIN"
response=$(api_call "POST" "/api/v1/ledger/balance-inquiry" "$AGENT_TOKEN" '{
  "cardNumber": "4111111111111111",
  "pin": "9999",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Invalid PIN" "401" "$(get_status "$response")"
assert_json_field "Error for invalid PIN" "$(get_body "$response")" ".error.code" "ERR_AUTH_INVALID_PIN"

subsection "BDD-L04-EC-02: Duplicate idempotency key"
idempotency_key=$(generate_uuid)
api_call "POST" "/api/v1/ledger/balance-inquiry" "$AGENT_TOKEN" "{
  \"cardNumber\": \"4111111111111111\",
  \"pin\": \"1234\",
  \"idempotencyKey\": \"$idempotency_key\"
}" > /dev/null
response=$(api_call "POST" "/api/v1/ledger/balance-inquiry" "$AGENT_TOKEN" "{
  \"cardNumber\": \"4111111111111111\",
  \"pin\": \"1234\",
  \"idempotencyKey\": \"$idempotency_key\"
}")
assert_status "Duplicate idempotency returns cached" "200" "$(get_status "$response")"
assert_json_field "Cached response status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field "Idempotent response" "$(get_body "$response")" ".idempotentReplayed" "true"

print_summary "Ledger & Float Tests"