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
MISSING_APIS=""

# Missing API report file
MISSING_API_REPORT="/tmp/e2e_missing_apis.txt"
FAILED_TESTS_REPORT="/tmp/e2e_failed_tests.txt"

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
# Missing API Tracking
# ============================================================================

track_missing_api() {
    local endpoint="$1"
    local method="$2"
    local key="$method:$endpoint"
    
    if ! echo "$MISSING_APIS" | grep -q "$key"; then
        MISSING_APIS="$MISSING_APIS|$key"
        echo "404|$method|$endpoint" >> "$MISSING_API_REPORT"
    fi
}

track_failed_test() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    local endpoint="$4"
    local method="$5"
    
    # Only track if it's a test failure (not auth issue)
    if [ "$expected" != "$actual" ]; then
        echo "$actual|$method|$endpoint|$test_name|expected:$expected" >> "$FAILED_TESTS_REPORT"
    fi
}

print_missing_api_report() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════════════╗"
    echo "║  API Test Failure Report                                      ║"
    echo "╚════════════════════════════════════════════════════════════════════╝"
    echo ""
    
    # Show failed tests (any non-matching status code)
    if [ -s "$FAILED_TESTS_REPORT" ]; then
        echo -e "${RED}Failed Tests:${NC}"
        echo "------------------------------------------------------------"
        while IFS='|' read -r actual method endpoint test_name expected; do
            if [ "$actual" = "404" ]; then
                echo -e "  ${YELLOW}404${NC}  $method $endpoint - $test_name"
            elif [ "$actual" = "401" ]; then
                echo -e "  ${RED}401${NC}  $method $endpoint - $test_name (Auth required)"
            elif [ "$actual" = "500" ]; then
                echo -e "  ${RED}500${NC}  $method $endpoint - $test_name (Server error)"
            else
                echo -e "  ${RED}$actual${NC}  $method $endpoint - $test_name (expected: $expected)"
            fi
        done < "$FAILED_TESTS_REPORT"
        echo ""
        echo "Total failed tests: $(wc -l < "$FAILED_TESTS_REPORT")"
    fi
    
    # Summary by status code
    if [ -s "$FAILED_TESTS_REPORT" ]; then
        echo ""
        echo "Summary by Status Code:"
        echo "------------------------------------------------------------"
        cut -d'|' -f1 "$FAILED_TESTS_REPORT" | sort | uniq -c | sort -rn | while read count status; do
            case $status in
                404) echo -e "  ${YELLOW}$count x 404${NC} - Endpoint not found / Not implemented" ;;
                401) echo -e "  ${RED}$count x 401${NC} - Authentication required" ;;
                500) echo -e "  ${RED}$count x 500${NC} - Server error" ;;
                *)   echo -e "  ${RED}$count x $status${NC} - Other error" ;;
            esac
        done
        echo ""
    fi
}

subsection() {
    echo ""
    echo -e "${YELLOW}--- $1 ---${NC}"
}

# Last API call info (used for tracking missing endpoints)
LAST_ENDPOINT=""
LAST_METHOD=""
LAST_ENDPOINT_FILE="/tmp/last_endpoint.txt"

# ============================================================================
# Assertion Functions
# ============================================================================

assert_status() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    
    # Read last endpoint from file (set by api_call in same command group)
    if [ -f "$LAST_ENDPOINT_FILE" ]; then
        LAST_ENDPOINT=$(head -1 "$LAST_ENDPOINT_FILE")
        LAST_METHOD=$(tail -1 "$LAST_ENDPOINT_FILE")
    fi
    
    TOTAL=$((TOTAL + 1))
    
    # Track missing APIs (404 responses)
    if [ "$actual" = "404" ] && [ -n "$LAST_ENDPOINT" ]; then
        track_missing_api "$LAST_ENDPOINT" "$LAST_METHOD"
    fi
    
    # Track all failed tests
    if [ "$expected" != "$actual" ]; then
        track_failed_test "$test_name" "$expected" "$actual" "$LAST_ENDPOINT" "$LAST_METHOD"
    fi
    
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
    
    # Write endpoint info to temp file for tracking
    echo -e "$endpoint\n$method" > "$LAST_ENDPOINT_FILE"
    
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
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
        if [ "$http_code" != "000" ]; then
            log_success "$name is ready (HTTP $http_code)"
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
    elif [ -f /proc/sys/kernel/random/uuid ]; then
        cat /proc/sys/kernel/random/uuid 2>/dev/null
    elif command -v powershell &> /dev/null; then
        powershell -Command "[guid]::NewGuid().ToString()"
    else
        echo "uuid-generation-failed"
        return 1
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

# Note: export -f doesn't work on Windows/Git Bash
export GATEWAY_URL TOKEN_FILE TEST_RESULTS_FILE MISSING_API_REPORT FAILED_TESTS_REPORT
export RED GREEN YELLOW BLUE NC