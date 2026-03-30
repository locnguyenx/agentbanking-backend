#!/bin/bash
#
# E2E Test Seed Data Script
# Creates test users with different roles for E2E testing
#
# Usage: ./seed-test-data.sh [gateway_url]

set -e

GATEWAY_URL="${1:-http://localhost:8080}"
AUTH_URL="$GATEWAY_URL"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${YELLOW}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ============================================================================
# Seed Functions
# ============================================================================

wait_for_service() {
    local url="$1"
    local name="$2"
    local max_attempts=30
    local attempt=1
    
    log_info "Waiting for $name to be ready..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "200\|401"; then
            log_success "$name is ready"
            return 0
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    log_error "$name failed to start"
    return 1
}

create_user_via_bootstrap() {
    local username="$1"
    local email="$2"
    local password="$3"
    local fullName="$4"
    
    log_info "Creating user via bootstrap: $username"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$AUTH_URL/auth/users/bootstrap" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"$username\",
            \"email\": \"$email\",
            \"password\": \"$password\",
            \"fullName\": \"$fullName\"
        }")
    
    local body=$(echo "$response" | head -n 1)
    local status=$(echo "$response" | tail -n 1)
    
    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_success "User $username created via bootstrap"
        echo "$body" | jq -r '.userId // empty'
    else
        log_error "Failed to create user $username via bootstrap: $status - $body"
        return 1
    fi
}

create_user() {
    local username="$1"
    local email="$2"
    local password="$3"
    local fullName="$4"
    local admin_token="$5"
    
    log_info "Creating user: $username"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$AUTH_URL/auth/users" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $admin_token" \
        -d "{
            \"username\": \"$username\",
            \"email\": \"$email\",
            \"password\": \"$password\",
            \"fullName\": \"$fullName\"
        }")
    
    local body=$(echo "$response" | head -n 1)
    local status=$(echo "$response" | tail -n 1)
    
    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_success "User $username created"
        echo "$body" | jq -r '.userId // empty'
    elif echo "$body" | grep -q "already exists"; then
        log_info "User $username already exists"
        # Get user ID from list
        local users_response=$(curl -s "$AUTH_URL/auth/users" \
            -H "Authorization: Bearer $admin_token")
        echo "$users_response" | jq -r ".[] | select(.username==\"$username\") | .userId // empty"
    else
        log_error "Failed to create user $username: $status - $body"
        return 1
    fi
}

create_role() {
    local role_name="$1"
    local description="$2"
    local admin_token="$3"
    
    log_info "Creating role: $role_name"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$AUTH_URL/auth/roles" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $admin_token" \
        -d "{
            \"roleName\": \"$role_name\",
            \"description\": \"$description\"
        }")
    
    local body=$(echo "$response" | head -n 1)
    local status=$(echo "$response" | tail -n 1)
    
    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_success "Role $role_name created"
        echo "$body" | jq -r '.roleId // empty'
    elif echo "$body" | grep -q "already exists"; then
        log_info "Role $role_name already exists"
    else
        log_error "Failed to create role $role_name: $status - $body"
    fi
}

create_permission() {
    local key="$1"
    local description="$2"
    local resource="$3"
    local action="$4"
    local admin_token="$5"
    
    log_info "Creating permission: $key"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$AUTH_URL/auth/permissions" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $admin_token" \
        -d "{
            \"permissionKey\": \"$key\",
            \"description\": \"$description\",
            \"resource\": \"$resource\",
            \"action\": \"$action\"
        }")
    
    local body=$(echo "$response" | head -n 1)
    local status=$(echo "$response" | tail -n 1)
    
    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_success "Permission $key created"
    elif echo "$body" | grep -q "already exists"; then
        log_info "Permission $key already exists"
    else
        log_error "Failed to create permission $key: $status - $body"
    fi
}

get_token() {
    local username="$1"
    local password="$2"
    
    local response=$(curl -s -X POST "$AUTH_URL/auth/token" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")
    
    echo "$response" | jq -r '.accessToken // empty'
}

# ============================================================================
# Main Seed Process
# ============================================================================

main() {
    echo ""
    echo "============================================="
    echo "  E2E Test Data Seeding"
    echo "============================================="
    echo ""
    
    # Wait for auth service
    wait_for_service "$AUTH_URL/actuator/health" "Auth/IAM Service"
    
    # Step 1: Get admin token (admin user should exist from Flyway migration)
    log_info "Step 1: Getting admin token..."
    
    local admin_token=$(get_token "admin" "AdminPass123!")
    
    if [ -z "$admin_token" ]; then
        log_info "Admin user doesn't exist in DB, creating via bootstrap..."
        create_user_via_bootstrap "admin" "admin@agentbanking.com" "AdminPass123!" "System Administrator"
        admin_token=$(get_token "admin" "AdminPass123!")
    fi
    
    if [ -z "$admin_token" ]; then
        log_error "Failed to get admin token"
        exit 1
    fi
    
    log_success "Admin authenticated"
    
    # Step 2: Create roles
    log_info ""
    log_info "Step 2: Creating roles..."
    create_role "IT_ADMIN" "IT Administrator with full system access" "$admin_token"
    create_role "BANK_OPERATOR" "Bank operations staff" "$admin_token"
    create_role "AGENT" "Field agent for transactions" "$admin_token"
    create_role "AUDITOR" "Read-only auditor access" "$admin_token"
    create_role "TELLER" "Branch teller for customer transactions" "$admin_token"
    
    # Step 3: Create permissions
    log_info ""
    log_info "Step 3: Creating permissions..."
    create_permission "user:read" "Read user information" "user" "read" "$admin_token"
    create_permission "user:write" "Create/update users" "user" "write" "$admin_token"
    create_permission "user:delete" "Delete users" "user" "delete" "$admin_token"
    create_permission "role:read" "Read role information" "role" "read" "$admin_token"
    create_permission "role:write" "Create/update roles" "role" "write" "$admin_token"
    create_permission "transaction:create" "Create transactions" "transaction" "create" "$admin_token"
    create_permission "transaction:read" "Read transactions" "transaction" "read" "$admin_token"
    create_permission "ledger:read" "Read ledger entries" "ledger" "read" "$admin_token"
    create_permission "ledger:write" "Write ledger entries" "ledger" "write" "$admin_token"
    create_permission "audit:read" "Read audit logs" "audit" "read" "$admin_token"
    create_permission "kyc:verify" "Perform KYC verification" "kyc" "verify" "$admin_token"
    
    # Step 4: Create test users for different roles
    log_info ""
    log_info "Step 4: Creating test users..."
    
    create_user "agent001" "agent001@bank.com" "AgentPass123!" "Test Agent 001" "$admin_token"
    create_user "operator001" "operator001@bank.com" "OperatorPass123!" "Test Operator 001" "$admin_token"
    create_user "auditor001" "auditor001@bank.com" "AuditorPass123!" "Test Auditor 001" "$admin_token"
    create_user "teller001" "teller001@bank.com" "TellerPass123!" "Test Teller 001" "$admin_token"
    
    # Step 5: Output tokens for E2E tests
    log_info ""
    log_info "Step 5: Getting tokens for test users..."
    
    local agent_token=$(get_token "agent001" "AgentPass123!")
    local operator_token=$(get_token "operator001" "OperatorPass123!")
    local auditor_token=$(get_token "auditor001" "AuditorPass123!")
    local teller_token=$(get_token "teller001" "TellerPass123!")
    
    # Output summary
    echo ""
    echo "============================================="
    echo "  Seed Data Summary"
    echo "============================================="
    echo ""
    echo "Users created:"
    echo "  - admin (IT_ADMIN)"
    echo "  - agent001 (AGENT)"
    echo "  - operator001 (BANK_OPERATOR)"
    echo "  - auditor001 (AUDITOR)"
    echo "  - teller001 (TELLER)"
    echo ""
    echo "Tokens (export these for E2E tests):"
    echo ""
    echo "export ADMIN_TOKEN='$admin_token'"
    echo "export AGENT_TOKEN='$agent_token'"
    echo "export OPERATOR_TOKEN='$operator_token'"
    echo "export AUDITOR_TOKEN='$auditor_token'"
    echo "export TELLER_TOKEN='$teller_token'"
    echo ""
    echo "============================================="
    
    # Save tokens to file for test scripts
    cat > /tmp/e2e_tokens.env << EOF
ADMIN_TOKEN=$admin_token
AGENT_TOKEN=$agent_token
OPERATOR_TOKEN=$operator_token
AUDITOR_TOKEN=$auditor_token
TELLER_TOKEN=$teller_token
EOF
    
    log_success "Tokens saved to /tmp/e2e_tokens.env"
    log_info "Source this file to use tokens: source /tmp/e2e_tokens.env"
}

main
