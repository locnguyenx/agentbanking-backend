#!/bin/bash

# Agent Banking Platform - Integration Test Suite
# Runs against mock server and tests key flows

set -e

BASE_URL="${BASE_URL:-http://localhost:8090}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
AUTH_TOKEN="${AUTH_TOKEN:-test-jwt-token}"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

PASS=0
FAIL=0

assert_status() {
    local test_name="$1"
    local actual="$2"
    local expected="$3"
    
    if [ "$actual" = "$expected" ]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
        ((PASS++))
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name (expected $expected, got $actual)"
        ((FAIL++))
    fi
}

assert_contains() {
    local test_name="$1"
    local response="$2"
    local expected="$3"
    
    if echo "$response" | grep -q "$expected"; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
        ((PASS++))
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name (expected to contain '$expected')"
        echo "  Response: $response"
        ((FAIL++))
    fi
}

echo "=========================================="
echo "Agent Banking Platform - Integration Tests"
echo "=========================================="
echo ""

# ==================== Mock Server Tests ====================
echo "--- Mock Server Tests ---"

# Test JPN verify (BDD-O01)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/jpn/verify" \
    -H "Content-Type: application/json" \
    -d '{"mykad":"123456789012"}')
assert_contains "JPN: verify MyKad returns found" "$RESPONSE" "FOUND"
assert_contains "JPN: verify returns fullName" "$RESPONSE" "AHMAD BIN ABU"
assert_contains "JPN: verify returns amlStatus CLEAN" "$RESPONSE" "CLEAN"

# Test JPN verify - not found (BDD-O01-EC-01)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/jpn/verify" \
    -H "Content-Type: application/json" \
    -d '{"mykad":"000000000000"}')
assert_contains "JPN: verify MyKad not found" "$RESPONSE" "NOT_FOUND"

# Test biometric match (BDD-O02)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/jpn/biometric" \
    -H "Content-Type: application/json" \
    -d '{"verificationId":"KYC-001","biometricData":"blob"}')
assert_contains "JPN: biometric match returns MATCH" "$RESPONSE" "MATCH"

# Test HSM PIN verification (BDD-W01)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/hsm/verify-pin" \
    -H "Content-Type: application/json" \
    -d '{"pinBlock":"blob","pan":"4111111111111111"}')
assert_contains "HSM: PIN verification returns valid" "$RESPONSE" "true"

# Test PayNet card auth (BDD-W01)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/paynet/iso8583/auth" \
    -H "Content-Type: application/json" \
    -d '{"pan":"4111111111111111","amount":500}')
assert_contains "PayNet: card auth returns APPROVED" "$RESPONSE" "APPROVED"
assert_contains "PayNet: card auth returns responseCode 00" "$RESPONSE" "00"
assert_contains "PayNet: card auth returns referenceId" "$RESPONSE" "PAYNET-"

# Test PayNet reversal (BDD-W01-EC-02)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/paynet/iso8583/reversal" \
    -H "Content-Type: application/json" \
    -d '{"originalReferenceId":"REF-123","amount":500}')
assert_contains "PayNet: reversal returns ACKNOWLEDGED" "$RESPONSE" "ACKNOWLEDGED"

# Test DuitNow transfer (BDD-DNOW-01)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/paynet/iso20022/transfer" \
    -H "Content-Type: application/json" \
    -d '{"proxyType":"MOBILE","proxyValue":"0123456789","amount":1000}')
assert_contains "PayNet: DuitNow returns SETTLED" "$RESPONSE" "SETTLED"

# Test JomPAY validation - valid ref (BDD-B01)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/billers/JOMPAY/validate" \
    -H "Content-Type: application/json" \
    -d '{"ref1":"INV-12345"}')
assert_contains "Biller: JomPAY validate returns valid=true" "$RESPONSE" "\"valid\":true"
assert_contains "Biller: JomPAY validate returns amount" "$RESPONSE" "150.00"

# Test JomPAY validation - invalid ref (BDD-B01-EC-01)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/billers/JOMPAY/validate" \
    -H "Content-Type: application/json" \
    -d '{"ref1":"INVALID-REF"}')
assert_contains "Biller: JomPAY validate returns valid=false for invalid ref" "$RESPONSE" "\"valid\":false"

# Test JomPAY payment (BDD-B01)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/billers/JOMPAY/pay" \
    -H "Content-Type: application/json" \
    -d '{"ref1":"INV-12345","amount":150}')
assert_contains "Biller: JomPAY payment returns PAID" "$RESPONSE" "PAID"
assert_contains "Biller: JomPAY payment returns receiptNo" "$RESPONSE" "JOMPAY-"

# Test CELCOM top-up (BDD-T01)
RESPONSE=$(curl -s -X POST "$BASE_URL/mock/billers/celcom/topup" \
    -H "Content-Type: application/json" \
    -d '{"phoneNumber":"0191234567","amount":30}')
assert_contains "Biller: CELCOM top-up returns COMPLETED" "$RESPONSE" "COMPLETED"

# ==================== Service Health Checks ====================
echo ""
echo "--- Service Health Checks ---"

check_service() {
    local name="$1"
    local url="$2"
    if curl -sf "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASS${NC}: $name is healthy"
        ((PASS++))
    else
        echo -e "${RED}✗ FAIL${NC}: $name is not responding at $url"
        ((FAIL++))
    fi
}

check_service "Rules Service" "http://localhost:8081/internal/fees/CASH_WITHDRAWAL/MICRO" 2>/dev/null || echo "  (skip - service not running)"
check_service "Ledger Service" "http://localhost:8082/internal/balance/00000000-0000-0000-0000-000000000000" 2>/dev/null || echo "  (skip - service not running)"
check_service "Onboarding Service" "http://localhost:8083/internal/verify-mykad" 2>/dev/null || echo "  (skip - service not running)"
check_service "Switch Adapter" "http://localhost:8084/internal/auth" 2>/dev/null || echo "  (skip - service not running)"
check_service "Biller Service" "http://localhost:8085/internal/validate-ref" 2>/dev/null || echo "  (skip - service not running)"

# ==================== Summary ====================
echo ""
echo "=========================================="
echo "Results: $PASS passed, $FAIL failed"
echo "=========================================="

if [ $FAIL -gt 0 ]; then
    exit 1
fi

echo -e "${GREEN}All tests passed!${NC}"
