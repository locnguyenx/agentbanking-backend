#!/bin/bash
#
# BDD Section 8: DuitNow & JomPAY E2E Tests
# Tests DuitNow transfers (mobile, NRIC, BRN) and JomPAY (ON-US, OFF-US)

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 8: DuitNow & JomPAY"

# ============================================================================
# BDD-DNOW-01: DuitNow Transfer via Mobile
# ============================================================================

subsection "BDD-DNOW-01: DuitNow transfer via mobile"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/transfer/duitnow" "$AGENT_TOKEN" "{
  \"proxyType\": \"MOBILE\",
  \"proxyValue\": \"60123456789\",
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"recipientName\": \"AHMAD BIN ISMAIL\",
  \"remark\": \"Test transfer\"
}")
assert_status "DuitNow mobile transfer" "200" "$(get_status "$response")"
assert_json_field "Transfer status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"
assert_json_field "Transfer type is DuitNow" "$(get_body "$response")" ".transferType" "DUITNOW"

subsection "BDD-DNOW-01-EC-01: Recipient account closed"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/transfer/duitnow" "$AGENT_TOKEN" "{
  \"proxyType\": \"MOBILE\",
  \"proxyValue\": \"60199999999\",
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"recipientName\": \"CLOSED ACCOUNT\",
  \"remark\": \"Test closed account\"
}")
assert_status "Recipient account closed" "400" "$(get_status "$response")"
assert_json_field "Error for closed account" "$(get_body "$response")" ".error.code" "ERR_BIZ_ACCOUNT_CLOSED"

subsection "BDD-DNOW-01-EC-02: Network timeout"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/transfer/duitnow" "$AGENT_TOKEN" "{
  \"proxyType\": \"MOBILE\",
  \"proxyValue\": \"60188888888\",
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"recipientName\": \"TIMEOUT TEST\",
  \"remark\": \"Test timeout\"
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "504" ]; then
    assert_json_field "Network timeout error" "$(get_body "$response")" ".error.code" "ERR_EXT_NETWORK_TIMEOUT"
else
    assert_status "Network timeout" "504" "$actual_status"
fi

subsection "BDD-DNOW-01-EC-03: Invalid proxy"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/transfer/duitnow" "$AGENT_TOKEN" "{
  \"proxyType\": \"MOBILE\",
  \"proxyValue\": \"INVALID\",
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"recipientName\": \"INVALID PROXY\",
  \"remark\": \"Test invalid proxy\"
}")
assert_status "Invalid proxy" "400" "$(get_status "$response")"
assert_json_field "Error for invalid proxy" "$(get_body "$response")" ".error.code" "ERR_VAL_INVALID_PROXY"

# ============================================================================
# BDD-DNOW-01: DuitNow Transfer via NRIC
# ============================================================================

subsection "BDD-DNOW-01-NRIC: Transfer via NRIC"
IDEMPOTENCY_KEY=$(generate_uuid)
mykad_number=$(random_mykad)
response=$(api_call "POST" "/api/v1/transfer/duitnow" "$AGENT_TOKEN" "{
  \"proxyType\": \"NRIC\",
  \"proxyValue\": \"$mykad_number\",
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"recipientName\": \"TEST CUSTOMER\",
  \"remark\": \"NRIC transfer\"
}")
assert_status "DuitNow NRIC transfer" "200" "$(get_status "$response")"
assert_json_field "NRIC transfer status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-DNOW-01: DuitNow Transfer via BRN
# ============================================================================

subsection "BDD-DNOW-01-BRN: Transfer via BRN"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/transfer/duitnow" "$AGENT_TOKEN" "{
  \"proxyType\": \"BRN\",
  \"proxyValue\": \"201901234567\",
  \"amount\": 200.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"recipientName\": \"TEST SDN BHD\",
  \"remark\": \"BRN transfer\"
}")
assert_status "DuitNow BRN transfer" "200" "$(get_status "$response")"
assert_json_field "BRN transfer status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-DNOW-02: JomPAY ON-US
# ============================================================================

subsection "BDD-DNOW-02: JomPAY ON-US"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/transfer/duitnow" "$AGENT_TOKEN" "{
  \"transferType\": \"JOMPAY\",
  \"billerCode\": \"2100\",
  \"ref1\": \"1234567890\",
  \"ref2\": \"123456\",
  \"amount\": 75.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "JomPAY ON-US" "200" "$(get_status "$response")"
assert_json_field "JomPAY status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-DNOW-03: JomPAY OFF-US
# ============================================================================

subsection "BDD-DNOW-03: JomPAY OFF-US"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/transfer/duitnow" "$AGENT_TOKEN" "{
  \"transferType\": \"JOMPAY\",
  \"billerCode\": \"3200\",
  \"ref1\": \"9876543210\",
  \"ref2\": \"654321\",
  \"amount\": 120.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "JomPAY OFF-US" "200" "$(get_status "$response")"
assert_json_field "JomPAY OFF-US status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

print_summary "DuitNow & JomPAY Tests"
