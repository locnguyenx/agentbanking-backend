#!/bin/bash
#
# BDD Section 17: Backoffice E2E Tests
# Tests agent management, dashboards, settlement reports, audit logs, and configuration

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 17: Backoffice"

# ============================================================================
# BDD-BO01: Create New Agent
# ============================================================================

subsection "BDD-BO01: Create new agent"
IDEMPOTENCY_KEY=$(generate_uuid)
AGENT_CODE="AGT_$(generate_uuid | cut -c1-8)"
response=$(api_call "POST" "/api/v1/backoffice/agents" "$ADMIN_TOKEN" "{
  \"agentCode\": \"$AGENT_CODE\",
  \"agentName\": \"Test Agent Store\",
  \"agentType\": \"RETAIL\",
  \"tier\": \"TIER_1\",
  \"address\": \"123 Jalan Test, Kuala Lumpur\",
  \"contactPerson\": \"Test User\",
  \"contactPhone\": \"60123456789\",
  \"floatLimit\": 50000.00,
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Create agent" "201" "$(get_status "$response")"
assert_json_field_exists "Agent ID" "$(get_body "$response")" ".agentId"
assert_json_field "Agent code" "$(get_body "$response")" ".agentCode" "$AGENT_CODE"
assert_json_field "Agent status" "$(get_body "$response")" ".status" "ACTIVE"

# ============================================================================
# BDD-BO01-EC-01: Duplicate Agent
# ============================================================================

subsection "BDD-BO01-EC-01: Duplicate agent"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/agents" "$ADMIN_TOKEN" "{
  \"agentCode\": \"AGENT_001\",
  \"agentName\": \"Duplicate Agent\",
  \"agentType\": \"RETAIL\",
  \"tier\": \"TIER_1\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Duplicate agent rejected" "409" "$(get_status "$response")"
assert_json_field "Duplicate error" "$(get_body "$response")" ".error.code" "ERR_BIZ_AGENT_EXISTS"

# ============================================================================
# BDD-BO01-EC-02: Deactivate with Pending Transactions
# ============================================================================

subsection "BDD-BO01-EC-02: Deactivate with pending transactions"
IDEMPOTENCY_KEY=$(generate_uuid)
api_call "POST" "/api/v1/withdrawal" "$AGENT_TOKEN" "{
  \"amount\": 50.00,
  \"currency\": \"MYR\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\",
  \"customerCard\": \"4111111111111111\",
  \"customerPin\": \"TIMEOUT\",
  \"location\": {\"latitude\": 3.1390, \"longitude\": 101.6869}
}" > /dev/null 2>&1

IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "PATCH" "/api/v1/backoffice/agents/AGENT_001" "$ADMIN_TOKEN" "{
  \"status\": \"INACTIVE\",
  \"reason\": \"Deactivating agent\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "409" ]; then
    assert_json_field "Pending txn error" "$(get_body "$response")" ".error.code" "ERR_BIZ_PENDING_TRANSACTIONS"
else
    assert_status "Deactivate blocked" "409" "$actual_status"
fi

# ============================================================================
# BDD-BO01-EC-03: Update Agent Tier
# ============================================================================

subsection "BDD-BO01-EC-03: Update agent tier"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "PATCH" "/api/v1/backoffice/agents/AGENT_001" "$ADMIN_TOKEN" "{
  \"tier\": \"TIER_2\",
  \"reason\": \"Performance upgrade\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Update agent tier" "200" "$(get_status "$response")"
assert_json_field "Updated tier" "$(get_body "$response")" ".tier" "TIER_2"

# ============================================================================
# BDD-BO02: Transaction Monitoring Dashboard
# ============================================================================

subsection "BDD-BO02: Transaction monitoring dashboard"
response=$(api_call "GET" "/api/v1/backoffice/dashboard" "$ADMIN_TOKEN")
assert_status "Dashboard load" "200" "$(get_status "$response")"
assert_json_field_exists "Total transactions" "$(get_body "$response")" ".totalTransactions"
assert_json_field_exists "Total volume" "$(get_body "$response")" ".totalVolume"
assert_json_field_exists "Active agents" "$(get_body "$response")" ".activeAgents"

# ============================================================================
# BDD-BO02-EC-01: Real-Time Updates
# ============================================================================

subsection "BDD-BO02-EC-01: Real-time updates"
response=$(api_call "GET" "/api/v1/backoffice/dashboard/realtime" "$ADMIN_TOKEN")
assert_status "Real-time dashboard" "200" "$(get_status "$response")"
assert_json_field_exists "Live transactions" "$(get_body "$response")" ".liveTransactionCount"
assert_json_field_exists "Active terminals" "$(get_body "$response")" ".activeTerminals"

# ============================================================================
# BDD-BO03: Settlement Report
# ============================================================================

subsection "BDD-BO03: Settlement report"
response=$(api_call "GET" "/api/v1/backoffice/settlement?date=$(date +%Y-%m-%d)&agentId=AGENT_001" "$ADMIN_TOKEN")
assert_status "Settlement report" "200" "$(get_status "$response")"
assert_json_field_exists "Settlement data" "$(get_body "$response")" ".settlements"
assert_json_field_exists "Total amount" "$(get_body "$response")" ".totalNetAmount"

# ============================================================================
# BDD-BO03-EC-01: CSV Export
# ============================================================================

subsection "BDD-BO03-EC-01: CSV export"
response=$(curl -s -w '\n%{http_code}' -X GET \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Accept: text/csv" \
    "$GATEWAY_URL/api/v1/backoffice/settlement/export?date=$(date +%Y-%m-%d)")
assert_status "CSV export" "200" "$(get_status "$response")"
assert_contains "CSV content type" "$(get_body "$response")" "agentId,settlementDate"

# ============================================================================
# BDD-BO04: Update Fee Configuration
# ============================================================================

subsection "BDD-BO04: Update fee configuration"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "PUT" "/api/v1/backoffice/fees" "$ADMIN_TOKEN" "{
  \"feeCode\": \"WD_FEE\",
  \"feeName\": \"Withdrawal Fee\",
  \"feeType\": \"PERCENTAGE\",
  \"feeValue\": 1.50,
  \"minFee\": 0.50,
  \"maxFee\": 10.00,
  \"effectiveDate\": \"$(date +%Y-%m-%d)\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Update fee config" "200" "$(get_status "$response")"
assert_json_field "Fee code" "$(get_body "$response")" ".feeCode" "WD_FEE"
assert_json_field "Fee value" "$(get_body "$response")" ".feeValue" "1.50"

# ============================================================================
# BDD-BO05: View Audit Logs
# ============================================================================

subsection "BDD-BO05: View audit logs"
response=$(api_call "GET" "/api/v1/backoffice/audit-logs?page=0&size=20" "$ADMIN_TOKEN")
assert_status "Audit logs" "200" "$(get_status "$response")"
assert_json_field_exists "Log entries" "$(get_body "$response")" ".content"
assert_json_field_exists "Total count" "$(get_body "$response")" ".totalElements"

# ============================================================================
# BDD-BO05-EC-01: Audit Log Search
# ============================================================================

subsection "BDD-BO05-EC-01: Audit log search"
response=$(api_call "GET" "/api/v1/backoffice/audit-logs?search=withdrawal&startDate=$(date +%Y-%m-%d)&endDate=$(date +%Y-%m-%d)&entityType=TRANSACTION" "$ADMIN_TOKEN")
assert_status "Audit log search" "200" "$(get_status "$response")"
assert_json_field_exists "Search results" "$(get_body "$response")" ".content"

print_summary "Backoffice Tests"
