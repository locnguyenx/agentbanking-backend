#!/bin/bash
#
# BDD Section 9: eWallet & eSSP E2E Tests
# Tests Sarawak Pay withdrawals/top-ups and eSSP purchases

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 9: eWallet & eSSP"

# ============================================================================
# BDD-WAL-01: Sarawak Pay Withdrawal Tests
# ============================================================================

subsection "BDD-WAL-01: Sarawak Pay withdrawal"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/ewallet/withdraw" "$AGENT_TOKEN" "{
  \"walletProvider\": \"SARAWAK_PAY\",
  \"walletAccountId\": \"SP123456789\",
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Sarawak Pay withdrawal" "200" "$(get_status "$response")"
assert_json_field "Withdrawal status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"
assert_json_field "Wallet provider" "$(get_body "$response")" ".walletProvider" "SARAWAK_PAY"

subsection "BDD-WAL-01-EC-01: Insufficient wallet"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/ewallet/withdraw" "$AGENT_TOKEN" "{
  \"walletProvider\": \"SARAWAK_PAY\",
  \"walletAccountId\": \"SP_LOW_BALANCE\",
  \"amount\": 99999.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Insufficient wallet" "402" "$(get_status "$response")"
assert_json_field "Error for insufficient wallet" "$(get_body "$response")" ".error.code" "ERR_BIZ_INSUFFICIENT_WALLET"

subsection "BDD-WAL-01-CARD: Card withdrawal"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/ewallet/withdraw" "$AGENT_TOKEN" "{
  \"walletProvider\": \"SARAWAK_PAY\",
  \"walletAccountId\": \"SP123456789\",
  \"amount\": 30.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\"
}")
assert_status "Card withdrawal" "200" "$(get_status "$response")"
assert_json_field "Card withdrawal status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-WAL-02: Sarawak Pay Top-Up Tests
# ============================================================================

subsection "BDD-WAL-02: Sarawak Pay top-up"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/ewallet/topup" "$AGENT_TOKEN" "{
  \"walletProvider\": \"SARAWAK_PAY\",
  \"walletAccountId\": \"SP123456789\",
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Sarawak Pay top-up" "200" "$(get_status "$response")"
assert_json_field "Top-up status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

subsection "BDD-WAL-02-CARD: Card top-up"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/ewallet/topup" "$AGENT_TOKEN" "{
  \"walletProvider\": \"SARAWAK_PAY\",
  \"walletAccountId\": \"SP123456789\",
  \"amount\": 75.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\"
}")
assert_status "Card top-up" "200" "$(get_status "$response")"
assert_json_field "Card top-up status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-ESSP-01: eSSP Purchase Tests
# ============================================================================

subsection "BDD-ESSP-01: eSSP purchase"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/essp/purchase" "$AGENT_TOKEN" "{
  \"schemeType\": \"SKIM_SIMPANAN_PENDIDIKAN\",
  \"beneficiaryMykad\": \"$(random_mykad)\",
  \"beneficiaryName\": \"TEST BENEFICIARY\",
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "eSSP purchase" "200" "$(get_status "$response")"
assert_json_field "eSSP status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"
assert_json_field "Scheme type" "$(get_body "$response")" ".schemeType" "SKIM_SIMPANAN_PENDIDIKAN"

subsection "BDD-ESSP-01-EC-01: BSN unavailable"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/essp/purchase" "$AGENT_TOKEN" "{
  \"schemeType\": \"SKIM_SIMPANAN_PENDIDIKAN\",
  \"beneficiaryMykad\": \"$(random_mykad)\",
  \"beneficiaryName\": \"TEST BENEFICIARY\",
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "503" ]; then
    assert_json_field "BSN unavailable error" "$(get_body "$response")" ".error.code" "ERR_EXT_BSN_UNAVAILABLE"
else
    assert_status "BSN unavailable" "503" "$actual_status"
fi

subsection "BDD-ESSP-01-CARD: Card eSSP"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/essp/purchase" "$AGENT_TOKEN" "{
  \"schemeType\": \"SKIM_SIMPANAN_PENDIDIKAN\",
  \"beneficiaryMykad\": \"$(random_mykad)\",
  \"beneficiaryName\": \"TEST BENEFICIARY\",
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\"
}")
assert_status "Card eSSP" "200" "$(get_status "$response")"
assert_json_field "Card eSSP status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

print_summary "eWallet & eSSP Tests"
