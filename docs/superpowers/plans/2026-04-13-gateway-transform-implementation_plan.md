# Implementation Plan: Pending Project Issues

## Goal Description
Resolve remaining architecture and payload issues using explicitly defined line-by-line changes. All legacy/deprecated APIs are ignored per instruction. The primary route for transactions (`/api/v1/transactions`) will be bridged using a dynamically resolved payload proxy in the API Gateway. 

## Proposed Changes

### Component: Hexagonal Architecture Enforcement
`HexagonalArchitectureTest` already exists across the services. A custom Gradle task is unnecessary—we will enforce it via Git Hooks natively.

**File:** [.git/hooks/pre-commit](file:///Users/me/myprojects/agentbanking-backend/.git/hooks/pre-commit) (New File)
```bash
#!/bin/bash
echo "Verifying Hexagonal Architecture Constraints..."
./gradlew test --tests "*HexagonalArchitectureTest*" --parallel
if [ $? -ne 0 ]; then
  echo "Architecture violation detected in domain/. Committing blocked."
  exit 1
fi
```
*(Make sure to run `chmod +x .git/hooks/pre-commit` after creation)*

### Component: API Gateway - Fix Payload Mutation
The [RequestTransformGatewayFilterFactory](file:///Users/me/myprojects/agentbanking-backend/gateway/src/main/java/com/agentbanking/gateway/filter/RequestTransformGatewayFilterFactory.java#43-628) is inadvertently dropping `transactionType`, [pan](file:///Users/me/myprojects/agentbanking-backend/gateway/src/test/java/com/agentbanking/gateway/integration/gateway/ExternalApiIntegrationTest.java#402-422), and `pinBlock` when transforming orchestrator requests. We will modify it to properly preserve the original payload while injecting `agentId` and `agentTier`.

#### [MODIFY] [RequestTransformGatewayFilterFactory.java](file:///Users/me/myprojects/agentbanking-backend/gateway/src/main/java/com/agentbanking/gateway/filter/RequestTransformGatewayFilterFactory.java)

- Update [transformRequest](file:///Users/me/myprojects/agentbanking-backend/gateway/src/main/java/com/agentbanking/gateway/filter/RequestTransformGatewayFilterFactory.java#197-231) to handle `orchestrator` as a pass-through with header injection.
- Ensure `transactionType` remains in the payload.

### Component: Orchestrator Service - Fix Workflow Bug
Transient `DataConverterException` errors are caused by hardcoded `"DEFAULT"` strings being passed to `UUID` fields in Temporal workflows.

#### [MODIFY] [StartTransactionUseCaseImpl.java](file:///Users/me/myprojects/agentbanking-backend/services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/usecase/StartTransactionUseCaseImpl.java)

- Replace `"DEFAULT"` with a valid `UUID` or `null` where appropriate (e.g., `UUID.randomUUID()` or a zero-UUID for dummy fields).
- Validate that all mandatory workflow inputs are correctly mapped from the command.


### Component: Template Scaffold
Finalize the domain model template to provide a clear reference for separating Java Records (domain) from JPA Entities (infrastructure) and Repository patterns.

**File:** [docs/templates/domain-model-template.md](file:///Users/me/myprojects/agentbanking-backend/docs/templates/domain-model-template.md) (Update/Finalize)
- Ensure it covers:
  - Domain Records (Value Objects)
  - Infrastructure JPA Entities
  - Repository Port/Adapter split

### Component: OpenAPI Schema 
Update the `TransactionStartRequest` component to guarantee the external APIs receive correct models.
**File:** [docs/api/openapi.yaml](file:///Users/me/myprojects/agentbanking-backend/docs/api/openapi.yaml)
**Lines:** 2639-2646 (Inside `TransactionStartRequest`)
```yaml
    TransactionStartRequest:
      required:
      - transactionType
      - agentId
      - amount
      - idempotencyKey
      type: object
```
*Note: explicitly added [idempotencyKey](file:///Users/me/myprojects/agentbanking-backend/gateway/src/test/java/com/agentbanking/gateway/integration/BaseIntegrationTest.java#294-297) to required to avoid runtime 400 errors from strict proxy mappers.*

## Verification Plan

### Automated Tests
1. Run `./gradlew test --tests "*HexagonalArchitectureTest*"` natively to ensure the hook functions. 
2. Use `docker compose --profile all up -d` and push a withdrawal transaction directly to `POST http://localhost:8080/api/v1/transactions` containing legacy payload `customerCard`—verifying Gateway flawlessly patches it to `customerCardMasked` downstream.

### User Review Required
None. Please review the detailed paths and file alterations and let me know if you would like me to execute this action plan directly.
