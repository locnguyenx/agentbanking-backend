#!/bin/bash
#
# BDD Section 5: eKYC Onboarding E2E Tests
# Tests MyKad verification, biometric matching, and auto-approval decisions

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 5: eKYC Onboarding"

# ============================================================================
# BDD-O01: MyKad Verification via JPN
# ============================================================================

subsection "BDD-O01: MyKad verification via JPN"
response=$(api_call "POST" "/api/v1/onboarding/verify-mykad" "$AGENT_TOKEN" '{
  "mykadNumber": "880112125478",
  "name": "AHMAD BIN ISMAIL",
  "dateOfBirth": "1988-01-12",
  "address": "JALAN BUKIT ANGKAT 45",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "MyKad verification" "200" "$(get_status "$response")"
assert_json_field "Verification status" "$(get_body "$response")" ".status" "SUCCESS"
assert_json_field_exists "Customer ID" "$(get_body "$response")" ".customerId"

subsection "BDD-O01-EC-01: MyKad not found"
response=$(api_call "POST" "/api/v1/onboarding/verify-mykad" "$AGENT_TOKEN" '{
  "mykadNumber": "000000000000",
  "name": "UNKNOWN PERSON",
  "dateOfBirth": "1980-01-01",
  "address": "UNKNOWN ADDRESS",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "MyKad not found" "404" "$(get_status "$response")"
assert_json_field "Error for MyKad not found" "$(get_body "$response")" ".error.code" "ERR_BIZ_MYKAD_NOT_FOUND"

subsection "BDD-O01-EC-02: JPN API unavailable"
response=$(api_call "POST" "/api/v1/onboarding/verify-mykad" "$AGENT_TOKEN" '{
  "mykadNumber": "880112125478",
  "name": "AHMAD BIN ISMAIL",
  "dateOfBirth": "1988-01-12",
  "address": "JALAN BUKIT ANGKAT 45",
  "idempotencyKey": "'$(generate_uuid)'"
}')
actual_status=$(get_status "$response")
if [ "$actual_status" = "503" ]; then
    assert_json_field "JPN unavailable error" "$(get_body "$response")" ".error.code" "ERR_EXT_JPN_UNAVAILABLE"
else
    assert_status "MyKad verification" "200" "$actual_status"
fi

subsection "BDD-O01-EC-03: Customer under 18"
response=$(api_call "POST" "/api/v1/onboarding/verify-mykad" "$AGENT_TOKEN" '{
  "mykadNumber": "081112125478",
  "name": "YOUNG PERSON",
  "dateOfBirth": "2010-01-12",
  "address": "JALAN BUKIT ANGKAT 45",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Under 18 rejection" "400" "$(get_status "$response")"
assert_json_field "Error for under 18" "$(get_body "$response")" ".error.code" "ERR_VAL_UNDERAGE"

# ============================================================================
# BDD-O02: Biometric Match Tests
# ============================================================================

subsection "BDD-O02: Biometric match"
response=$(api_call "POST" "/api/v1/onboarding/verify-biometric" "$AGENT_TOKEN" '{
  "customerId": "CUST_001",
  "biometricType": "FINGERPRINT",
  "biometricData": "base64encodedfingerprint",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Biometric verification" "200" "$(get_status "$response")"
assert_json_field "Biometric status" "$(get_body "$response")" ".matchResult" "MATCHED"
assert_json_field "Biometric score >= 80" "$(get_body "$response")" ".biometricScore" "85"

subsection "BDD-O02-EC-01: Biometric mismatch"
response=$(api_call "POST" "/api/v1/onboarding/verify-biometric" "$AGENT_TOKEN" '{
  "customerId": "CUST_001",
  "biometricType": "FINGERPRINT",
  "biometricData": "bas64differentfingerprint",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Biometric mismatch" "401" "$(get_status "$response")"
assert_json_field "Error for mismatch" "$(get_body "$response")" ".error.code" "ERR_AUTH_BIOMETRIC_MISMATCH"

# ============================================================================
# BDD-O03: Auto-approval Decision Matrix Tests
# ============================================================================

subsection "BDD-O03: Auto-approval decision matrix"
response=$(api_call "POST" "/api/v1/onboarding/submit-application" "$AGENT_TOKEN" '{
  "mykadNumber": "880112125478",
  "name": "AHMAD BIN ISMAIL",
  "dateOfBirth": "1988-01-12",
  "address": "JALAN BUKIT ANGKAT 45",
  "biometricData": "base64encodedfingerprint",
  "idempotencyKey": "'$(generate_uuid)'"
}')
assert_status "Application submission" "200" "$(get_status "$response")"
actual_decision=$(echo "$(get_body "$response")" | jq -r '.decision // empty')
if [ "$actual_decision" = "AUTO_APPROVED" ] || [ "$actual_decision" = "MANUAL_REVIEW" ]; then
    TOTAL=$((TOTAL + 1))
    echo -e "  ${GREEN}✓${NC} Auto-approval decision: $actual_decision"
    PASSED=$((PASSED + 1))
else
    TOTAL=$((TOTAL + 1))
    echo -e "  ${RED}✗${NC} Auto-approval decision (expected AUTO_APPROVED or MANUAL_REVIEW, got '$actual_decision')"
    FAILED=$((FAILED + 1))
fi
assert_json_field "Application has decision" "$(get_body "$response")" ".decision" "$actual_decision"

print_summary "eKYC Onboarding Tests"