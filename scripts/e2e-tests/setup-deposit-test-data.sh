#!/bin/bash
#
# Setup Test Data for Cash Deposit E2E Tests
#
# Creates the complete data chain needed for deposit tests:
#   1. Auth user (agent role) with known UUID
#   2. Agent float record in ledger DB (linked by user UUID)
#   3. JWT token for the user
#
# Why a separate script?
#   - seed-test-data.sh handles all roles generically
#   - Deposit tests require a specific linkage: auth user UUID → ledger agent_float.agent_id
#   - The auth user's userId becomes the JWT's agent_id claim
#   - The ledger service looks up float by this agent_id
#   - So agent_float.agent_id MUST equal the auth user's user_id
#
# Usage:
#   ./scripts/e2e-tests/setup-deposit-test-data.sh [gateway_url]
#   Gateway URL defaults to http://localhost:8080
#
# Prerequisites:
#   - All services running (docker compose --profile all up -d)
#   - jq installed
#   - psql available (or Docker-based fallback)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

GATEWAY_URL="${1:-http://localhost:8080}"

# ============================================================================
# Test Agent Constants (deterministic UUIDs for idempotent seeding)
# ============================================================================

DEPOSIT_AGENT_USER_ID="d1000000-0000-0000-0000-000000000001"
DEPOSIT_AGENT_USERNAME="deposit_agent_01"
DEPOSIT_AGENT_EMAIL="deposit.agent01@e2etest.com"
DEPOSIT_AGENT_PASSWORD="DepositAgent123!"
DEPOSIT_AGENT_FULL_NAME="Cash Deposit Test Agent"
DEPOSIT_AGENT_FLOAT_BALANCE="100000.00"
DEPOSIT_AGENT_CODE="AGT-E2E-D01"

# Onboarding agent data (for agent registry, separate from auth)
DEPOSIT_AGENT_ONBOARDING_ID="a1000000-0000-0000-0000-000000000001"
DEPOSIT_AGENT_MYKAD="880101011234"

# Ledger DB connection (Docker container)
LEDGER_DB_HOST="localhost"
LEDGER_DB_PORT="5434"
LEDGER_DB_USER="postgres"
LEDGER_DB_PASS="postgres"
LEDGER_DB_NAME="ledger_db"

# ============================================================================
# Helper: Execute SQL on ledger DB
# ============================================================================

ledger_sql() {
    local sql="$1"
    local result

    # Try psql directly first, then via Docker
    if command -v psql &> /dev/null; then
        result=$(PGPASSWORD="$LEDGER_DB_PASS" psql -h "$LEDGER_DB_HOST" -p "$LEDGER_DB_PORT" \
            -U "$LEDGER_DB_USER" -d "$LEDGER_DB_NAME" -t -A -c "$sql" 2>/dev/null) || true
    else
        # Fallback: use docker exec
        result=$(docker exec "$(docker ps -q -f name=postgres-ledger)" \
            psql -U "$LEDGER_DB_USER" -d "$LEDGER_DB_NAME" -t -A -c "$sql" 2>/dev/null) || true
    fi

    echo "$result"
}

# ============================================================================
# Helper: Get or create user, return HTTP status
# ============================================================================

bootstrap_deposit_agent() {
    log_info "Creating auth user: $DEPOSIT_AGENT_USERNAME (UUID: $DEPOSIT_AGENT_USER_ID)"

    # Use bootstrap endpoint with explicit UUID in a deterministic way
    # Note: bootstrap endpoint doesn't accept UUID, it generates one.
    # We need the UUID to match the ledger float record.
    # Strategy: create via bootstrap, then check what UUID was assigned.

    local response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/auth/users/bootstrap" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"$DEPOSIT_AGENT_USERNAME\",
            \"email\": \"$DEPOSIT_AGENT_EMAIL\",
            \"password\": \"$DEPOSIT_AGENT_PASSWORD\",
            \"fullName\": \"$DEPOSIT_AGENT_FULL_NAME\"
        }")

    local body=$(get_body "$response")
    local status=$(get_status "$response")

    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_success "Auth user $DEPOSIT_AGENT_USERNAME created"
        echo "$body"
    elif [ "$status" = "409" ]; then
        log_info "Auth user $DEPOSIT_AGENT_USERNAME already exists"
        echo "{}"
    else
        log_error "Failed to create auth user: $status - $body"
        return 1
    fi
}

create_deposit_agent_via_admin() {
    local admin_token="$1"

    log_info "Creating auth user via admin API with known UUID: $DEPOSIT_AGENT_USER_ID"

    local response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/auth/users" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $admin_token" \
        -d "{
            \"userId\": \"$DEPOSIT_AGENT_USER_ID\",
            \"username\": \"$DEPOSIT_AGENT_USERNAME\",
            \"email\": \"$DEPOSIT_AGENT_EMAIL\",
            \"password\": \"$DEPOSIT_AGENT_PASSWORD\",
            \"fullName\": \"$DEPOSIT_AGENT_FULL_NAME\"
        }")

    local body=$(get_body "$response")
    local status=$(get_status "$response")

    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_success "Auth user $DEPOSIT_AGENT_USERNAME created with UUID $DEPOSIT_AGENT_USER_ID"
    elif echo "$body" | grep -qi "already exists\|duplicate\|conflict"; then
        log_info "Auth user $DEPOSIT_AGENT_USERNAME already exists"
    else
        log_warn "Admin create returned $status: $body"
        log_info "Attempting bootstrap fallback..."
        bootstrap_deposit_agent
    fi
}

assign_agent_role() {
    local admin_token="$1"

    log_info "Assigning AGENT role to $DEPOSIT_AGENT_USERNAME"

    # First get the AGENT role ID
    local roles_response=$(api_call "GET" "/auth/roles" "$admin_token")
    local agent_role_id=$(echo "$(get_body "$roles_response")" | jq -r '.[] | select(.roleName == "AGENT") | .roleId // empty' 2>/dev/null)

    if [ -z "$agent_role_id" ]; then
        log_warn "Could not find AGENT role ID, role may already be assigned"
        return 0
    fi

    local response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/auth/users/$DEPOSIT_AGENT_USER_ID/roles" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $admin_token" \
        -d "{\"roleId\": \"$agent_role_id\"}")

    local status=$(get_status "$response")
    if [ "$status" = "200" ] || [ "$status" = "201" ] || [ "$status" = "204" ]; then
        log_success "AGENT role assigned to $DEPOSIT_AGENT_USERNAME"
    else
        log_info "Role assignment status: $status (may already be assigned)"
    fi
}

# ============================================================================
# Main Setup Process
# ============================================================================

main() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Cash Deposit E2E Test Data Setup                         ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    log_info "Gateway URL: $GATEWAY_URL"
    echo ""

    # Wait for services
    wait_for_service "$GATEWAY_URL/actuator/health" "Gateway"

    # ========================================================================
    # Step 1: Ensure base roles/users exist (run seed first if needed)
    # ========================================================================
    section "Step 1: Base Auth Setup"

    # Create admin if not exists
    local admin_bootstrap=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/auth/users/bootstrap" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "admin",
            "email": "admin@agentbanking.com",
            "password": "password",
            "fullName": "System Administrator"
        }')
    local admin_status=$(get_status "$admin_bootstrap")
    if [ "$admin_status" = "200" ] || [ "$admin_status" = "201" ] || [ "$admin_status" = "409" ]; then
        log_success "Admin user ready"
    fi

    # Get admin token
    local admin_token=$(get_token "admin" "password")
    if [ -z "$admin_token" ]; then
        log_error "Cannot authenticate as admin. Run seed-test-data.sh first."
        exit 1
    fi
    log_success "Admin authenticated"

    # Ensure AGENT role exists
    create_role "AGENT" "Field agent for transactions" "$admin_token"

    # ========================================================================
    # Step 2: Create auth user for deposit agent
    # ========================================================================
    section "Step 2: Auth User for Deposit Agent"

    create_deposit_agent_via_admin "$admin_token"
    assign_agent_role "$admin_token"

    # Get the deposit agent token
    local deposit_agent_token=$(get_token "$DEPOSIT_AGENT_USERNAME" "$DEPOSIT_AGENT_PASSWORD")
    if [ -z "$deposit_agent_token" ]; then
        log_error "Failed to get token for $DEPOSIT_AGENT_USERNAME"
        exit 1
    fi
    log_success "Deposit agent token obtained"

    # Verify the user ID from the JWT subject claim
    local token_payload=$(echo "$deposit_agent_token" | cut -d'.'' -f2 | base64 -d 2>/dev/null || echo "{}")
    local actual_user_id=$(echo "$token_payload" | jq -r '.sub // empty' 2>/dev/null)

    if [ -n "$actual_user_id" ] && [ "$actual_user_id" != "null" ]; then
        log_info "JWT subject (user ID): $actual_user_id"

        # If bootstrap assigned a different UUID than our constant, update our reference
        if [ "$actual_user_id" != "$DEPOSIT_AGENT_USER_ID" ]; then
            log_warn "User UUID differs from expected: $actual_user_id vs $DEPOSIT_AGENT_USER_ID"
            log_info "Using actual user UUID: $actual_user_id"
            DEPOSIT_AGENT_USER_ID="$actual_user_id"
        fi
    fi

    # ========================================================================
    # Step 3: Create agent onboarding record (for agent registry)
    # ========================================================================
    section "Step 3: Agent Onboarding Record"

    log_info "Creating agent in onboarding service: $DEPOSIT_AGENT_CODE"

    local onboard_response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/api/v1/onboarding/submit-application" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $deposit_agent_token" \
        -d "{
            \"mykadNumber\": \"$DEPOSIT_AGENT_MYKAD\",
            \"businessName\": \"E2E Deposit Test Shop\",
            \"tier\": \"STANDARD\",
            \"merchantGpsLat\": 3.1390,
            \"merchantGpsLng\": 101.6869,
            \"phoneNumber\": \"012-9999001\"
        }")

    local onboard_status=$(get_status "$onboard_response")
    if [ "$onboard_status" = "200" ] || [ "$onboard_status" = "201" ]; then
        log_success "Agent onboarding submitted"
    else
        log_info "Onboarding status: $onboard_status (may already exist or endpoint not available)"
    fi

    # ========================================================================
    # Step 4: Create float record in ledger DB
    # ========================================================================
    section "Step 4: Ledger Float Record"

    log_info "Creating agent_float record for user UUID: $DEPOSIT_AGENT_USER_ID"

    local float_sql="INSERT INTO agent_float (float_id, agent_id, balance, reserved_balance, currency, version, updated_at)
        SELECT '$DEPOSIT_AGENT_USER_ID', '$DEPOSIT_AGENT_USER_ID', $DEPOSIT_AGENT_FLOAT_BALANCE, 0.00, 'MYR', 0, NOW()
        WHERE NOT EXISTS (SELECT 1 FROM agent_float WHERE agent_id = '$DEPOSIT_AGENT_USER_ID');"

    local float_result=$(ledger_sql "$float_sql")

    # Verify the record was created
    local verify_sql="SELECT balance FROM agent_float WHERE agent_id = '$DEPOSIT_AGENT_USER_ID';"
    local existing_balance=$(ledger_sql "$verify_sql")

    if [ -n "$existing_balance" ]; then
        log_success "Float record exists with balance: MYR $existing_balance"
    else
        log_error "Failed to create float record in ledger DB"
        log_info "Trying alternative: using Docker exec..."

        local container_id=$(docker ps -q -f "name=postgres-ledger" 2>/dev/null | head -1)
        if [ -n "$container_id" ]; then
            docker exec "$container_id" psql -U postgres -d ledger_db -c "$float_sql"
            local verify=$(docker exec "$container_id" psql -U postgres -d ledger_db -t -A -c "$verify_sql")
            if [ -n "$verify" ]; then
                log_success "Float record created via Docker exec (balance: MYR $verify)"
            else
                log_error "Float record creation failed even via Docker"
                exit 1
            fi
        else
            log_error "Cannot find ledger DB container. Is Docker running?"
            exit 1
        fi
    fi

    # ========================================================================
    # Step 5: Save token and agent data
    # ========================================================================
    section "Step 5: Save Configuration"

    # Write deposit-specific test config
    local deposit_config_file="/tmp/e2e_deposit_test.env"
    cat > "$deposit_config_file" << EOF
# Cash Deposit E2E Test Configuration
# Generated: $(date -Iseconds)

DEPOSIT_AGENT_USERNAME=$DEPOSIT_AGENT_USERNAME
DEPOSIT_AGENT_PASSWORD=$DEPOSIT_AGENT_PASSWORD
DEPOSIT_AGENT_USER_ID=$DEPOSIT_AGENT_USER_ID
DEPOSIT_AGENT_TOKEN=$deposit_agent_token
DEPOSIT_AGENT_FLOAT_BALANCE=$DEPOSIT_AGENT_FLOAT_BALANCE
DEPOSIT_AGENT_CODE=$DEPOSIT_AGENT_CODE
EOF

    log_success "Deposit test config saved to $deposit_config_file"

    # Also append to the main token file if it exists
    if [ -f "$TOKEN_FILE" ]; then
        # Remove old deposit entries if any
        grep -v "^DEPOSIT_" "$TOKEN_FILE" > "${TOKEN_FILE}.tmp" 2>/dev/null || true
        cat >> "${TOKEN_FILE}.tmp" << EOF
DEPOSIT_AGENT_TOKEN=$deposit_agent_token
DEPOSIT_AGENT_USER_ID=$DEPOSIT_AGENT_USER_ID
DEPOSIT_AGENT_USERNAME=$DEPOSIT_AGENT_USERNAME
EOF
        mv "${TOKEN_FILE}.tmp" "$TOKEN_FILE"
        log_success "Tokens appended to $TOKEN_FILE"
    fi

    # ========================================================================
    # Summary
    # ========================================================================
    section "Setup Complete"
    echo ""
    echo "  Auth User:     $DEPOSIT_AGENT_USERNAME"
    echo "  User UUID:     $DEPOSIT_AGENT_USER_ID"
    echo "  Agent Code:    $DEPOSIT_AGENT_CODE"
    echo "  Float Balance: MYR $DEPOSIT_AGENT_FLOAT_BALANCE"
    echo "  Token File:    $deposit_config_file"
    echo ""
    echo "  To run deposit tests:"
    echo "    source $deposit_config_file"
    echo "    ./scripts/e2e-tests/04-cash-deposit-e2e.sh"
    echo ""
    echo "  Or run the full flow:"
    echo "    ./scripts/e2e-tests/setup-deposit-test-data.sh && \\"
    echo "    ./scripts/e2e-tests/04-cash-deposit-e2e.sh"
    echo ""
}

main "$@"
