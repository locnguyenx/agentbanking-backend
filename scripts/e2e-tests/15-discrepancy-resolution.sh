#!/bin/bash
#
# BDD Section 15: Discrepancy Resolution E2E Tests
# Tests Maker-Checker workflow for ghost, orphan, and mismatch discrepancies

source "$(dirname "$0")/common.sh"
load_tokens

section "BDD Section 15: Discrepancy Resolution"

# ============================================================================
# BDD-DR01: Maker Investigates Ghost Transaction
# ============================================================================

subsection "BDD-DR01: Maker investigates ghost transaction"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_001/maker-action" "$MAKER_TOKEN" "{
  \"caseId\": \"DCASE_001\",
  \"action\": \"INVESTIGATE\",
  \"reasonCode\": \"GHOST_TXN\",
  \"remarks\": \"Transaction exists in ledger but not in switch - investigating root cause\",
  \"evidence\": [{\"type\": \"SCREENSHOT\", \"reference\": \"EVIDENCE_001\"}],
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Ghost investigation" "200" "$(get_status "$response")"
assert_json_field "Case status" "$(get_body "$response")" ".status" "UNDER_INVESTIGATION"
assert_json_field_exists "Case ID" "$(get_body "$response")" ".caseId"

# ============================================================================
# BDD-DR01-EC-01: Missing Reason Code
# ============================================================================

subsection "BDD-DR01-EC-01: Missing reason code"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_002/maker-action" "$MAKER_TOKEN" "{
  \"caseId\": \"DCASE_002\",
  \"action\": \"INVESTIGATE\",
  \"remarks\": \"Investigating without reason code\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Missing reason code" "400" "$(get_status "$response")"
assert_json_field "Validation error" "$(get_body "$response")" ".error.code" "ERR_VAL_MISSING_REASON_CODE"

# ============================================================================
# BDD-DR01-EC-02: Missing Evidence
# ============================================================================

subsection "BDD-DR01-EC-02: Missing evidence"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_003/maker-action" "$MAKER_TOKEN" "{
  \"caseId\": \"DCASE_003\",
  \"action\": \"INVESTIGATE\",
  \"reasonCode\": \"GHOST_TXN\",
  \"remarks\": \"No evidence attached\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Missing evidence" "400" "$(get_status "$response")"
assert_json_field "Evidence required" "$(get_body "$response")" ".error.code" "ERR_VAL_MISSING_EVIDENCE"

# ============================================================================
# BDD-DR02: Checker Approves Ghost Resolution
# ============================================================================

subsection "BDD-DR02: Checker approves ghost resolution"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_001/checker-approve" "$CHECKER_TOKEN" "{
  \"caseId\": \"DCASE_001\",
  \"action\": \"APPROVE\",
  \"resolution\": \"REVERSE_GHOST\",
  \"remarks\": \"Ghost transaction confirmed, initiating reversal\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Ghost resolution approval" "200" "$(get_status "$response")"
assert_json_field "Resolution status" "$(get_body "$response")" ".status" "RESOLVED"
assert_json_field "Resolution type" "$(get_body "$response")" ".resolution" "REVERSE_GHOST"

# ============================================================================
# BDD-DR02-ORPHAN: Checker Approves Orphan Resolution
# ============================================================================

subsection "BDD-DR02-ORPHAN: Checker approves orphan resolution"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_ORPHAN/maker-action" "$MAKER_TOKEN" "{
  \"caseId\": \"DCASE_ORPHAN\",
  \"action\": \"INVESTIGATE\",
  \"reasonCode\": \"ORPHAN_TXN\",
  \"remarks\": \"Transaction in switch but not in ledger - orphan identified\",
  \"evidence\": [{\"type\": \"LOG\", \"reference\": \"SWITCH_LOG_001\"}],
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Orphan investigation" "200" "$(get_status "$response")"

IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_ORPHAN/checker-approve" "$CHECKER_TOKEN" "{
  \"caseId\": \"DCASE_ORPHAN\",
  \"action\": \"APPROVE\",
  \"resolution\": \"POST_ORPHAN\",
  \"remarks\": \"Orphan transaction to be posted to ledger\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Orphan resolution approval" "200" "$(get_status "$response")"
assert_json_field "Resolution status" "$(get_body "$response")" ".status" "RESOLVED"
assert_json_field "Resolution type" "$(get_body "$response")" ".resolution" "POST_ORPHAN"

# ============================================================================
# BDD-DR02-EC-01: Checker Rejects
# ============================================================================

subsection "BDD-DR02-EC-01: Checker rejects"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_004/maker-action" "$MAKER_TOKEN" "{
  \"caseId\": \"DCASE_004\",
  \"action\": \"INVESTIGATE\",
  \"reasonCode\": \"GHOST_TXN\",
  \"remarks\": \"Initial investigation\",
  \"evidence\": [{\"type\": \"SCREENSHOT\", \"reference\": \"EVIDENCE_004\"}],
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Investigation for rejection" "200" "$(get_status "$response")"

IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_004/checker-reject" "$CHECKER_TOKEN" "{
  \"caseId\": \"DCASE_004\",
  \"action\": \"REJECT\",
  \"remarks\": \"Insufficient investigation - additional evidence required\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Checker rejection" "200" "$(get_status "$response")"
assert_json_field "Rejection status" "$(get_body "$response")" ".status" "REJECTED"

# ============================================================================
# BDD-DR03: Four-Eyes Principle
# ============================================================================

subsection "BDD-DR03: Four-eyes principle"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_005/maker-action" "$MAKER_TOKEN" "{
  \"caseId\": \"DCASE_005\",
  \"action\": \"INVESTIGATE\",
  \"reasonCode\": \"GHOST_TXN\",
  \"remarks\": \"Investigated by maker\",
  \"evidence\": [{\"type\": \"SCREENSHOT\", \"reference\": \"EVIDENCE_005\"}],
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Four-eyes maker action" "200" "$(get_status "$response")"

IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_005/checker-approve" "$MAKER_TOKEN" "{
  \"caseId\": \"DCASE_005\",
  \"action\": \"APPROVE\",
  \"resolution\": \"REVERSE_GHOST\",
  \"remarks\": \"Self-approval attempt\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "403" ]; then
    assert_json_field "Self-approval blocked" "$(get_body "$response")" ".error.code" "ERR_BIZ_SELF_APPROVAL_PROHIBITED"
else
    assert_status "Four-eyes principle" "403" "$actual_status"
fi

# ============================================================================
# BDD-DR03-EC-01: Different User from Same Department
# ============================================================================

subsection "BDD-DR03-EC-01: Different user from same department"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_006/maker-action" "$MAKER_TOKEN" "{
  \"caseId\": \"DCASE_006\",
  \"action\": \"INVESTIGATE\",
  \"reasonCode\": \"GHOST_TXN\",
  \"remarks\": \"Investigated by maker from ops\",
  \"evidence\": [{\"type\": \"SCREENSHOT\", \"reference\": \"EVIDENCE_006\"}],
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Same dept maker action" "200" "$(get_status "$response")"

IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_006/checker-approve" "$SAME_DEPT_CHECKER_TOKEN" "{
  \"caseId\": \"DCASE_006\",
  \"action\": \"APPROVE\",
  \"resolution\": \"REVERSE_GHOST\",
  \"remarks\": \"Same department approval attempt\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
actual_status=$(get_status "$response")
if [ "$actual_status" = "403" ]; then
    assert_json_field "Same dept blocked" "$(get_body "$response")" ".error.code" "ERR_BIZ_SAME_DEPARTMENT_APPROVAL"
else
    assert_status "Same department approval" "403" "$actual_status"
fi

# ============================================================================
# BDD-DR01-MISMATCH: Mismatch Resolution
# ============================================================================

subsection "BDD-DR01-MISMATCH: Mismatch resolution"
IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_MISMATCH/maker-action" "$MAKER_TOKEN" "{
  \"caseId\": \"DCASE_MISMATCH\",
  \"action\": \"INVESTIGATE\",
  \"reasonCode\": \"AMOUNT_MISMATCH\",
  \"remarks\": \"Ledger shows RM100 but switch shows RM95 - currency conversion issue\",
  \"evidence\": [{\"type\": \"LOG\", \"reference\": \"SWITCH_LOG_099\"}, {\"type\": \"SCREENSHOT\", \"reference\": \"LEDGER_SCREEN_099\"}],
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Mismatch investigation" "200" "$(get_status "$response")"
assert_json_field "Mismatch reason" "$(get_body "$response")" ".reasonCode" "AMOUNT_MISMATCH"

IDEMPOTENCY_KEY=$(generate_uuid)
response=$(api_call "POST" "/api/v1/backoffice/discrepancy/DCASE_MISMATCH/checker-approve" "$CHECKER_TOKEN" "{
  \"caseId\": \"DCASE_MISMATCH\",
  \"action\": \"APPROVE\",
  \"resolution\": \"ADJUST_LEDGER\",
  \"remarks\": \"Adjust ledger to match switch amount\",
  \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
}")
assert_status "Mismatch resolution" "200" "$(get_status "$response")"
assert_json_field "Resolution status" "$(get_body "$response")" ".status" "RESOLVED"
assert_json_field "Resolution type" "$(get_body "$response")" ".resolution" "ADJUST_LEDGER"

print_summary "Discrepancy Resolution Tests"
