# Implementation Plan: Align Gateway API Endpoints

## Goal
Add the missing external routes to the API Gateway to align with the [docs/api/openapi.yaml](file:///Users/me/myprojects/agentbanking-backend/docs/api/openapi.yaml) specification and verify them using isolated integration tests.

## Proposed Changes

### Gateway Service

#### [MODIFY] [application.yaml](file:///Users/me/myprojects/agentbanking-backend/gateway/src/main/resources/application.yaml)
Add the following `external-` routes to the `spring.cloud.gateway.routes` section:

1.  **Proxy Enquiry (Biller)**: `/api/v1/transfer/proxy/enquiry` -> `/internal/transfer/proxy/enquiry`
2.  **Balance Inquiry (Switch)**: `/api/v1/balance-inquiry` -> `/internal/balance-inquiry`
3.  **Rules Fees**: `/api/v1/rules/fees` -> `/internal/fees`
4.  **MyKad Verification**: `/api/v1/onboarding/verify-mykad` -> `/internal/verify-mykad`
5.  **Submit Application**: `/api/v1/onboarding/submit-application` -> `/internal/onboarding/application`
6.  **Agent Balance**: `/api/v1/agent/balance` -> `/internal/balance`
7.  **Biometric Match**: `/api/v1/kyc/biometric` -> `/internal/biometric`

> [!NOTE]
> All new external routes include the `JwtAuth` filter for token validation.

#### [MODIFY] [build.gradle](file:///Users/me/myprojects/agentbanking-backend/gateway/build.gradle)
Add WireMock dependency for isolated integration testing:
```gradle
testImplementation 'org.wiremock:wiremock:3.5.4'
```

#### [MODIFY] [ExternalApiIntegrationTest.java](file:///Users/me/myprojects/agentbanking-backend/gateway/src/test/java/com/agentbanking/gateway/integration/gateway/ExternalApiIntegrationTest.java) [REFACTOR]
Refactor the test to use `@SpringBootTest` and WireMock:
- Remove `@Tag("e2e")` and dependency on external Docker services.
- Extend [AbstractIntegrationTest](file:///Users/me/myprojects/agentbanking-backend/common/src/testFixtures/java/com/agentbanking/common/test/AbstractIntegrationTest.java#12-47) for Redis support.
- Use `WireMockServer` to stub downstream service responses.
- Configure dynamic routes using `@DynamicPropertySource`.

## Verification Plan

### Automated Tests
1.  **Run Gateway Integration Tests**: `./gradlew :gateway:test --tests "com.agentbanking.gateway.integration.gateway.ExternalApiIntegrationTest"`
2.  **Verify Routing**: The test will assert that the Gateway correctly proxies requests to WireMock stubs and returns the expected responses.
3.  **Verify Auth**: The test will verify that requests without valid tokens are rejected by the `JwtAuth` filter.
