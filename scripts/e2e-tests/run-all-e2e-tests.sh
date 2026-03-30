#!/bin/bash
#
# Master E2E Test Runner
# Orchestrates: Docker startup → Seed data → Run all BDD tests
#
# Usage: ./run-all-e2e-tests.sh [--skip-docker] [--cleanup]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/common.sh"

SKIP_DOCKER=false
CLEANUP=false

for arg in "$@"; do
    case $arg in
        --skip-docker) SKIP_DOCKER=true ;;
        --cleanup) CLEANUP=true ;;
    esac
done

# ============================================================================
# Docker Management
# ============================================================================

start_docker() {
    if [ "$SKIP_DOCKER" = true ]; then
        log_info "Skipping Docker startup (--skip-docker)"
        return
    fi
    
    log_info "Starting Docker containers..."
    cd "$PROJECT_DIR"
    
    log_info "Building containers..."
    docker compose --profile all build
    
    log_info "Starting services..."
    docker compose --profile all up -d
    
    log_info "Waiting for services to start..."
    sleep 10
    
    local max_attempts=30
    local attempt=1
    while [ $attempt -le $max_attempts ]; do
        if curl -s -o /dev/null http://localhost:8080/actuator/health 2>/dev/null; then
            log_success "Gateway is ready"
            break
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        log_error "Services failed to start"
        docker compose logs gateway
        exit 1
    fi
    
    log_success "All services started"
}

stop_docker() {
    log_info "Stopping Docker containers..."
    cd "$PROJECT_DIR"
    docker compose --profile all down
    log_success "Cleanup complete"
}

# ============================================================================
# Run Test Suite
# ============================================================================

run_test_script() {
    local script="$1"
    local name="$2"
    
    log_info "Running: $name"
    if bash "$SCRIPT_DIR/$script" 2>&1; then
        log_success "$name: PASSED"
        return 0
    else
        log_error "$name: FAILED"
        return 1
    fi
}

# ============================================================================
# Main
# ============================================================================

main() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Agent Banking Platform - Full BDD E2E Test Suite          ║"
    echo "║  179 Scenarios across 18 sections                          ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    
    local start_time=$(date +%s)
    local failed_suites=0
    
    # Clear results file
    > "$TEST_RESULTS_FILE"
    
    # Start Docker
    start_docker
    
    # Seed data
    section "Seeding Test Data"
    bash "$SCRIPT_DIR/seed-test-data.sh"
    
    # Run all BDD section tests
    section "Running BDD Tests"
    
    run_test_script "01-rules-fee-engine.sh" "Section 1: Rules & Fee Engine" || failed_suites=$((failed_suites + 1))
    run_test_script "02-ledger-float.sh" "Section 2: Ledger & Float" || failed_suites=$((failed_suites + 1))
    run_test_script "03-cash-withdrawal.sh" "Section 3: Cash Withdrawal" || failed_suites=$((failed_suites + 1))
    run_test_script "04-cash-deposit.sh" "Section 4: Cash Deposit" || failed_suites=$((failed_suites + 1))
    run_test_script "05-ekyc-onboarding.sh" "Section 5: e-KYC & Onboarding" || failed_suites=$((failed_suites + 1))
    run_test_script "06-bill-payments.sh" "Section 6: Bill Payments" || failed_suites=$((failed_suites + 1))
    run_test_script "07-prepaid-topup.sh" "Section 7: Prepaid Top-up" || failed_suites=$((failed_suites + 1))
    run_test_script "08-duitnow-jompay.sh" "Section 8: DuitNow & JomPAY" || failed_suites=$((failed_suites + 1))
    run_test_script "09-ewallet-essp.sh" "Section 9: e-Wallet & eSSP" || failed_suites=$((failed_suites + 1))
    run_test_script "10-agent-onboarding.sh" "Section 10: Agent Onboarding" || failed_suites=$((failed_suites + 1))
    run_test_script "11-reversals-disputes.sh" "Section 11: Reversals & Disputes" || failed_suites=$((failed_suites + 1))
    run_test_script "12-merchant-services.sh" "Section 12: Merchant Services" || failed_suites=$((failed_suites + 1))
    run_test_script "13-aml-fraud-velocity.sh" "Section 13: AML/Fraud & Velocity" || failed_suites=$((failed_suites + 1))
    run_test_script "14-eod-settlement.sh" "Section 14: EOD Net Settlement" || failed_suites=$((failed_suites + 1))
    run_test_script "15-discrepancy-resolution.sh" "Section 15: Discrepancy Resolution" || failed_suites=$((failed_suites + 1))
    run_test_script "16-api-gateway.sh" "Section 16: API Gateway" || failed_suites=$((failed_suites + 1))
    run_test_script "17-backoffice.sh" "Section 17: Backoffice" || failed_suites=$((failed_suites + 1))
    run_test_script "18-stp-processing.sh" "Section 18: STP Processing" || failed_suites=$((failed_suites + 1))
    
    # Print overall summary
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Final Summary                                              ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "  Duration: ${duration}s"
    echo "  Failed suites: $failed_suites / 18"
    echo ""
    
    # Aggregate results
    local total=0 passed=0 failed=0
    if [ -f "$TEST_RESULTS_FILE" ]; then
        while IFS='|' read -r name t p f s; do
            total=$((total + t))
            passed=$((passed + p))
            failed=$((failed + f))
        done < "$TEST_RESULTS_FILE"
    fi
    
    echo "  Total tests: $total"
    echo -e "  ${GREEN}Passed: $passed${NC}"
    echo -e "  ${RED}Failed: $failed${NC}"
    echo ""
    
    # Cleanup if requested
    if [ "$CLEANUP" = true ]; then
        stop_docker
    fi
    
    if [ $failed_suites -eq 0 ] && [ $failed -eq 0 ]; then
        echo -e "${GREEN}✓ All BDD tests passed!${NC}"
        exit 0
    else
        echo -e "${RED}✗ Some tests failed${NC}"
        exit 1
    fi
}

main "$@"
