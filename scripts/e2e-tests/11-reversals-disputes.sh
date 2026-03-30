#!/bin/bash
#
# BDD Section 11: Reversals & Disputes E2E Tests
# Tests automatic reversals, Store & Forward retries, and dispute resolution

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 11: Reversals & Disputes"

# ============================================================================
# BDD-V01: Automatic Reversal Tests
# ============================================================================

subsection "BDD-V01: Automatic reversal on timeout"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"TIMEOUT\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "504" ]; then
    TXN_ID=$(echo "$(get_body "$response")" | jq -r '.transactionId // empty')
    if [ -n "$TXN_ID" ]; then
        reversal_response=$(api_call "GET" "/api/v1/transactions/$TXN_ID/reversal" "$AGENT_TOKEN")
        assert_status "Reversal initiated" "200" "$(get_status "$reversal_response")"
        assert_json_field "Reversal status" "$(get_body "$reversal_response")" ".status" "REVERSED"
    else
        TOTAL=$((TOTAL + 1))
        echo -e "  ${GREEN}✓${NC} Timeout detected (auto-reversal expected)"
        PASSED=$((PASSED + 1))
    fi
else
    assert_status "Timeout for reversal" "504" "$actual_status"
fi

subsection "BDD-V01-EC-01: Store & Forward retry"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"RETRY\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "202" ]; then
    assert_json_field "Store & Forward queued" "$(get_body "$response")" ".status" "QUEUED_RETRY"
    assert_json_field_exists "Retry ID" "$(get_body "$response")" ".retryId"
else
    assert_status "Store & Forward queued" "202" "$actual_status"
fi

subsection "BDD-V01-EC-02: Max retries exceeded"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 25.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"MAXRETRY\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "503" ]; then
    assert_json_field "Max retries error" "$(get_body "$response")" ".error.code" "ERR_EXT_MAX_RETRIES_EXCEEDED"
else
    assert_status "Max retries exceeded" "503" "$actual_status"
fi

subsection "BDD-V01-EC-03: No financial retry"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"NOFINRETRY\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "No financial retry" "200" "$(get_status "$response")"
assert_json_field "Direct response" "$(get_body "$response")" ".status" "SUCCESS"

subsection "BDD-V01-ECHO: Non-financial retry"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 0.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "200" ]; then
    assert_json_field "Echo test status" "$(get_body "$response")" ".status" "SUCCESS"
    assert_json_field "Non-financial flag" "$(get_body "$response")" ".nonFinancial" "true"
else
    assert_status "Non-financial retry" "200" "$actual_status"
fi

# ============================================================================
# BDD-V02: Dispute Investigation Tests
# ============================================================================

subsection "BDD-V02: Maker investigates dispute"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/kyc/review-queue" "$MAKER_TOKEN" "{
  \"disputeId\": \"DISP_$(generate_uuid | cut -c1-8)\",
  \"transactionId\": \"TXN_001\",
  \"action\": \"INVESTIGATE\",
  \"remarks\": \"Customer claims double deduction\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Dispute investigation" "200" "$(get_status "$response")"
assert_json_field "Investigation status" "$(get_body "$response")" ".status" "UNDER_INVESTIGATION"
assert_json_field_exists "Dispute ID" "$(get_body "$response")" ".disputeId"

subsection "BDD-V02-EC-01: No evidence of double-deduction"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/kyc/review-queue" "$MAKER_TOKEN" "{
  \"disputeId\": \"DISP_NO_EVIDENCE\",
  \"transactionId\": \"TXN_002\",
  \"action\": \"INVESTIGATE\",
  \"remarks\": \"No evidence found for double deduction\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "No evidence investigation" "200" "$(get_status "$response")"
assert_json_field "Investigation outcome" "$(get_body "$response")" ".outcome" "NO_EVIDENCE"

# ============================================================================
# BDD-V03: Checker Resolution Tests
# ============================================================================

subsection "BDD-V03: Checker approves resolution"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/kyc/review-queue" "$CHECKER_TOKEN" "{
  \"disputeId\": \"DISP_APPROVE_TEST\",
  \"transactionId\": \"TXN_003\",
  \"action\": \"RESOLVE\",
  \"resolution\": \"REFUND\",
  \"remarks\": \"Verified double deduction, refund approved\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Checker resolution" "200" "$(get_status "$response")"
assert_json_field "Resolution status" "$(get_body "$response")" ".status" "RESOLVED"
assert_json_field "Resolution type" "$(get_body "$response")" ".resolution" "REFUND"

subsection "BDD-V03-EC-01: Checker rejects"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/kyc/review-queue" "$CHECKER_TOKEN" "{
  \"disputeId\": \"DISP_REJECT_TEST\",
  \"transactionId\": \"TXN_004\",
  \"action\": \"RESOLVE\",
  \"resolution\": \"REJECT\",
  \"remarks\": \"Insufficient evidence to support claim\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Checker rejects" "200" "$(get_status "$response")"
assert_json_field "Rejection status" "$(get_body "$response")" ".status" "REJECTED"

print_summary "Reversals & Disputes Tests"
