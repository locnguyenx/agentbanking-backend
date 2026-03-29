#!/bin/bash
# scripts/verify-services-startup.sh
# Tests that all services can start successfully
# Usage: ./scripts/verify-services-startup.sh [service-name]

set -e

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="/tmp/service-startup-logs"
mkdir -p "$LOG_DIR"

# Service configs (name:default_port)
SERVICES=(
  "rules-service:8081"
  "ledger-service:8082"
  "onboarding-service:8083"
  "switch-adapter-service:8084"
  "orchestrator-service:8085"
  "biller-service:8086"
)

# Filter by argument if provided
if [ -n "$1" ]; then
  SERVICES=()
  for svc in "$@"; do
    SERVICES+=("${svc}:${PORT_OFFSET:-8081}")
  done
fi

# Health check endpoint
HEALTH_PATH="/actuator/health"
MAX_WAIT=60  # seconds

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Function to test a single service
test_service() {
  local svc_name=$1
  local port=$2
  local log_file="${LOG_DIR}/${svc_name}.log"
  local pid_file="${LOG_DIR}/${svc_name}.pid"
  
  echo -e "${YELLOW}Testing ${svc_name} on port ${port}...${NC}"
  
  # Kill any existing process on this port
  lsof -ti:${port} | xargs kill -9 2>/dev/null || true
  sleep 1
  
  # Start the service
  cd "${BASE_DIR}"
  ./gradlew :services:${svc_name}:bootRun \
    --no-daemon \
    -Dserver.port=${port} \
    -Dspring.main.allow-bean-definition-overriding=true \
    -Dspring.flyway.enabled=true \
    -Dspring.datasource.url=jdbc:postgresql://localhost:5432/${svc_name//-/_}_db \
    > "${log_file}" 2>&1 &
  
  local gradle_pid=$!
  echo ${gradle_pid} > "${pid_file}"
  
  # Wait for health check
  local waited=0
  while [ $waited -lt $MAX_WAIT ]; do
    # Check if process is still running
    if ! kill -0 ${gradle_pid} 2>/dev/null; then
      echo -e "${RED}✗ ${svc_name}: Process died${NC}"
      echo "  Last 20 lines of log:"
      tail -20 "${log_file}" | sed 's/^/    /'
      return 1
    fi
    
    # Try health check
    if curl -sf "http://localhost:${port}${HEALTH_PATH}" > /dev/null 2>&1; then
      echo -e "${GREEN}✓ ${svc_name}: Healthy${NC}"
      # Save PID for cleanup
      pgrep -P ${gradle_pid} | head -1 > "${pid_file}" 2>/dev/null || echo ${gradle_pid} > "${pid_file}"
      return 0
    fi
    
    # Check for common errors in log
    if grep -q "APPLICATION FAILED TO START" "${log_file}" 2>/dev/null; then
      echo -e "${RED}✗ ${svc_name}: Failed to start${NC}"
      echo "  Error:"
      grep -A5 "APPLICATION FAILED TO START" "${log_file}" | sed 's/^/    /'
      kill -9 ${gradle_pid} 2>/dev/null
      return 1
    fi
    
    sleep 2
    waited=$((waited + 2))
  done
  
  echo -e "${RED}✗ ${svc_name}: Timeout after ${MAX_WAIT}s${NC}"
  echo "  Last 20 lines of log:"
  tail -20 "${log_file}" | sed 's/^/    /'
  kill -9 ${gradle_pid} 2>/dev/null
  return 1
}

# Cleanup function
cleanup() {
  echo ""
  echo "Cleaning up..."
  for svc_info in "${SERVICES[@]}"; do
    local svc_name="${svc_info%%:*}"
    local pid_file="${LOG_DIR}/${svc_name}.pid"
    if [ -f "${pid_file}" ]; then
      local pid=$(cat "${pid_file}")
      kill -9 ${pid} 2>/dev/null || true
      kill -9 $(pgrep -P ${pid}) 2>/dev/null || true
      rm -f "${pid_file}"
    fi
  done
  # Also kill any leftover gradle processes
  pkill -f "bootRun" 2>/dev/null || true
}

# Set up cleanup on exit
trap cleanup EXIT INT TERM

# Main
echo "=========================================="
echo "Testing Service Startup"
echo "=========================================="
echo ""

# Check if PostgreSQL is running
if ! pg_isready -h localhost > /dev/null 2>&1; then
  echo -e "${RED}ERROR: PostgreSQL is not running on localhost${NC}"
  echo "Start PostgreSQL or use Docker: docker-compose up -d postgres-rules postgres-ledger..."
  exit 1
fi

# Build all services first
echo "Building services..."
cd "${BASE_DIR}"
./gradlew :services:rules-service:bootJar \
  :services:ledger-service:bootJar \
  :services:onboarding-service:bootJar \
  :services:switch-adapter-service:bootJar \
  :services:orchestrator-service:bootJar \
  :services:biller-service:bootJar \
  --no-daemon 2>&1 | tail -5

echo ""
echo "Starting services..."
echo ""

# Test each service
FAILED=0
for svc_info in "${SERVICES[@]}"; do
  svc_name="${svc_info%%:*}"
  port="${svc_info##*:}"
  
  if ! test_service "${svc_name}" "${port}"; then
    FAILED=$((FAILED + 1))
  fi
  echo ""
done

# Summary
echo "=========================================="
echo "Summary"
echo "=========================================="
TOTAL=${#SERVICES[@]}
PASSED=$((TOTAL - FAILED))

if [ ${FAILED} -eq 0 ]; then
  echo -e "${GREEN}All ${TOTAL} services started successfully!${NC}"
else
  echo -e "${RED}${FAILED}/${TOTAL} services failed to start${NC}"
  echo ""
  echo "Check logs in: ${LOG_DIR}"
  exit 1
fi
