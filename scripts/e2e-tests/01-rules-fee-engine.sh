#!/bin/bash
#
# BDD Section 1: Rules & Fee Engine E2E Tests
# Tests fee configuration, transaction limits, and velocity checks

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 1: Rules & Fee Engine"

# ============================================================================
# BDD-R01: Fee Structure Configuration Tests
# ============================================================================

subsection "BDD-R01: Configure fee structure for Micro agent cash withdrawal"
response=$(api_call "POST" "/api/v1/rules/fees" "$ADMIN_TOKEN" '{
  "agentType": "MICRO",
  "transactionType": "CASH_WITHDRAWAL",
  "feeType": "FIXED",
  "feeAmount": 1.50,
  "currency": "MYR",
  "effectiveFrom": "2026-01-01T00:00:00Z",
  "effectiveTo": "2026-12-31T23:59:59Z"
}')
assert_status "Fee config for Micro agent" "201" "$(get_status "$response")"
assert_json_field "Fee type is FIXED" "$(get_body "$response")" ".feeType" "FIXED"

subsection "BDD-R01-PCT: Percentage-based fee for Premier agent"
response=$(api_call "POST" "/api/v1/rules/fees" "$ADMIN_TOKEN" '{
  "agentType": "PREMIER",
  "transactionType": "CASH_WITHDRAWAL",
  "feeType": "PERCENTAGE",
  "percentage": 0.50,
  "minFee": 2.00,
  "maxFee": 50.00,
  "currency": "MYR",
  "effectiveFrom": "2026-01-01T00:00:00Z",
  "effectiveTo": "2026-12-31T23:59:59Z"
}')
assert_status "Percentage fee for Premier" "201" "$(get_status "$response")"
assert_json_field "Fee type is PERCENTAGE" "$(get_body "$response")" ".feeType" "PERCENTAGE"
assert_json_field "Percentage is 0.50%" "$(get_body "$response")" ".percentage" "0.50"

subsection "BDD-R01-EC-01: No fee configuration"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 500.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"agentType\": \"UNKNOWN\"
}")
assert_status "Withdrawal without fee config" "402" "$(get_status "$response")"
assert_json_field "Error code present" "$(get_body "$response")" ".error.code" "ERR_BIZ_FEE_NOT_CONFIGURED"

subsection "BDD-R01-EC-02: Expired fee config"
response=$(api_call "POST" "/api/v1/rules/fees" "$ADMIN_TOKEN" '{
  "agentType": "STANDARD",
  "transactionType": "CASH_WITHDRAWAL",
  "feeType": "FIXED",
  "feeAmount": 1.00,
  "currency": "MYR",
  "effectiveFrom": "2020-01-01T00:00:00Z",
  "effectiveTo": "2020-12-31T23:59:59Z"
}')
IDEMPOTENCY_KEY=$(generate_uuid)
withdraw_response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"agentType\": \"STANDARD\"
}")
assert_status "Withdrawal with expired fee" "402" "$(get_status "$withdraw_response")"

# ============================================================================
# BDD-R02: Daily Transaction Limit Tests
# ============================================================================

subsection "BDD-R02: Daily transaction limit check passes"
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" '{
  "amount": 100.00,
  "currency": "MYR",
  "idempotencyKey": "'$(generate_uuid)'"
}')
actual_status=$(get_status "$response")
if [ "$actual_status" = "200" ] || [ "$actual_status" = "201" ]; then
    assert_json_field "Daily limit check passes" "$(get_body "$response")" ".status" "SUCCESS"
else
    assert_status "Daily limit check passes" "200" "$actual_status"
fi

subsection "BDD-R02-EC-01: Daily limit exceeded"
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" '{
  "amount": 10000.00,
  "currency": "MYR",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Daily limit exceeded" "403" "$(get_status "$response")"
assert_json_field "Error code for limit exceeded" "$(get_body "$response")" ".error.code" "ERR_BIZ_DAILY_LIMIT_EXCEEDED"

subsection "BDD-R02-EC-02: Daily count limit exceeded"
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" '{
  "amount": 50.00,
  "currency": "MYR",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Daily count limit" "403" "$(get_status "$response")"
assert_json_field "Error code for count limit" "$(get_body "$response")" ".error.code" "ERR_BIZ_DAILY_COUNT_EXCEEDED"

subsection "BDD-R02-EC-03: Zero amount"
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" '{
  "amount": 0,
  "currency": "MYR",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Zero amount validation" "400" "$(get_status "$response")"
assert_json_field "Error for zero amount" "$(get_body "$response")" ".error.code" "ERR_VAL_AMOUNT_ZERO"

subsection "BDD-R02-EC-04: Negative amount"
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" '{
  "amount": -100.00,
  "currency": "MYR",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Negative amount validation" "400" "$(get_status "$response")"
assert_json_field "Error for negative amount" "$(get_body "$response")" ".error.code" "ERR_VAL_AMOUNT_NEGATIVE"

# ============================================================================
# BDD-R03: Velocity Check Tests
# ============================================================================

subsection "BDD-R03: Velocity check passes"
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" '{
  "amount": 50.00,
  "currency": "MYR",
  "idempotencyKey": "'$(generate_uuid)'"
}')
actual_status=$(get_status "$response")
if [ "$actual_status" = "200" ] || [ "$actual_status" = "201" ]; then
    assert_json_field "Velocity check passes" "$(get_body "$response")" ".status" "SUCCESS"
else
    assert_status "Velocity check passes" "200" "$actual_status"
fi

subsection "BDD-R03-EC-01: Velocity count exceeded"
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" '{
  "amount": 50.00,
  "currency": "MYR",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Velocity count exceeded" "429" "$(get_status "$response")"
assert_json_field "Error for velocity exceeded" "$(get_body "$response")" ".error.code" "ERR_BIZ_VELOCITY_EXCEEDED"

# ============================================================================
# BDD-R04: Percentage-based Fee Calculation
# ============================================================================

subsection "BDD-R04: Percentage-based fee calculation with rounding"
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" '{
  "amount": 1000.00,
  "currency": "MYR",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Fee calculation request" "200" "$(get_status "$response")"
assert_json_field_exists "Fee field present" "$(get_body "$response")" ".fee"
assert_json_field_number "Fee is numeric" "$(get_body "$response")" ".fee"

print_summary "Rules & Fee Engine Tests"