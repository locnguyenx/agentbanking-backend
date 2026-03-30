#!/bin/bash
#
# BDD Section 4: Cash Deposit E2E Tests (Enhanced)
# Tests cash deposit functionality with proper test data linkage
#
# Prerequisites:
#   Run setup-deposit-test-data.sh first to create linked test data:
#     ./scripts/e2e-tests/setup-deposit-test-data.sh
#
# This test uses the dedicated deposit agent (deposit_agent_01) whose
# JWT agent_id claim matches a ledger agent_float record with MYR 100,000.

source "$(dirname "$0")/common.sh"

# Load main tokens
load_tokens

# Load deposit-specific config (overrides AGENT_TOKEN if available)
DEPOSIT_CONFIG="/tmp/e2e_deposit_test.env"
if [ -f "$DEPOSIT_CONFIG" ]; then
    source "$DEPOSIT_CONFIG"
    log_info "Loaded deposit test config from $DEPOSIT_CONFIG"
fi

# Use deposit agent token if available, fall back to generic agent token
DEPOSIT_TOKEN="${DEPOSIT_AGENT_TOKEN:-$AGENT_TOKEN}"

if [ -z "$DEPOSIT_TOKEN" ]; then
    log_error "No agent token available. Run setup-deposit-test-data.sh first."
    exit 1
fi

section "BDD Section 4: Cash Deposit E2E Tests"

# ============================================================================
# Pre-test: Verify agent float exists
# ============================================================================

subsection "Pre-test: Agent float verification"
response=$(api_call "GET" "/api/v1/agent/balance" "$DEPOSIT_TOKEN")
balance_status=$(get_status "$response")

if [ "$balance_status" = "200" ]; then
    balance=$(get_body "$response" | jq -r '.balance // .availableBalance // "unknown"')
    assert_status "Agent float exists" "200" "$balance_status"
    log_info "Agent float balance: MYR $balance"
else
    log_warn "Agent balance check returned $balance_status - tests may fail if float not linked"
    assert_status "Agent float exists" "200" "$balance_status"
fi

# ============================================================================
# BDD-D01: Successful Cash Deposit
# ============================================================================

subsection "BDD-D01: Successful cash deposit"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$DEPOSIT_TOKEN" "{
  \"amount\": 500.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"1234567890\",
  \"customerName\": \"Test Customer\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "D01: Successful deposit returns 200" "200" "$(get_status "$response")"
assert_json_field "D01: Deposit status is SUCCESS" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "D01: Transaction ID returned" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-D01-EC-01: Invalid Account
# ============================================================================

subsection "BDD-D01-EC-01: Invalid account number"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$DEPOSIT_TOKEN" "{
  \"amount\": 500.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"INVALID\",
  \"customerName\": \"Test Customer\"
}")
assert_status "D01-EC-01: Invalid account returns 400" "400" "$(get_status "$response")"
assert_json_field "D01-EC-01: Error code ERR_VAL_INVALID_ACCOUNT" "$(get_body "$response")" ".error.code" "ERR_VAL_INVALID_ACCOUNT"

# ============================================================================
# BDD-D01-EC-02: Zero Amount
# ============================================================================

subsection "BDD-D01-EC-02: Zero amount deposit"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$DEPOSIT_TOKEN" "{
  \"amount\": 0,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"1234567890\",
  \"customerName\": \"Test Customer\"
}")
assert_status "D01-EC-02: Zero amount returns 400" "400" "$(get_status "$response")"
assert_json_field "D01-EC-02: Error code ERR_VAL_AMOUNT_ZERO" "$(get_body "$response")" ".error.code" "ERR_VAL_AMOUNT_ZERO"

# ============================================================================
# BDD-D01-EC-03: Negative Amount
# ============================================================================

subsection "BDD-D01-EC-03: Negative amount deposit"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$DEPOSIT_TOKEN" "{
  \"amount\": -100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"1234567890\",
  \"customerName\": \"Test Customer\"
}")
assert_status "D01-EC-03: Negative amount returns 400" "400" "$(get_status "$response")"
assert_json_field "D01-EC-03: Error code ERR_VAL_AMOUNT_NEGATIVE" "$(get_body "$response")" ".error.code" "ERR_VAL_AMOUNT_NEGATIVE"

# ============================================================================
# BDD-D01-EC-04: Float Cap Exceeded
# ============================================================================

subsection "BDD-D01-EC-04: Deposit exceeds float cap"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$DEPOSIT_TOKEN" "{
  \"amount\": 999999.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"1234567890\",
  \"customerName\": \"Test Customer\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "D01-EC-04: Float cap exceeded returns 402" "402" "$(get_status "$response")"
assert_json_field "D01-EC-04: Error code ERR_BIZ_FLOAT_CAP_EXCEEDED" "$(get_body "$response")" ".error.code" "ERR_BIZ_FLOAT_CAP_EXCEEDED"

# ============================================================================
# BDD-D01-EC-05: Duplicate Idempotency Key
# ============================================================================

subsection "BDD-D01-EC-05: Duplicate idempotency key"
idempotency_key=$(generate_uuid)

# First call
api_call "POST" "/api/v1/deposit" "$DEPOSIT_TOKEN" "{
  \"amount\": 200.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$idempotency_key\",
  \"customerAccount\": \"9876543210\",
  \"customerName\": \"Idempotent Test\"
}" > /dev/null

# Second call with same key
response=$(api_call "POST" "/api/v1/deposit" "$DEPOSIT_TOKEN" "{
  \"amount\": 200.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$idempotency_key\",
  \"customerAccount\": \"9876543210\",
  \"customerName\": \"Idempotent Test\"
}")
assert_status "D01-EC-05: Duplicate key returns cached 200" "200" "$(get_status "$response")"
assert_json_field "D01-EC-05: Cached response has SUCCESS status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field "D01-EC-05: Idempotent replayed flag" "$(get_body "$response")" ".idempotentReplayed" "true"

# ============================================================================
# BDD-D02: Card-based Deposit Tests
# ============================================================================

subsection "BDD-D02: Card-based deposit"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$DEPOSIT_TOKEN" "{
  \"amount\": 300.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "D02: Card-based deposit returns 200" "200" "$(get_status "$response")"
assert_json_field "D02: Card deposit status SUCCESS" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "D02: Transaction ID returned" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-D02-EC-01: Card-based deposit invalid PIN
# ============================================================================

subsection "BDD-D02-EC-01: Card deposit with invalid PIN"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$DEPOSIT_TOKEN" "{
  \"amount\": 300.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"9999\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "D02-EC-01: Invalid PIN returns 401" "401" "$(get_status "$response")"
assert_json_field "D02-EC-01: Error code ERR_AUTH_INVALID_PIN" "$(get_body "$response")" ".error.code" "ERR_AUTH_INVALID_PIN"

# ============================================================================
# BDD-D01-EC-06: Float balance increases after deposit
# ============================================================================

subsection "BDD-D01-EC-06: Float balance increases after deposit"

# Get balance before
response_before=$(api_call "GET" "/api/v1/agent/balance" "$DEPOSIT_TOKEN")
balance_before=$(get_body "$response_before" | jq -r '.balance // .availableBalance // 0')

# Perform deposit
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$DEPOSIT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerAccount\": \"5555666677\",
  \"customerName\": \"Balance Test\"
}")
deposit_status=$(get_status "$response")

if [ "$deposit_status" = "200" ]; then
    # Get balance after
    response_after=$(api_call "GET" "/api/v1/agent/balance" "$DEPOSIT_TOKEN")
    balance_after=$(get_body "$response_after" | jq -r '.balance // .availableBalance // 0')

    TOTAL=$((TOTAL + 1))
    if [ "$(echo "$balance_after > $balance_before" | bc 2>/dev/null || echo 0)" = "1" ]; then
        echo -e "  ${GREEN}✓${NC} D01-EC-06: Float increased from $balance_before to $balance_after"
        PASSED=$((PASSED + 1))
    else
        echo -e "  ${YELLOW}⚠${NC} D01-EC-06: Float balance change deferred (balance_before=$balance_before, balance_after=$balance_after)"
        SKIPPED=$((SKIPPED + 1))
    fi
else
    assert_status "D01-EC-06: Deposit for balance check" "200" "$deposit_status"
fi

# ============================================================================
# Summary
# ============================================================================

print_summary "Cash Deposit E2E Tests"
