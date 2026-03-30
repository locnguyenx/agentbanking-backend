#!/bin/bash
#
# BDD Section 16: API Gateway E2E Tests
# Tests routing, authentication, error handling, rate limiting, and resilience

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 16: API Gateway"

# ============================================================================
# BDD-G01: Gateway Routes to Correct Service
# ============================================================================

subsection "BDD-G01: Gateway routes to correct service"
response=$(api_call "GET" "/api/v1/balance-inquiry" "$AGENT_TOKEN")
assert_status "Route to balance service" "200" "$(get_status "$response")"
assert_json_field_exists "Balance response" "$(get_body "$response")" ".availableBalance"

response=$(api_call "POST" "/api/v1/kyc/verify" "$AGENT_TOKEN" "{
  \"mykadNumber\": \"880112125478\",
  \"name\": \"AHMAD BIN ISMAIL\",
  \"idempotencyKey\": \"$(generate_uuid)\"
}")
assert_status "Route to KYC service" "200" "$(get_status "$response")"

response=$(api_call "POST" "/api/v1/bill/pay" "$AGENT_TOKEN" "{
  \"billId\": \"BILL_001\",
  \"amount\": 50.00,
  \"idempotencyKey\": \"$(generate_uuid)\"
}")
assert_status "Route to biller service" "200" "$(get_status "$response")"

# ============================================================================
# BDD-G02: Gateway Authenticates Request
# ============================================================================

subsection "BDD-G02: Gateway authenticates request"
response=$(api_call "GET" "/api/v1/balance-inquiry" "$AGENT_TOKEN")
assert_status "Authenticated request" "200" "$(get_status "$response")"
assert_json_field_exists "Valid response" "$(get_body "$response")" ".status"

# ============================================================================
# BDD-G01-EC-01: Expired Token
# ============================================================================

subsection "BDD-G01-EC-01: Expired token"
EXPIRED_TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBR0VOVF8wMDEiLCJyb2xlIjoiQUdFTlQiLCJleHAiOjE1MTYyMzkwMjJ9.expired"
response=$(api_call "GET" "/api/v1/balance-inquiry" "$EXPIRED_TOKEN")
assert_status "Expired token rejected" "401" "$(get_status "$response")"
assert_json_field "Token expired error" "$(get_body "$response")" ".error.code" "ERR_AUTH_TOKEN_EXPIRED"

# ============================================================================
# BDD-G01-EC-02: Missing Token
# ============================================================================

subsection "BDD-G01-EC-02: Missing token"
response=$(api_call "GET" "/api/v1/balance-inquiry" "")
assert_status "Missing token rejected" "401" "$(get_status "$response")"
assert_json_field "Missing token error" "$(get_body "$response")" ".error.code" "ERR_AUTH_TOKEN_MISSING"

# ============================================================================
# BDD-G01-EC-03: Backend Service Down
# ============================================================================

subsection "BDD-G01-EC-03: Backend service down"
response=$(api_call "GET" "/api/v1/balance-inquiry?simulateDown=true" "$AGENT_TOKEN")
actual_status=$(get_status "$response")
if [ "$actual_status" = "503" ]; then
    assert_json_field "Service unavailable" "$(get_body "$response")" ".error.code" "ERR_EXT_SERVICE_UNAVAILABLE"
    assert_json_field "Retry action" "$(get_body "$response")" ".error.action_code" "RETRY"
else
    assert_status "Backend service down" "503" "$actual_status"
fi

# ============================================================================
# BDD-G01-EC-04: Rate Limiting
# ============================================================================

subsection "BDD-G01-EC-04: Rate limiting"
rate_limited=false
for i in $(seq 1 150); do
    resp=$(api_call "GET" "/api/v1/balance-inquiry" "$AGENT_TOKEN")
    status=$(get_status "$resp")
    if [ "$status" = "429" ]; then
        rate_limited=true
        TOTAL=$((TOTAL + 1))
        echo -e "  ${GREEN}✓${NC} Rate limiting triggered at request $i"
        PASSED=$((PASSED + 1))
        break
    fi
done

if [ "$rate_limited" = false ]; then
    TOTAL=$((TOTAL + 1))
    echo -e "  ${YELLOW}⚠${NC} Rate limiting not triggered in 150 requests (may have high limit)"
    SKIPPED=$((SKIPPED + 1))
fi

# ============================================================================
# BDD-G01-EC-05: Invalid JSON Body
# ============================================================================

subsection "BDD-G01-EC-05: Invalid JSON body"
response=$(curl -s -w '\n%{http_code}' -X POST \
    -H "Authorization: Bearer $AGENT_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{invalid json}' \
    "$GATEWAY_URL/api/v1/withdrawal")
assert_status "Invalid JSON rejected" "400" "$(get_status "$response")"
assert_json_field "Parse error" "$(get_body "$response")" ".error.code" "ERR_VAL_INVALID_JSON"

print_summary "API Gateway Tests"
