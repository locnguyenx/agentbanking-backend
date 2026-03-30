#!/bin/bash
#
# BDD Section 12: Merchant Services E2E Tests
# Tests retail sales, PIN purchases, DuitNow QR, RTP, and cash-back

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 12: Merchant Services"

# ============================================================================
# BDD-M01: Retail Sale via Debit Card Tests
# ============================================================================

subsection "BDD-M01: Retail sale via debit card"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/sale" "$AGENT_TOKEN" "{
  \"amount\": 45.50,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\",
  \"items\": [{\"sku\": \"ITEM001\", \"description\": \"Test Item\", \"quantity\": 1, \"unitPrice\": 45.50}]
}")
assert_status "Retail sale" "200" "$(get_status "$response")"
assert_json_field "Sale status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"
assert_json_field "Sale amount" "$(get_body "$response")" ".amount" "45.50"

subsection "BDD-M01-EC-01: Card declined"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/sale" "$AGENT_TOKEN" "{
  \"amount\": 10.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4000000000000000\",
  \"customerPin\": \"1234\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\"
}")
assert_status "Card declined" "402" "$(get_status "$response")"
assert_json_field "Error for card declined" "$(get_body "$response")" ".error.code" "ERR_BIZ_CARD_DECLINED"

subsection "BDD-M01-EC-02: Daily limit exceeded"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/sale" "$AGENT_TOKEN" "{
  \"amount\": 5000.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\"
}")
assert_status "Daily limit exceeded" "403" "$(get_status "$response")"
assert_json_field "Error for daily limit" "$(get_body "$response")" ".error.code" "ERR_BIZ_DAILY_LIMIT_EXCEEDED"

subsection "BDD-M01-QR: DuitNow QR"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/sale" "$AGENT_TOKEN" "{
  \"amount\": 25.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"paymentMethod\": \"DUITNOW_QR\",
  \"qrPayload\": \"00020101021226580014A000000615000101065802MY5914TEST%20MERCHANT6002KL\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\"
}")
assert_status "DuitNow QR sale" "200" "$(get_status "$response")"
assert_json_field "QR sale status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

subsection "BDD-M01-RTP: Request to Pay"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/sale" "$AGENT_TOKEN" "{
  \"amount\": 35.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"paymentMethod\": \"REQUEST_TO_PAY\",
  \"payerMobile\": \"60123456789\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\"
}")
assert_status "Request to Pay" "200" "$(get_status "$response")"
assert_json_field "RTP status" "$(get_body "$response")" ".status" "PENDING_CONFIRMATION"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-M02: PIN Purchase Tests
# ============================================================================

subsection "BDD-M02: PIN purchase"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/pin-purchase" "$AGENT_TOKEN" "{
  \"amount\": 80.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\",
  \"items\": [{\"sku\": \"PIN001\", \"description\": \"Prepaid PIN\", \"quantity\": 1, \"unitPrice\": 80.00}]
}")
assert_status "PIN purchase" "200" "$(get_status "$response")"
assert_json_field "PIN purchase status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

subsection "BDD-M02-EC-01: Inventory depleted"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/pin-purchase" "$AGENT_TOKEN" "{
  \"amount\": 80.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"merchantId\": \"MERCH_DEPLETED\",
  \"terminalId\": \"TERM_001\"
}")
assert_status "Inventory depleted" "409" "$(get_status "$response")"
assert_json_field "Error for depleted inventory" "$(get_body "$response")" ".error.code" "ERR_BIZ_INVENTORY_DEPLETED"

subsection "BDD-M02-CARD: Card PIN purchase"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/pin-purchase" "$AGENT_TOKEN" "{
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"5500000000000004\",
  \"customerPin\": \"4321\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\"
}")
assert_status "Card PIN purchase" "200" "$(get_status "$response")"
assert_json_field "Card PIN purchase status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-M03: Cash-Back Transaction Tests
# ============================================================================

subsection "BDD-M03: Cash-back transaction"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/cashback" "$AGENT_TOKEN" "{
  \"purchaseAmount\": 60.00,
  \"cashbackAmount\": 20.00,
  \"totalAmount\": 80.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\"
}")
assert_status "Cash-back transaction" "200" "$(get_status "$response")"
assert_json_field "Cash-back status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"
assert_json_field "Cash-back amount" "$(get_body "$response")" ".cashbackAmount" "20.00"

subsection "BDD-M03-EC-01: Insufficient float for cash"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/cashback" "$AGENT_TOKEN" "{
  \"purchaseAmount\": 60.00,
  \"cashbackAmount\": 500.00,
  \"totalAmount\": 560.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\"
}")
assert_status "Insufficient float for cash-back" "402" "$(get_status "$response")"
assert_json_field "Error for insufficient float" "$(get_body "$response")" ".error.code" "ERR_BIZ_INSUFFICIENT_FLOAT"

subsection "BDD-M03-EC-02: Cash-back exceeds purchase amount"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/retail/cashback" "$AGENT_TOKEN" "{
  \"purchaseAmount\": 20.00,
  \"cashbackAmount\": 100.00,
  \"totalAmount\": 120.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"merchantId\": \"MERCH_001\",
  \"terminalId\": \"TERM_001\"
}")
assert_status "Cash-back exceeds purchase" "400" "$(get_status "$response")"
assert_json_field "Error for cash-back ratio" "$(get_body "$response")" ".error.code" "ERR_VAL_CASHBACK_EXCEEDS_PURCHASE"

print_summary "Merchant Services Tests"
