#!/bin/bash
#
# BDD Section 13: AML / Fraud / Velocity E2E Tests
# Tests smurfing detection, geofence violations, velocity limits, and STR filing

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 13: AML / Fraud / Velocity"

# ============================================================================
# BDD-EFM01: Smurfing Detection
# ============================================================================

subsection "BDD-EFM01: Smurfing detection"
for i in 1 2 3 4 5; do
    IDEMPOTENCY_KEY=$(generate_uuid)
    api_call "POST" "/api/v1/deposit" "$AGENT_TOKEN" "{
      \"amount\": 9500.00,
      \"currency\": \"MYR\",
      \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
      \"customerCard\": \"4111111111111111\",
      \"customerMykad\": \"900101101234\",
      \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
    }" > /dev/null 2>&1
done

IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$AGENT_TOKEN" "{
  \"amount\": 9500.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerMykad\": \"900101101234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "403" ]; then
    assert_json_field "Smurfing alert" "$(get_body "$response")" ".error.code" "ERR_BIZ_SMURFING_DETECTED"
    assert_json_field "Action code" "$(get_body "$response")" ".error.action_code" "DECLINE"
else
    assert_status "Smurfing detection" "403" "$actual_status"
fi

# ============================================================================
# BDD-EFM01-EC-01: Normal High-Volume Agent
# ============================================================================

subsection "BDD-EFM01-EC-01: Normal high-volume agent"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/deposit" "$HIGH_VOLUME_AGENT_TOKEN" "{
  \"amount\": 15000.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerMykad\": \"880101105678\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "Normal high-volume deposit" "200" "$(get_status "$response")"
assert_json_field "Deposit status" "$(get_body "$response")" ".status" "SUCCESS"

# ============================================================================
# BDD-EFM02: Compliance Officer Unfreezes
# ============================================================================

subsection "BDD-EFM02: Compliance officer unfreezes"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/agents/AGENT_001/unfreeze" "$COMPLIANCE_TOKEN" "{
  \"agentId\": \"AGENT_001\",
  \"action\": \"UNFREEZE\",
  \"reason\": \"Reviewed and cleared - legitimate business activity\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Unfreeze agent" "200" "$(get_status "$response")"
assert_json_field "Unfreeze status" "$(get_body "$response")" ".status" "UNFROZEN"

# ============================================================================
# BDD-EFM02-EC-01: Confirms Smurfing and Files STR
# ============================================================================

subsection "BDD-EFM02-EC-01: Confirms smurfing and files STR"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/agents/AGENT_SMURF/freeze" "$COMPLIANCE_TOKEN" "{
  \"agentId\": \"AGENT_SMURF\",
  \"action\": \"CONFIRM_SMURFING\",
  \"reason\": \"Confirmed smurfing pattern - filing STR\",
  \"fileStr\": true,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Confirm smurfing" "200" "$(get_status "$response")"
assert_json_field "Smurfing confirmed" "$(get_body "$response")" ".status" "FROZEN"
assert_json_field_exists "STR reference" "$(get_body "$response")" ".strReference"

# ============================================================================
# BDD-EFM03: Geofence Violation
# ============================================================================

subsection "BDD-EFM03: Geofence violation"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 100.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 1.3521, \"longitude\": 103.8198}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "403" ]; then
    assert_json_field "Geofence violation" "$(get_body "$response")" ".error.code" "ERR_BIZ_GEOFENCE_VIOLATION"
else
    assert_status "Geofence violation" "403" "$actual_status"
fi

# ============================================================================
# BDD-EFM04: Per-NRIC Velocity Limit
# ============================================================================

subsection "BDD-EFM04: Per-NRIC velocity limit"
for i in 1 2 3 4; do
    IDEMPOTENCY_KEY=$(generate_uuid)
    api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
      \"amount\": 500.00,
      \"currency\": \"MYR\",
      \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
      \"customerCard\": \"4111111111111111\",
      \"customerMykad\": \"900505104444\",
      \"customerPin\": \"1234\",
      \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
    }" > /dev/null 2>&1
done

IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 500.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerMykad\": \"900505104444\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "403" ]; then
    assert_json_field "NRIC velocity limit" "$(get_body "$response")" ".error.code" "ERR_BIZ_VELOCITY_NRIC_COUNT"
else
    assert_status "Per-NRIC velocity limit" "403" "$actual_status"
fi

# ============================================================================
# BDD-EFM04-EC-01: Per-NRIC Amount Limit
# ============================================================================

subsection "BDD-EFM04-EC-01: Per-NRIC amount limit"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 25000.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerMykad\": \"900505105555\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "403" ]; then
    assert_json_field "NRIC amount limit" "$(get_body "$response")" ".error.code" "ERR_BIZ_VELOCITY_NRIC_AMOUNT"
else
    assert_status "Per-NRIC amount limit" "403" "$actual_status"
fi

print_summary "AML / Fraud / Velocity Tests"
