name: rules-service
description: Contract for Rules Service Velocity Check API
version: 1.0.0

request:
  method: POST
  url: /internal/rules/check-velocity
  body:
    agentId: "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
    amount: 500.00
    customerMykad: "700101012345"
    
response:
  status: 200
  body:
    passed: true
    errorCode: null
  headers:
    Content-Type: application/json
    
---
    
name: rules-service
description: Contract for Rules Service Velocity Check - Exceeded
request:
  method: POST
  url: /internal/rules/check-velocity
  body:
    agentId: "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
    amount: 50000.00
    customerMykad: "700101012345"
    
response:
  status: 200
  body:
    passed: false
    errorCode: "ERR_VELOCITY_AMOUNT_EXCEEDED"
  headers:
    Content-Type: application/json