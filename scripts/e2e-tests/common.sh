#!/bin/bash
#
# Common E2E Test Helpers
# Shared utilities for all BDD E2E test scripts
#
# Source this file in test scripts: source "$(dirname "$0")/common.sh"

set -e

# ============================================================================
# Configuration
# ============================================================================

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TOKEN_FILE="/tmp/e2e_tokens.env"
TEST_RESULTS_FILE="/tmp/e2e_results.txt"

# Load tokens if available
if [ -f "$TOKEN_FILE" ]; then
    source "$TOKEN_FILE"
fi

# ============================================================================
# Colors
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ============================================================================
# Counters
# ============================================================================

TOTAL=0
PASSED=0
FAILED=0
SKIPPED=0

# ============================================================================
# Logging Functions
# ============================================================================

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

section() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

subsection() {
    echo ""
    echo -e "${YELLOW}--- $1 ---${NC}"
}

# ============================================================================
# Assertion Functions
# ============================================================================

assert_status() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    
    TOTAL=$((TOTAL + 1))
    
    if [ "$expected" = "$actual" ]; then
        echo -e "  ${GREEN}✓${NC} $test_name"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "  ${RED}✗${NC} $test_name (expected: $expected, got: $actual)"
        FAILED=$((FAILED + 1))
        return 1
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
        return 0
    else
        echo -e "  ${RED}✗${NC} $test_name (pattern not found: $pattern)"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

assert_not_contains() {
    local test_name="$1"
    local response="$2"
    local pattern="$3"
    
    TOTAL=$((TOTAL + 1))
    
    if ! echo "$response" | grep -q "$pattern"; then
        echo -e "  ${GREEN}✓${NC} $test_name"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "  ${RED}✗${NC} $test_name (pattern found when not expected: $pattern)"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

assert_json_field() {
    local test_name="$1"
    local response="$2"
    local field="$3"
    local expected="$4"
    
    TOTAL=$((TOTAL + 1))
    
    local actual=$(echo "$response" | jq -r "$field // empty" 2>/dev/null)
    
    if [ "$actual" = "$expected" ]; then
        echo -e "  ${GREEN}✓${NC} $test_name"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "  ${RED}✗${NC} $test_name (field $field: expected '$expected', got '$actual')"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

assert_json_field_exists() {
    local test_name="$1"
    local response="$2"
    local field="$3"
    
    TOTAL=$((TOTAL + 1))
    
    local value=$(echo "$response" | jq -r "$field // empty" 2>/dev/null)
    
    if [ -n "$value" ] && [ "$value" != "null" ]; then
        echo -e "  ${GREEN}✓${NC} $test_name"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "  ${RED}✗${NC} $test_name (field $field not found or null)"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

assert_json_field_number() {
    local test_name="$1"
    local response="$2"
    local field="$3"
    
    TOTAL=$((TOTAL + 1))
    
    local value=$(echo "$response" | jq -r "$field // empty" 2>/dev/null)
    
    if [[ "$value" =~ ^[0-9]+\.?[0-9]*$ ]]; then
        echo -e "  ${GREEN}✓${NC} $test_name"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "  ${RED}✗${NC} $test_name (field $field is not a number: '$value')"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

# ============================================================================
# HTTP Functions
# ============================================================================

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
    
    curl_cmd="$curl_cmd '$GATEWAY_URL$endpoint'"
    
    eval $curl_cmd
}

get_status() {
    echo "$1" | tail -1
}

get_body() {
    echo "$1" | head -1
}

# ============================================================================
# Token Management
# ============================================================================

get_token() {
    local username="$1"
    local password="$2"
    
    local response=$(curl -s -X POST "$GATEWAY_URL/auth/token" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")
    
    echo "$response" | jq -r '.accessToken // empty'
}

load_tokens() {
    if [ -f "$TOKEN_FILE" ]; then
        source "$TOKEN_FILE"
        log_info "Loaded tokens from $TOKEN_FILE"
    else
        log_warn "Token file not found: $TOKEN_FILE"
        log_warn "Run seed-test-data.sh first"
        return 1
    fi
}

# ============================================================================
# Wait Functions
# ============================================================================

wait_for_service() {
    local url="$1"
    local name="$2"
    local max_attempts="${3:-30}"
    local attempt=1
    
    log_info "Waiting for $name to be ready..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null | grep -q "200\|401"; then
            log_success "$name is ready"
            return 0
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    log_error "$name failed to start after $max_attempts attempts"
    return 1
}

# ============================================================================
# Test Data
# ============================================================================

generate_uuid() {
    if command -v uuidgen &> /dev/null; then
        uuidgen | tr '[:upper:]' '[:lower:]'
    else
        cat /proc/sys/kernel/random/uuid 2>/dev/null || python3 -c "import uuid; print(uuid.uuid4())"
    fi
}

random_mykad() {
    # Generate a random 12-digit MyKad number
    echo "$(shuf -i 100000000000-999999999999 -n 1)"
}

# ============================================================================
# Summary
# ============================================================================

print_summary() {
    local test_name="${1:-E2E Tests}"
    
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  $test_name - Summary${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "  Total:   $TOTAL"
    echo -e "  ${GREEN}Passed:  $PASSED${NC}"
    echo -e "  ${RED}Failed:  $FAILED${NC}"
    if [ $SKIPPED -gt 0 ]; then
        echo -e "  ${YELLOW}Skipped: $SKIPPED${NC}"
    fi
    echo ""
    
    # Write results to file
    echo "$test_name|$TOTAL|$PASSED|$FAILED|$SKIPPED" >> "$TEST_RESULTS_FILE"
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}✓ All tests passed!${NC}"
        return 0
    else
        echo -e "${RED}✗ $FAILED test(s) failed${NC}"
        return 1
    fi
}

reset_counters() {
    TOTAL=0
    PASSED=0
    FAILED=0
    SKIPPED=0
}

# ============================================================================
# Export
# ============================================================================

export GATEWAY_URL TOKEN_FILE TEST_RESULTS_FILE
export RED GREEN YELLOW BLUE NC
export -f log_info log_success log_error log_warn section subsection
export -f assert_status assert_contains assert_not_contains assert_json_field
export -f assert_json_field_exists assert_json_field_number
export -f api_call get_status get_body
export -f get_token load_tokens wait_for_service
export -f generate_uuid random_mykad
export -f print_summary reset_counters