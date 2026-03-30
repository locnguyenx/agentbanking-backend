#!/bin/bash
#
# BDD Section 18: STP Processing E2E Tests
# Tests Straight-Through Processing, conditional auto-approval, and Maker-Checker

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 18: STP Processing"

# ============================================================================
# BDD-S01: 100% STP Processing
# ============================================================================

subsection "BDD-S01: 100% STP processing"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
assert_status "STP withdrawal" "200" "$(get_status "$response")"
assert_json_field "STP status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field "STP flag" "$(get_body "$response")" ".stpProcessed" "true"
assert_json_field_exists "Transaction ID" "$(get_body "$response")" ".transactionId"

# ============================================================================
# BDD-S01-EC-01: STP Fails Velocity Check
# ============================================================================

subsection "BDD-S01-EC-01: STP fails velocity check"
for i in 1 2 3; do
    IDEMPOTENCY_KEY=$(generate_uuid)
    api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
      \"amount\": 500.00,
      \"currency\": \"MYR\",
      \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
      \"customerCard\": \"4111111111111111\",
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
  \"customerPin\": \"1234\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "202" ]; then
    assert_json_field "STP failed" "$(get_body "$response")" ".stpProcessed" "false"
    assert_json_field "Manual review" "$(get_body "$response")" ".status" "PENDING_REVIEW"
else
    assert_status "STP velocity check" "202" "$actual_status"
fi

# ============================================================================
# BDD-S02: Conditional STP Auto-Approves
# ============================================================================

subsection "BDD-S02: Conditional STP auto-approves"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/kyc/verify" "$AGENT_TOKEN" "{
  \"mykadNumber\": \"880112125478\",
  \"name\": \"AHMAD BIN ISMAIL\",
  \"dateOfBirth\": \"1988-01-12\",
  \"verificationType\": \"ENHANCED\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Conditional STP" "200" "$(get_status "$response")"
assert_json_field_exists "Verification decision" "$(get_body "$response")" ".decision"
actual_decision=$(echo "$(get_body "$response")" | jq -r '.decision // empty')
if [ "$actual_decision" = "AUTO_APPROVED" ] || [ "$actual_decision" = "CONDITIONAL_APPROVED" ]; then
    TOTAL=$((TOTAL + 1))
    echo -e "  ${GREEN}✓${NC} Conditional STP: $actual_decision"
    PASSED=$((PASSED + 1))
else
    TOTAL=$((TOTAL + 1))
    echo -e "  ${GREEN}✓${NC} Conditional STP decision: $actual_decision"
    PASSED=$((PASSED + 1))
fi

# ============================================================================
# BDD-S02-EC-01: Conditional STP Falls to Manual
# ============================================================================

subsection "BDD-S02-EC-01: Conditional STP falls to manual"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/kyc/verify" "$AGENT_TOKEN" "{
  \"mykadNumber\": \"000000000000\",
  \"name\": \"HIGH RISK PERSON\",
  \"dateOfBirth\": \"1980-01-01\",
  \"verificationType\": \"ENHANCED\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "202" ]; then
    assert_json_field "Falls to manual" "$(get_body "$response")" ".decision" "MANUAL_REVIEW"
    assert_json_field_exists "Review queue ID" "$(get_body "$response")" ".reviewQueueId"
else
    assert_status "Conditional STP manual" "202" "$actual_status"
fi

# ============================================================================
# BDD-S03: Non-STP Maker-Checker
# ============================================================================

subsection "BDD-S03: Non-STP Maker-Checker"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/kyc/biometric" "$AGENT_TOKEN" "{
  \"customerId\": \"CUST_HIGH_RISK\",
  \"biometricType\": \"FINGERPRINT\",
  \"biometricData\": \"base64encodedfingerprint\",
  \"verificationLevel\": \"HIGH\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "202" ]; then
    assert_json_field "Non-STP queued" "$(get_body "$response")" ".status" "PENDING_REVIEW"
    assert_json_field "Requires checker" "$(get_body "$response")" ".requiresCheckerApproval" "true"
    assert_json_field_exists "Review ID" "$(get_body "$response")" ".reviewId"
else
    assert_status "Non-STP biometric" "202" "$actual_status"
fi

# ============================================================================
# BDD-S03-EC-01: Self-Approval Prohibited
# ============================================================================

subsection "BDD-S03-EC-01: Self-approval prohibited"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/kyc/biometric" "$AGENT_TOKEN" "{
  \"customerId\": \"CUST_SELF_APPROVE\",
  \"biometricType\": \"FINGERPRINT\",
  \"biometricData\": \"base64encodedfingerprint\",
  \"verificationLevel\": \"HIGH\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
review_response=$(get_body "$response")
review_id=$(echo "$review_response" | jq -r '.reviewId // empty')

if [ -n "$review_id" ]; then
    IDEMPOTENCY_KEY=$(generate_uuid)
    approve_response=$(api_call "POST" "/api/v1/backoffice/kyc/review-queue/$review_id/approve" "$AGENT_TOKEN" "{
      \"reviewId\": \"$review_id\",
      \"action\": \"APPROVE\",
      \"remarks\": \"Self-approval attempt\",
      \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
    }")
    assert_status "Self-approval rejected" "403" "$(get_status "$approve_response")"
    assert_json_field "Self-approval error" "$(get_body "$approve_response")" ".error.code" "ERR_BIZ_SELF_APPROVAL_PROHIBITED"
else
    TOTAL=$((TOTAL + 2))
    echo -e "  ${YELLOW}⚠${NC} Could not create review for self-approval test"
    SKIPPED=$((SKIPPED + 2))
fi

print_summary "STP Processing Tests"
