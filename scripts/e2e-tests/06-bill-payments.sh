#!/bin/bash
#
# BDD Section 6: Bill Payments E2E Tests
# Tests JomPAY, ASTRO RPN, TM RPN, and EPF i-SARAAN payments

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 6: Bill Payments"

# ============================================================================
# BDD-B01: JomPAY Payment Tests
# ============================================================================

subsection "BDD-B01: JomPAY payment"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/billpayment/jompay" "$AGENT_TOKEN" "{
  \"billerCode\": \"2100\",
  \"ref1\": \"1234567890\",
  \"ref2\": \"123456\",
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerMobile\": \"60123456789\"
}")
assert_status "JomPAY payment" "200" "$(get_status "$response")"
assert_json_field "Payment status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"
assert_json_field "Biller type is JomPAY" "$(get_body "$response")" ".billerType" "JOMPAY"

subsection "BDD-B01-EC-01: Ref-1 validation fails"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/billpayment/jompay" "$AGENT_TOKEN" "{
  \"billerCode\": \"2100\",
  \"ref1\": \"INVALID\",
  \"ref2\": \"123456\",
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Ref-1 validation fails" "400" "$(get_status "$response")"
assert_json_field "Error for ref-1 validation" "$(get_body "$response")" ".error.code" "ERR_VAL_REF1_INVALID"

subsection "BDD-B01-EC-02: Biller timeout"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/billpayment/jompay" "$AGENT_TOKEN" "{
  \"billerCode\": \"9999\",
  \"ref1\": \"1234567890\",
  \"ref2\": \"123456\",
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "504" ]; then
    assert_json_field "Biller timeout error" "$(get_body "$response")" ".error.code" "ERR_EXT_BILLER_TIMEOUT"
else
    assert_status "Biller timeout" "504" "$actual_status"
fi

subsection "BDD-B01-EC-03: Insufficient float"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/billpayment/jompay" "$AGENT_TOKEN" "{
  \"billerCode\": \"2100\",
  \"ref1\": \"1234567890\",
  \"ref2\": \"123456\",
  \"amount\": 999999.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Insufficient float for payment" "402" "$(get_status "$response")"
assert_json_field "Error for insufficient float" "$(get_body "$response")" ".error.code" "ERR_BIZ_INSUFFICIENT_FLOAT"

# ============================================================================
# BDD-B02: ASTRO RPN Payment Tests
# ============================================================================

subsection "BDD-B02: ASTRO RPN payment"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/billpayment/astro-rpn" "$AGENT_TOKEN" "{
  \"subscriberNumber\": \"838291234567\",
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "ASTRO RPN payment" "200" "$(get_status "$response")"
assert_json_field "ASTRO payment status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field "Biller type is ASTRO" "$(get_body "$response")" ".billerType" "ASTRO_RPN"

# ============================================================================
# BDD-B03: TM RPN Payment Tests
# ============================================================================

subsection "BDD-B03: TM RPN payment"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/billpayment/tm-rpn" "$AGENT_TOKEN" "{
  \"accountNumber\": \"130024567890\",
  \"amount\": 150.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "TM RPN payment" "200" "$(get_status "$response")"
assert_json_field "TM payment status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field "Biller type is TM" "$(get_body "$response")" ".billerType" "TM_RPN"

# ============================================================================
# BDD-B04: EPF i-SARAAN Payment Tests
# ============================================================================

subsection "BDD-B04: EPF i-SARAAN payment"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/billpayment/epf-isaraan" "$AGENT_TOKEN" "{
  \"employerCode\": \"MY00012\",
  \"employeeNumber\": \"1234567890\",
  \"amount\": 500.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"contributionMonth\": \"2026-03\"
}")
assert_status "EPF i-SARAAN payment" "200" "$(get_status "$response")"
assert_json_field "EPF payment status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field "Biller type is EPF" "$(get_body "$response")" ".billerType" "EPF_ISARAAN"

print_summary "Bill Payments Tests"