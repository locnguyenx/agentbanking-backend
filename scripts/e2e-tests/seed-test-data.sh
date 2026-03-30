#!/bin/bash
#
# E2E Test Seed Data Script
# Seeds comprehensive test data for all services
#
# Usage: ./seed-test-data.sh [gateway_url]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

GATEWAY_URL="${1:-http://localhost:8080}"

# ============================================================================
# Seed Functions
# ============================================================================

create_user_via_bootstrap() {
    local username="$1"
    local email="$2"
    local password="$3"
    local fullName="$4"
    
    log_info "Creating user via bootstrap: $username"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/auth/users/bootstrap" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"$username\",
            \"email\": \"$email\",
            \"password\": \"$password\",
            \"fullName\": \"$fullName\"
        }")
    
    local body=$(get_body "$response")
    local status=$(get_status "$response")
    
    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_success "User $username created via bootstrap"
    elif [ "$status" = "409" ]; then
        log_info "User $username already exists"
    else
        log_error "Failed to create user $username: $status - $body"
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
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/auth/users" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $admin_token" \
        -d "{
            \"username\": \"$username\",
            \"email\": \"$email\",
            \"password\": \"$password\",
            \"fullName\": \"$fullName\"
        }")
    
    local body=$(get_body "$response")
    local status=$(get_status "$response")
    
    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_success "User $username created"
    elif echo "$body" | grep -q "already exists"; then
        log_info "User $username already exists"
    else
        log_error "Failed to create user $username: $status - $body"
    fi
}

create_role() {
    local role_name="$1"
    local description="$2"
    local admin_token="$3"
    
    log_info "Creating role: $role_name"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/auth/roles" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $admin_token" \
        -d "{
            \"roleName\": \"$role_name\",
            \"description\": \"$description\"
        }")
    
    local status=$(get_status "$response")
    
    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_success "Role $role_name created"
    else
        log_info "Role $role_name may already exist"
    fi
}

create_permission() {
    local key="$1"
    local description="$2"
    local resource="$3"
    local action="$4"
    local admin_token="$5"
    
    log_info "Creating permission: $key"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/auth/permissions" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $admin_token" \
        -d "{
            \"permissionKey\": \"$key\",
            \"description\": \"$description\",
            \"resource\": \"$resource\",
            \"action\": \"$action\"
        }")
    
    local status=$(get_status "$response")
    
    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_success "Permission $key created"
    else
        log_info "Permission $key may already exist"
    fi
}

# ============================================================================
# Main Seed Process
# ============================================================================

main() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Agent Banking Platform - E2E Test Data Seeding            ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    log_info "Gateway URL: $GATEWAY_URL"
    echo ""
    
    # Wait for services
    wait_for_service "$GATEWAY_URL/actuator/health" "Gateway"
    
    # ========================================================================
    # 1. Auth/IAM Setup
    # ========================================================================
    section "1. Auth/IAM Setup"
    
    # Create admin via bootstrap
    create_user_via_bootstrap "admin" "admin@agentbanking.com" "AdminPass123!" "System Administrator"
    
    # Get admin token
    local admin_token=$(get_token "admin" "AdminPass123!")
    
    if [ -z "$admin_token" ]; then
        log_error "Failed to get admin token"
        exit 1
    fi
    
    log_success "Admin authenticated"
    
    # Create roles
    subsection "Creating roles"
    create_role "IT_ADMIN" "IT Administrator" "$admin_token"
    create_role "BANK_OPERATOR" "Bank operations staff" "$admin_token"
    create_role "AGENT" "Field agent" "$admin_token"
    create_role "AUDITOR" "Read-only auditor" "$admin_token"
    create_role "TELLER" "Branch teller" "$admin_token"
    create_role "COMPLIANCE_OFFICER" "Compliance review" "$admin_token"
    create_role "MAKER" "Discrepancy investigation" "$admin_token"
    create_role "CHECKER" "Discrepancy approval" "$admin_token"
    create_role "SUPERVISOR" "Operations supervisor" "$admin_token"
    
    # Create permissions
    subsection "Creating permissions"
    create_permission "user:read" "Read users" "user" "read" "$admin_token"
    create_permission "user:write" "Write users" "user" "write" "$admin_token"
    create_permission "user:delete" "Delete users" "user" "delete" "$admin_token"
    create_permission "role:read" "Read roles" "role" "read" "$admin_token"
    create_permission "role:write" "Write roles" "role" "write" "$admin_token"
    create_permission "transaction:create" "Create transactions" "transaction" "create" "$admin_token"
    create_permission "transaction:read" "Read transactions" "transaction" "read" "$admin_token"
    create_permission "ledger:read" "Read ledger" "ledger" "read" "$admin_token"
    create_permission "ledger:write" "Write ledger" "ledger" "write" "$admin_token"
    create_permission "audit:read" "Read audit" "audit" "read" "$admin_token"
    create_permission "kyc:verify" "Verify KYC" "kyc" "verify" "$admin_token"
    create_permission "agent:manage" "Manage agents" "agent" "manage" "$admin_token"
    create_permission "settlement:read" "Read settlement" "settlement" "read" "$admin_token"
    create_permission "discrepancy:investigate" "Investigate" "discrepancy" "investigate" "$admin_token"
    create_permission "discrepancy:approve" "Approve resolution" "discrepancy" "approve" "$admin_token"
    
    # Create test users
    subsection "Creating test users"
    create_user "agent001" "agent001@bank.com" "AgentPass123!" "Test Agent" "$admin_token"
    create_user "operator001" "operator001@bank.com" "OperatorPass123!" "Test Operator" "$admin_token"
    create_user "auditor001" "auditor001@bank.com" "AuditorPass123!" "Test Auditor" "$admin_token"
    create_user "teller001" "teller001@bank.com" "TellerPass123!" "Test Teller" "$admin_token"
    create_user "maker001" "maker001@bank.com" "MakerPass123!" "Test Maker" "$admin_token"
    create_user "checker001" "checker001@bank.com" "CheckerPass123!" "Test Checker" "$admin_token"
    create_user "compliance001" "compliance001@bank.com" "CompliancePass123!" "Test Compliance" "$admin_token"
    create_user "supervisor001" "supervisor001@bank.com" "SupervisorPass123!" "Test Supervisor" "$admin_token"
    
    # Get tokens for all users
    subsection "Getting tokens"
    local agent_token=$(get_token "agent001" "AgentPass123!")
    local operator_token=$(get_token "operator001" "OperatorPass123!")
    local auditor_token=$(get_token "auditor001" "AuditorPass123!")
    local teller_token=$(get_token "teller001" "TellerPass123!")
    local maker_token=$(get_token "maker001" "MakerPass123!")
    local checker_token=$(get_token "checker001" "CheckerPass123!")
    local compliance_token=$(get_token "compliance001" "CompliancePass123!")
    local supervisor_token=$(get_token "supervisor001" "SupervisorPass123!")
    
    # Save tokens
    cat > "$TOKEN_FILE" << EOF
ADMIN_TOKEN=$admin_token
AGENT_TOKEN=$agent_token
OPERATOR_TOKEN=$operator_token
AUDITOR_TOKEN=$auditor_token
TELLER_TOKEN=$teller_token
MAKER_TOKEN=$maker_token
CHECKER_TOKEN=$checker_token
COMPLIANCE_TOKEN=$compliance_token
SUPERVISOR_TOKEN=$supervisor_token
MICRO_AGENT_ID=AGT-01
STANDARD_AGENT_ID=AGT-02
PREMIER_AGENT_ID=AGT-03
EOF
    
    log_success "Tokens saved to $TOKEN_FILE"
    
    # ========================================================================
    # 2. Summary
    # ========================================================================
    section "2. Seed Complete"
    echo ""
    echo "Users: admin, agent001, operator001, auditor001, teller001,"
    echo "       maker001, checker001, compliance001, supervisor001"
    echo ""
    echo "Agents: AGT-01 (MICRO), AGT-02 (PREMIER), AGT-03 (STANDARD)"
    echo ""
    echo "Tokens: source $TOKEN_FILE"
    echo ""
}

main "$@"
