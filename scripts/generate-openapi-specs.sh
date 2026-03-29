#!/bin/bash
# scripts/generate-openapi-specs.sh
# Generates OpenAPI spec from API Gateway

set -e

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${BASE_DIR}/docs/api"
mkdir -p "${OUTPUT_DIR}"

GATEWAY_PORT=8080

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo "============================================"
echo "OpenAPI Spec Generator (from Gateway)"
echo "============================================"
echo ""

echo -n "Checking gateway (port ${GATEWAY_PORT})... "

if curl -sf "http://localhost:${GATEWAY_PORT}/v3/api-docs" > /dev/null 2>&1; then
  echo -e "${GREEN}OK${NC}"
  
  # Fetch spec from gateway
  echo -n "Fetching OpenAPI spec... "
  
  if curl -sf "http://localhost:${GATEWAY_PORT}/v3/api-docs" > "${OUTPUT_DIR}/openapi.json" 2>/dev/null; then
    echo -e "${GREEN}OK${NC}"
    
    # Convert to YAML
    echo -n "Converting to YAML... "
    
    if command -v yq > /dev/null 2>&1; then
      yq -P '.' "${OUTPUT_DIR}/openapi.json" > "${OUTPUT_DIR}/openapi.yaml"
      echo -e "${GREEN}OK${NC}"
    elif python3 -c "import yaml" 2>/dev/null; then
      python3 -c "import json, yaml; data = json.load(open('${OUTPUT_DIR}/openapi.json')); yaml.dump(data, open('${OUTPUT_DIR}/openapi.yaml', 'w'), default_flow_style=False, sort_keys=False)"
      echo -e "${GREEN}OK${NC}"
    else
      echo -e "${YELLOW}Skipped - install yq or PyYAML${NC}"
    fi
  else
    echo -e "${RED}Failed - gateway may not have /v3/api-docs endpoint${NC}"
    exit 1
  fi
else
  echo -e "${RED}Gateway not running${NC}"
  echo ""
  echo "Start gateway first:"
  echo "  docker-compose --profile infra --profile backend --profile gateway up -d"
  exit 1
fi

echo ""
echo "Done!"
