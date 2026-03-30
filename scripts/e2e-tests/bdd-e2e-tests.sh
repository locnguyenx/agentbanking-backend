#!/bin/bash
#
# Comprehensive E2E Tests for Auth/IAM Service
# Covers all BDD scenarios from 2026-03-29-auth-iam-service-bdd.md
#
# Prerequisites:
# 1. Docker containers running (docker compose --profile all up -d)
# 2. Test data seeded (./seed-test-data.sh)
# 3. Tokens available in /tmp/e2e_tokens.env
#
# Usage: ./bdd-e2e-tests.sh [gateway_url]

set -e

GATEWAY_URL="${1:-http://localhost:8080}"
AUTH_URL="$GATEWAY_URL"

# Load tokens
if [ -f /tmp/e2e_tokens.env ]; then
    source /tmp/e2e_tokens.env
else
    echo "ERROR: Run seed-test-data.sh first"
    exit 1
fi

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Counters
TOTAL=0
PASSED=0
FAILED=0

# ============================================================================
# Test Helpers
# ============================================================================

assert_status() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    
    TOTAL=$((TOTAL + 1))
    
    if [ "$expected" = "$actual" ]; then
        echo -e "  ${GREEN}✓${NC} $test_name"
        PASSED=$((PASSED + 1))
    else
        echo -e "  ${RED}✗${NC} $test_name (expected: $expected, got: $actual)"
        FAILED=$((FAILED + 1))
    fi
}

assert_contains() {
    local test_name="$1"
    local response="$2"
    local pattern="$3"
    
    TOTAL=$((TOTAL + 1))
    
    if echo "$response" | grep -q "$pattern"; then
        echo -e "  ${GREEN}✓${NC} $test_name"
        PASSED=$((PASSED + 1))
    else
        echo -e "  ${RED}✗${NC} $test_name (pattern not found: $pattern)"
        FAILED=$((FAILED + 1))
    fi
}

api_call() {
    local method="$1"
    local endpoint="$2"
    local token="$3"
    local body="$4"
    
    local curl_cmd="curl -s -w '\n%{http_code}' -X $method"
    
    if [ -n "$token" ]; then
        curl_cmd="$curl_cmd -H 'Authorization: Bearer $token'"
    fi
    
    if [ -n "$body" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$body'"
    fi
    
    curl_cmd="$curl_cmd '$AUTH_URL$endpoint'"
    
    eval $curl_cmd
}

section() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# ============================================================================
# BDD Section 1: User Management (US-AUTH-01, US-AUTH-02, US-AUTH-06, US-AUTH-07)
# ============================================================================

test_user_management() {
    section "BDD 1: User Management"
    
    # US-AUTH-01: Administrator creates a new user account
    echo -e "\n${YELLOW}Scenario: Administrator creates a new user account${NC}"
    local response=$(api_call "POST" "/auth/users" "$ADMIN_TOKEN" \
        '{"username":"newuser001","email":"newuser001@bank.com","password":"NewPass123!","fullName":"New User 001"}')
    local status=$(echo "$response" | tail -1)
    local body=$(echo "$response" | head -1)
    assert_status "User is created" "201" "$status"
    assert_contains "User has username" "$body" "newuser001"
    
    # US-AUTH-01: Duplicate username is rejected
    echo -e "\n${YELLOW}Scenario: Duplicate username is rejected${NC}"
    response=$(api_call "POST" "/auth/users" "$ADMIN_TOKEN" \
        '{"username":"newuser001","email":"duplicate@bank.com","password":"Pass123!","fullName":"Duplicate"}')
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | head -1)
    assert_status "Duplicate rejected" "400" "$status"
    
    # US-AUTH-01: Duplicate email is rejected
    echo -e "\n${YELLOW}Scenario: Duplicate email is rejected${NC}"
    response=$(api_call "POST" "/auth/users" "$ADMIN_TOKEN" \
        '{"username":"anotheruser","email":"newuser001@bank.com","password":"Pass123!","fullName":"Another"}')
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | head -1)
    assert_status "Duplicate email rejected" "400" "$status"
    
    # US-AUTH-06: Administrator locks user account
    echo -e "\n${YELLOW}Scenario: Administrator locks user account${NC}"
    response=$(api_call "POST" "/auth/users/newuser001/lock" "$ADMIN_TOKEN" "")
    status=$(echo "$response" | tail -1)
    assert_status "User locked" "200" "$status"
    
    # US-AUTH-07: Administrator resets user password
    echo -e "\n${YELLOW}Scenario: Administrator resets user password${NC}"
    response=$(api_call "POST" "/auth/users/newuser001/reset-password" "$ADMIN_TOKEN" \
        '{"newPassword":"ResetPass123!"}')
    status=$(echo "$response" | tail -1)
    assert_status "Password reset" "200" "$status"
}

# ============================================================================
# BDD Section 2: Authentication (US-AUTH-16, US-AUTH-17, US-AUTH-18)
# ============================================================================

test_authentication() {
    section "BDD 2: Authentication"
    
    # US-AUTH-16: End user authenticates with valid credentials
    echo -e "\n${YELLOW}Scenario: End user authenticates with valid credentials${NC}"
    local response=$(api_call "POST" "/auth/token" "" \
        '{"username":"admin","password":"AdminPass123!"}')
    local status=$(echo "$response" | tail -1)
    local body=$(echo "$response" | head -1)
    assert_status "Authentication successful" "200" "$status"
    assert_contains "Access token returned" "$body" "accessToken"
    assert_contains "Refresh token returned" "$body" "refreshToken"
    
    # Extract refresh token for later test
    REFRESH_TOKEN=$(echo "$body" | jq -r '.refreshToken // empty')
    
    # US-AUTH-16: Authentication fails with invalid password
    echo -e "\n${YELLOW}Scenario: Authentication fails with invalid password${NC}"
    response=$(api_call "POST" "/auth/token" "" \
        '{"username":"admin","password":"WrongPass!"}')
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | head -1)
    assert_status "Authentication fails" "401" "$status"
    
    # US-AUTH-16: Authentication fails with non-existent user
    echo -e "\n${YELLOW}Scenario: Authentication fails with non-existent user${NC}"
    response=$(api_call "POST" "/auth/token" "" \
        '{"username":"nonexistent","password":"AnyPass123!"}')
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | head -1)
    assert_status "Authentication fails" "401" "$status"
    
    # US-AUTH-17: End user refreshes access token
    if [ -n "$REFRESH_TOKEN" ]; then
        echo -e "\n${YELLOW}Scenario: End user refreshes access token${NC}"
        response=$(api_call "POST" "/auth/refresh" "" \
            "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
        status=$(echo "$response" | tail -1)
        body=$(echo "$response" | head -1)
        assert_status "Token refreshed" "200" "$status"
        assert_contains "New access token" "$body" "accessToken"
    fi
}

# ============================================================================
# BDD Section 3: Authorization & Access Control (US-AUTH-02, US-AUTH-04, US-AUTH-19)
# ============================================================================

test_authorization() {
    section "BDD 3: Authorization & Access Control"
    
    # US-AUTH-02: Administrator creates role
    echo -e "\n${YELLOW}Scenario: Administrator creates role${NC}"
    local response=$(api_call "POST" "/auth/roles" "$ADMIN_TOKEN" \
        '{"roleName":"TEST_ROLE","description":"Test role for E2E"}')
    local status=$(echo "$response" | tail -1)
    assert_status "Role created" "201" "$status"
    
    # US-AUTH-02: Administrator creates permission
    echo -e "\n${YELLOW}Scenario: Administrator creates permission${NC}"
    response=$(api_call "POST" "/auth/permissions" "$ADMIN_TOKEN" \
        '{"permissionKey":"test:e2e","description":"E2E test permission","resource":"test","action":"e2e"}')
    status=$(echo "$response" | tail -1)
    assert_status "Permission created" "201" "$status"
    
    # US-AUTH-19: Unauthorized access is rejected
    echo -e "\n${YELLOW}Scenario: Unauthorized access is rejected${NC}"
    response=$(api_call "GET" "/auth/users" "" "")
    status=$(echo "$response" | tail -1)
    assert_status "No token rejected" "401" "$status"
    
    response=$(api_call "GET" "/auth/users" "invalid.token.here" "")
    status=$(echo "$response" | tail -1)
    assert_status "Invalid token rejected" "401" "$status"
}

# ============================================================================
# BDD Section 6: Integration & Configuration (US-AUTH-13)
# ============================================================================

test_integration() {
    section "BDD 6: Integration & Configuration"
    
    # US-AUTH-13: Health check endpoint reports service status
    echo -e "\n${YELLOW}Scenario: Health check endpoint reports service status${NC}"
    local response=$(api_call "GET" "/actuator/health" "" "")
    local status=$(echo "$response" | tail -1)
    local body=$(echo "$response" | head -1)
    assert_status "Health check responds" "200" "$status"
    assert_contains "Status is UP" "$body" '"status":"UP"'
}

# ============================================================================
# Gateway Routing Tests
# ============================================================================

test_gateway_routing() {
    section "Gateway Routing Tests"
    
    # Test auth routes
    echo -e "\n${YELLOW}Auth routes via gateway${NC}"
    local response=$(api_call "GET" "/auth/users" "$ADMIN_TOKEN" "")
    local status=$(echo "$response" | tail -1)
    assert_status "GET /auth/users" "200" "$status"
    
    response=$(api_call "GET" "/auth/roles" "$ADMIN_TOKEN" "")
    status=$(echo "$response" | tail -1)
    assert_status "GET /auth/roles" "200" "$status"
    
    response=$(api_call "GET" "/auth/permissions" "$ADMIN_TOKEN" "")
    status=$(echo "$response" | tail -1)
    assert_status "GET /auth/permissions" "200" "$status"
    
    # Test API docs
    echo -e "\n${YELLOW}API documentation routes${NC}"
    response=$(api_call "GET" "/docs/auth/v3/api-docs" "" "")
    status=$(echo "$response" | tail -1)
    assert_status "Auth API docs accessible" "200" "$status"
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Agent Banking Platform - BDD E2E Tests                   ║"
    echo "║  Auth/IAM Service Comprehensive Testing                   ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "Gateway: $GATEWAY_URL"
    echo ""
    
    # Run all test suites
    test_authentication
    test_user_management
    test_authorization
    test_integration
    test_gateway_routing
    
    # Summary
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Test Summary                                              ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "  Total:   $TOTAL"
    echo -e "  ${GREEN}Passed:  $PASSED${NC}"
    echo -e "  ${RED}Failed:  $FAILED${NC}"
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}✓ All BDD scenarios passed!${NC}"
        exit 0
    else
        echo -e "${RED}✗ $FAILED scenario(s) failed${NC}"
        exit 1
    fi
}

main
