#!/bin/bash
#
# End-to-End API Gateway Tests
# Tests all external APIs with real JWT tokens from auth-iam-service
#
# Usage: ./run-e2e-tests.sh [gateway_url]
# Default gateway_url: http://localhost:8080

set -e

GATEWAY_URL="${1:-http://localhost:8080}"
AUTH_URL="${GATEWAY_URL}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Token storage
declare -A TOKENS

# ============================================================================
# Utility Functions
# ============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

log_section() {
    echo ""
    echo -e "${YELLOW}===================================================${NC}"
    echo -e "${YELLOW}  $1${NC}"
    echo -e "${YELLOW}===================================================${NC}"
}

# Run a test and track results
run_test() {
    local test_name="$1"
    local expected_status="$2"
    local url="$3"
    local method="${4:-GET}"
    local auth_header="${5:-}"
    local body="${6:-}"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    local curl_cmd="curl -s -o /tmp/e2e_response.json -w '%{http_code}' -X $method"
    
    if [ -n "$auth_header" ]; then
        curl_cmd="$curl_cmd -H 'Authorization: Bearer $auth_header'"
    fi
    
    if [ -n "$body" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$body'"
    fi
    
    curl_cmd="$curl_cmd '$url'"
    
    local status=$(eval $curl_cmd)
    local response=$(cat /tmp/e2e_response.json 2>/dev/null || echo "{}")
    
    if [ "$status" = "$expected_status" ]; then
        log_success "$test_name (HTTP $status)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "$response" > /dev/null
    else
        log_error "$test_name (Expected: $expected_status, Got: $status)"
        echo "  Response: $(echo $response | head -c 200)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# Get JWT token for a user
get_token() {
    local username="$1"
    local password="$2"
    
    local response=$(curl -s -X POST "$AUTH_URL/auth/token" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")
    
    local token=$(echo "$response" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    echo "$token"
}

# ============================================================================
# Test Sections
# ============================================================================

test_public_endpoints() {
    log_section "1. PUBLIC ENDPOINTS (No Auth Required)"
    
    # Health check
    run_test "Gateway Health Check" "200" "$GATEWAY_URL/actuator/health"
    
    # Auth token endpoint
    run_test "Auth Token Endpoint Exists" "401" "$AUTH_URL/auth/token" "POST" "" '{"username":"test","password":"test"}'
    
    # Well-known endpoints
    run_test "JWKS Endpoint" "200" "$AUTH_URL/.well-known/jwks.json"
}

test_authentication() {
    log_section "2. AUTHENTICATION (BDD: US-AUTH-16, US-AUTH-17)"
    
    # Test invalid credentials
    run_test "Auth fails with invalid credentials" "401" "$AUTH_URL/auth/token" "POST" "" \
        '{"username":"nonexistent","password":"wrong"}'
    
    # Note: To test valid auth, you need pre-seeded users
    # The tests below assume users exist in the database
    
    log_info "To test valid authentication, ensure these users exist:"
    log_info "  - admin/password (IT Admin)"
    log_info "  - agent001/AgentPass123! (Agent)"
    log_info "  - operator001/OperatorPass123! (Bank Operator)"
}

test_protected_endpoints_with_tokens() {
    log_section "3. PROTECTED ENDPOINTS (JWT Required)"
    
    # Try to get tokens (will fail if users don't exist, but that's expected)
    log_info "Attempting to get JWT tokens..."
    
    local admin_token=$(get_token "admin" "password")
    local agent_token=$(get_token "agent001" "AgentPass123!")
    local operator_token=$(get_token "operator001" "OperatorPass123!")
    
    if [ -z "$admin_token" ]; then
        log_info "No admin token available - skipping admin tests"
        log_info "Create admin user first: POST /auth/users with admin credentials"
        return
    fi
    
    TOKENS[admin]="$admin_token"
    TOKENS[agent]="$agent_token"
    TOKENS[operator]="$operator_token"
    
    log_info "Admin token obtained: ${admin_token:0:20}..."
    
    # Test protected endpoints with admin token
    test_user_management "${TOKENS[admin]}"
    test_role_management "${TOKENS[admin]}"
    test_permission_management "${TOKENS[admin]}"
    test_audit_endpoints "${TOKENS[admin]}"
    
    # Test agent-specific endpoints
    if [ -n "$agent_token" ]; then
        test_agent_endpoints "$agent_token"
    fi
}

test_user_management() {
    local token="$1"
    log_section "3.1 USER MANAGEMENT (BDD: US-AUTH-01)"
    
    # List users
    run_test "List Users" "200" "$AUTH_URL/auth/users" "GET" "$token"
    
    # Create user
    run_test "Create User" "200" "$AUTH_URL/auth/users" "POST" "$token" \
        '{"username":"testuser001","email":"test001@bank.com","password":"TestPass123!","fullName":"Test User 001"}'
    
    # Get user by ID (would need actual ID from create response)
    # This is a placeholder - actual test would extract ID from create response
    
    # Lock user
    # Delete user
}

test_role_management() {
    local token="$1"
    log_section "3.2 ROLE MANAGEMENT (BDD: US-AUTH-02, US-AUTH-04)"
    
    # List roles
    run_test "List Roles" "200" "$AUTH_URL/auth/roles" "GET" "$token"
    
    # Create role
    run_test "Create Role" "200" "$AUTH_URL/auth/roles" "POST" "$token" \
        '{"roleName":"TEST_TELLER","description":"Test teller role"}'
}

test_permission_management() {
    local token="$1"
    log_section "3.3 PERMISSION MANAGEMENT (BDD: US-AUTH-02)"
    
    # List permissions
    run_test "List Permissions" "200" "$AUTH_URL/auth/permissions" "GET" "$token"
    
    # Create permission
    run_test "Create Permission" "200" "$AUTH_URL/auth/permissions" "POST" "$token" \
        '{"permissionKey":"test:read","description":"Test read permission","resource":"test","action":"read"}'
}

test_audit_endpoints() {
    local token="$1"
    log_section "3.4 AUDIT LOGS (BDD: US-AUTH-09)"
    
    # Get audit logs
    run_test "Get Audit Logs" "200" "$AUTH_URL/auth/audit/logs" "GET" "$token"
}

test_agent_endpoints() {
    local token="$1"
    log_section "3.5 AGENT ENDPOINTS (via Gateway)"
    
    # These test the gateway routing to backend services
    # They require backend services to be running
    
    run_test "Agent Balance Inquiry" "200" "$GATEWAY_URL/api/v1/agent/balance" "GET" "$token"
}

test_gateway_routing() {
    log_section "4. GATEWAY ROUTING"
    
    local admin_token="${TOKENS[admin]}"
    if [ -z "$admin_token" ]; then
        log_info "No admin token - skipping gateway routing tests"
        return
    fi
    
    # Test backoffice routes through gateway
    run_test "Backoffice Dashboard Route" "200" "$GATEWAY_URL/api/v1/backoffice/dashboard" "GET" "$admin_token"
    run_test "Backoffice Agents Route" "200" "$GATEWAY_URL/api/v1/backoffice/agents" "GET" "$admin_token"
    run_test "Backoffice Transactions Route" "200" "$GATEWAY_URL/api/v1/backoffice/transactions" "GET" "$admin_token"
}

test_error_handling() {
    log_section "5. ERROR HANDLING (Global Error Schema)"
    
    # Test missing token
    run_test "Missing Token Returns 401" "401" "$GATEWAY_URL/api/v1/agent/balance" "GET" ""
    
    # Test invalid token
    run_test "Invalid Token Returns 401" "401" "$GATEWAY_URL/api/v1/agent/balance" "GET" "invalid.token.here"
    
    # Test malformed Authorization header
    run_test "Malformed Auth Header Returns 401" "401" "$GATEWAY_URL/api/v1/agent/balance" "GET" "NotBearer token"
}

# ============================================================================
# Main Execution
# ============================================================================

main() {
    echo ""
    echo "==================================================="
    echo "  Agent Banking Platform - E2E API Gateway Tests"
    echo "==================================================="
    echo ""
    log_info "Gateway URL: $GATEWAY_URL"
    log_info "Auth URL: $AUTH_URL"
    echo ""
    
    # Run test sections
    test_public_endpoints
    test_authentication
    test_protected_endpoints_with_tokens
    test_gateway_routing
    test_error_handling
    
    # Summary
    echo ""
    log_section "TEST SUMMARY"
    echo ""
    echo "  Total Tests:  $TOTAL_TESTS"
    echo -e "  ${GREEN}Passed:${NC}       $PASSED_TESTS"
    echo -e "  ${RED}Failed:${NC}       $FAILED_TESTS"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}All tests passed!${NC}"
        exit 0
    else
        echo -e "${RED}$FAILED_TESTS test(s) failed.${NC}"
        exit 1
    fi
}

main
