#!/bin/bash
# Simple JWT token generator using HMAC-SHA512

SECRET="your-super-secret-jwt-key-change-in-production-minimum-32-chars-long"
AGENT_ID="a0000000-0000-0000-0000-000000000001"
SUBJECT="be581cb8-62b0-414f-9826-b539ee40dc4e"

# Create header
HEADER=$(echo -n '{"alg":"HS512","typ":"JWT"}' | base64 -w 0 | tr '+/' '-_' | tr -d '=')

# Create payload with current timestamps
IAT=$(date +%s)
EXP=$((IAT + 3600))
JTI=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || echo "test-$(date +%s)")

PAYLOAD=$(echo -n "{\"agent_id\":\"$AGENT_ID\",\"permissions\":[],\"fullName\":\"Test Agent\",\"email\":\"agent001@bank.com\",\"sub\":\"$SUBJECT\",\"iat\":$IAT,\"exp\":$EXP,\"jti\":\"$JTI\"}" | base64 -w 0 | tr '+/' '-_' | tr -d '=')

# Create signature (using openssl)
SIGNATURE=$(echo -n "$HEADER.$PAYLOAD" | openssl dgst -sha512 -hmac "$SECRET" -binary | base64 -w 0 | tr '+/' '-_' | tr -d '=')

# Combine to form JWT
echo "$HEADER.$PAYLOAD.$SIGNATURE"
