#!/bin/bash
#
# BDD Section 10: Agent Onboarding E2E Tests
# Tests Micro-Agent self-onboarding and Standard/Premier maker-checker flow

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 10: Agent Onboarding"

# ============================================================================
# BDD-A01: Micro-Agent Self-Onboarding Tests
# ============================================================================

subsection "BDD-A01: Micro-Agent self-onboarding"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/onboarding/submit-application" "$ADMIN_TOKEN" "{
  \"agentTier\": \"MICRO\",
  \"mykadNumber\": \"880112125478\",
  \"name\": \"SITI BINTI AHMAD\",
  \"dateOfBirth\": \"1988-01-12\",
  \"address\": \"JALAN AMPANG 123, KUALA LUMPUR\",
  \"phoneNumber\": \"60123456789\",
  \"email\": \"siti.ahmad@example.com\",
  \"gpsLatitude\": 3.1390,
  \"gpsLongitude\": 101.6869,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Micro-Agent onboarding" "200" "$(get_status "$response")"
assert_json_field "Application status" "$(get_body "$response")" ".status" "SUBMITTED"
assert_json_field_exists "Application ID" "$(get_body "$response")" ".applicationId"
assert_json_field "Agent tier" "$(get_body "$response")" ".agentTier" "MICRO"

subsection "BDD-A01-EC-01: OCR name mismatch"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/onboarding/submit-application" "$ADMIN_TOKEN" "{
  \"agentTier\": \"MICRO\",
  \"mykadNumber\": \"880112125478\",
  \"name\": \"WRONG NAME MISMATCH\",
  \"dateOfBirth\": \"1988-01-12\",
  \"address\": \"JALAN AMPANG 123, KUALA LUMPUR\",
  \"phoneNumber\": \"60123456789\",
  \"email\": \"wrong.name@example.com\",
  \"gpsLatitude\": 3.1390,
  \"gpsLongitude\": 101.6869,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "OCR name mismatch" "400" "$(get_status "$response")"
assert_json_field "Error for OCR mismatch" "$(get_body "$response")" ".error.code" "ERR_VAL_OCR_NAME_MISMATCH"

subsection "BDD-A01-EC-02: AML hit"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/onboarding/submit-application" "$ADMIN_TOKEN" "{
  \"agentTier\": \"MICRO\",
  \"mykadNumber\": \"000000000001\",
  \"name\": \"AML SANCTIONED PERSON\",
  \"dateOfBirth\": \"1970-01-01\",
  \"address\": \"UNKNOWN\",
  \"phoneNumber\": \"60199999999\",
  \"email\": \"aml.hit@example.com\",
  \"gpsLatitude\": 3.1390,
  \"gpsLongitude\": 101.6869,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "AML hit rejection" "403" "$(get_status "$response")"
assert_json_field "Error for AML hit" "$(get_body "$response")" ".error.code" "ERR_BIZ_AML_SANCTIONED"

subsection "BDD-A01-EC-03: High-risk GPS zone"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/onboarding/submit-application" "$ADMIN_TOKEN" "{
  \"agentTier\": \"MICRO\",
  \"mykadNumber\": \"880112125478\",
  \"name\": \"SITI BINTI AHMAD\",
  \"dateOfBirth\": \"1988-01-12\",
  \"address\": \"HIGH RISK ZONE\",
  \"phoneNumber\": \"60123456789\",
  \"email\": \"highrisk@example.com\",
  \"gpsLatitude\": 1.0000,
  \"gpsLongitude\": 100.0000,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "High-risk GPS zone" "403" "$(get_status "$response")"
assert_json_field "Error for high-risk zone" "$(get_body "$response")" ".error.code" "ERR_BIZ_HIGH_RISK_ZONE"

# ============================================================================
# BDD-A02: Standard/Premier Onboarding (Maker-Checker) Tests
# ============================================================================

subsection "BDD-A02: Standard/Premier onboarding (Maker-Checker)"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/onboarding/submit-application" "$MAKER_TOKEN" "{
  \"agentTier\": \"STANDARD\",
  \"mykadNumber\": \"900101123456\",
  \"name\": \"ALI BIN HASSAN\",
  \"dateOfBirth\": \"1990-01-01\",
  \"address\": \"JALAN SULTAN ISMAIL 88, KUALA LUMPUR\",
  \"phoneNumber\": \"60187654321\",
  \"email\": \"ali.hassan@example.com\",
  \"businessName\": \"ALI MINI MART SDN BHD\",
  \"businessRegistrationNumber\": \"201901234567\",
  \"gpsLatitude\": 3.1390,
  \"gpsLongitude\": 101.6869,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Standard onboarding submission" "200" "$(get_status "$response")"
assert_json_field "Application pending review" "$(get_body "$response")" ".status" "PENDING_CHECKER_REVIEW"
assert_json_field_exists "Application ID" "$(get_body "$response")" ".applicationId"

subsection "BDD-A02-EC-01: Self-approval prohibited"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/onboarding/submit-application" "$MAKER_TOKEN" "{
  \"agentTier\": \"STANDARD\",
  \"mykadNumber\": \"900101123456\",
  \"name\": \"ALI BIN HASSAN\",
  \"dateOfBirth\": \"1990-01-01\",
  \"address\": \"JALAN SULTAN ISMAIL 88, KUALA LUMPUR\",
  \"phoneNumber\": \"60187654321\",
  \"email\": \"ali.hassan@example.com\",
  \"businessName\": \"ALI MINI MART SDN BHD\",
  \"businessRegistrationNumber\": \"201901234567\",
  \"gpsLatitude\": 3.1390,
  \"gpsLongitude\": 101.6869,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Self-approval submission" "200" "$(get_status "$response")"
APP_ID=$(echo "$(get_body "$response")" | jq -r '.applicationId // empty')
if [ -n "$APP_ID" ]; then
    review_response=$(api_call "POST" "/api/v1/backoffice/kyc/review-queue" "$MAKER_TOKEN" "{
      \"applicationId\": \"$APP_ID\",
      \"action\": \"APPROVE\",
      \"remarks\": \"Self-approval attempt\"
    }")
    assert_status "Self-approval prohibited" "403" "$(get_status "$review_response")"
    assert_json_field "Error for self-approval" "$(get_body "$review_response")" ".error.code" "ERR_BIZ_SELF_APPROVAL_PROHIBITED"
else
    TOTAL=$((TOTAL + 1))
    echo -e "  ${RED}✗${NC} Self-approval test skipped (no application ID)"
    FAILED=$((FAILED + 1))
fi

subsection "BDD-A02-EC-02: Checker rejects"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/onboarding/submit-application" "$MAKER_TOKEN" "{
  \"agentTier\": \"STANDARD\",
  \"mykadNumber\": \"910202234567\",
  \"name\": \"REJECT TEST\",
  \"dateOfBirth\": \"1991-02-02\",
  \"address\": \"JALAN REJECT 1, KUALA LUMPUR\",
  \"phoneNumber\": \"60176543210\",
  \"email\": \"reject.test@example.com\",
  \"businessName\": \"REJECT BUSINESS\",
  \"businessRegistrationNumber\": \"202001234567\",
  \"gpsLatitude\": 3.1390,
  \"gpsLongitude\": 101.6869,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Reject test submission" "200" "$(get_status "$response")"
APP_ID=$(echo "$(get_body "$response")" | jq -r '.applicationId // empty')
if [ -n "$APP_ID" ]; then
    review_response=$(api_call "POST" "/api/v1/backoffice/kyc/review-queue" "$CHECKER_TOKEN" "{
      \"applicationId\": \"$APP_ID\",
      \"action\": \"REJECT\",
      \"remarks\": \"Documents insufficient\"
    }")
    assert_status "Checker rejection" "200" "$(get_status "$review_response")"
    assert_json_field "Application rejected" "$(get_body "$review_response")" ".status" "REJECTED"
else
    TOTAL=$((TOTAL + 1))
    echo -e "  ${RED}✗${NC} Checker rejection test skipped (no application ID)"
    FAILED=$((FAILED + 1))
fi

print_summary "Agent Onboarding Tests"
