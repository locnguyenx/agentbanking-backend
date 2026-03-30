#!/bin/bash
#
# BDD Section 3: Cash Withdrawal E2E Tests
# Tests ATM card and MyKad-based cash withdrawal scenarios

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 3: Cash Withdrawal"

# ============================================================================
# BDD-W01: ATM Card Withdrawal Tests
# ============================================================================

subsection "BDD-W01: Successful ATM card withdrawal"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "Successful withdrawal" "200" "$(get_status "$response")"
assert_json_field "Withdrawal status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

subsection "BDD-W01-EC-01: Invalid card PIN"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"9999\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "Invalid PIN rejection" "401" "$(get_status "$response")"
assert_json_field "Error for invalid PIN" "$(get_body "$response")" ".error.code" "ERR_AUTH_INVALID_PIN"

subsection "BDD-W01-EC-04: Withdrawal exceeds daily limit"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 10000.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "Daily limit exceeded" "403" "$(get_status "$response")"
assert_json_field "Error for limit exceeded" "$(get_body "$response")" ".error.code" "ERR_BIZ_DAILY_LIMIT_EXCEEDED"

subsection "BDD-W01-EC-05: Outside geofence"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 1.3521, \"longitude\": 103.8198}
}")
assert_status "Geofence violation" "403" "$(get_status "$response")"
assert_json_field "Error for geofence" "$(get_body "$response")" ".error.code" "ERR_BIZ_OUTSIDE_GEOFENCE"

subsection "BDD-W01-EC-06: GPS unavailable"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\"
}")
assert_status "GPS missing" "400" "$(get_status "$response")"
assert_json_field "Error for GPS unavailable" "$(get_body "$response")" ".error.code" "ERR_VAL_GPS_UNAVAILABLE"

subsection "BDD-W01-EC-07: Duplicate idempotency key"
idempotency_key=$(generate_uuid)
api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$idempotency_key\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}" > /dev/null
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$idempotency_key\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "Duplicate idempotency" "200" "$(get_status "$response")"
assert_json_field "Cached response" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field "Idempotent replayed" "$(get_body "$response")" ".idempotentReplayed" "true"

# ============================================================================
# BDD-W02: MyKad-based Withdrawal Tests
# ============================================================================

subsection "BDD-W02: MyKad-based withdrawal"
IDEMPOTENCY_KEY=$(generate_uuid)
mykad_number=$(random_mykad)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 200.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"mykadNumber\": \"$mykad_number\",
  \"biometricData\": \"base64encodedbiometrics\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "200" ] || [ "$actual_status" = "201" ]; then
    assert_json_field "MyKad withdrawal status" "$(get_body "$response")" ".status" "SUCCESS"
else
    assert_status "MyKad withdrawal" "200" "$actual_status"
fi

print_summary "Cash Withdrawal Tests"