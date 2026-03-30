#!/bin/bash
#
# Master E2E Test Runner
# Orchestrates: Docker startup → Seed data → Run tests
#
# Usage: ./run-all-e2e-tests.sh [--skip-docker]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

SKIP_DOCKER=false
if [ "$1" = "--skip-docker" ]; then
    SKIP_DOCKER=true
fi

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ============================================================================
# Step 1: Start Docker containers
# ============================================================================

start_docker() {
    if [ "$SKIP_DOCKER" = true ]; then
        log_info "Skipping Docker startup (--skip-docker)"
        return
    fi
    
    log_info "Starting Docker containers..."
    cd "$PROJECT_DIR"
    
    # Build first
    log_info "Building containers..."
    docker compose --profile all build
    
    # Start services
    log_info "Starting services..."
    docker compose --profile all up -d
    
    # Wait for services to be healthy
    log_info "Waiting for services to start..."
    sleep 10
    
    # Check auth service specifically
    local max_attempts=30
    local attempt=1
    while [ $attempt -le $max_attempts ]; do
        if curl -s -o /dev/null http://localhost:8087/actuator/health 2>/dev/null; then
            log_success "Auth/IAM service is ready"
            break
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        log_error "Auth/IAM service failed to start"
        docker compose logs auth-iam-service
        exit 1
    fi
    
    log_success "All services started"
}

# ============================================================================
# Step 2: Seed test data
# ============================================================================

seed_data() {
    log_info "Seeding test data..."
    cd "$SCRIPT_DIR"
    bash ./seed-test-data.sh http://localhost:8080
}

# ============================================================================
# Step 3: Run E2E tests
# ============================================================================

run_tests() {
    log_info "Running E2E tests..."
    cd "$SCRIPT_DIR"
    
    # Run basic E2E tests
    log_info ""
    log_info "=== Phase 1: Basic API Gateway Tests ==="
    bash ./run-e2e-tests.sh http://localhost:8080
    
    # Run BDD scenario tests
    log_info ""
    log_info "=== Phase 2: BDD Scenario Tests ==="
    bash ./bdd-e2e-tests.sh http://localhost:8080
}

# ============================================================================
# Step 4: Cleanup (optional)
# ============================================================================

cleanup() {
    if [ "$1" = "--cleanup" ]; then
        log_info "Stopping Docker containers..."
        cd "$PROJECT_DIR"
        docker compose --profile all down
        log_success "Cleanup complete"
    fi
}

# ============================================================================
# Main
# ============================================================================

main() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Agent Banking Platform - Full E2E Test Suite             ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    
    local start_time=$(date +%s)
    
    # Run steps
    start_docker
    seed_data
    run_tests
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  E2E Test Suite Complete                                   ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "  Duration: ${duration}s"
    echo ""
    
    # Cleanup if requested
    if [ "$2" = "--cleanup" ]; then
        cleanup
    fi
}

main "$@"
