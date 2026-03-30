# OpenAPI Changelog

## Version 1.0.0 (2026-03-30)

## Summary
I've updated the OpenAPI specification with explicit schemas. Here's what was done:

## Changes Made

1. Added External API Request Schemas (with required properties):
- WithdrawalExternalRequest - POST /api/v1/withdrawal
- DepositExternalRequest - POST /api/v1/deposit
- TopupExternalRequest - POST /api/v1/topup
- BillPayExternalRequest - POST /api/v1/bill/pay
- JomPayExternalRequest - POST /api/v1/billpayment/jompay
- EWalletWithdrawExternalRequest - POST /api/v1/ewallet/withdraw
- EWalletTopupExternalRequest - POST /api/v1/ewallet/topup
- DuitNowExternalRequest - POST /api/v1/transfer/duitnow
- EsspExternalRequest - POST /api/v1/essp/purchase
- RetailSaleExternalRequest - POST /api/v1/retail/sale
- BalanceInquiryExternalRequest - POST /api/v1/balance-inquiry
- CreateAgentExternalRequest - POST /api/v1/backoffice/agents
- FeeConfigRequest - POST /api/v1/rules/fees
- MyKadVerifyRequest - POST /api/v1/onboarding/verify-mykad
- ApplicationSubmitRequest - POST /api/v1/onboarding/submit-application
- DiscrepancyMakerActionRequest - POST /api/v1/backoffice/discrepancy/{caseId}/maker-action
- DiscrepancyCheckerActionRequest - POST /api/v1/backoffice/discrepancy/{caseId}/checker-approve/reject
- GeoLocation - Embedded object for location data
2. Added Response Schemas (with all properties):
- TransactionResponse - Standard transaction response
- BalanceResponse - Balance inquiry response
- FeeConfigResponse - Fee configuration response
- AgentResponse - Agent details response
- DashboardResponse - Dashboard statistics
- TransactionListResponse - Paginated transaction list
- SettlementResponse - Settlement details
- KycVerifyResponse - KYC verification result
- ApplicationSubmitResponse - Application submission result
- ErrorResponse - Standard error response (with action_code, trace_id, timestamp)
3. Updated Endpoints with Proper References:
- All endpoints now reference explicit schemas
- Added error responses (400, 401, 402) where appropriate
- Added proper content-type: application/json for all responses
4. Added Missing Endpoints:
- POST /api/v1/rules/fees
- POST /api/v1/billpayment/jompay
- POST /api/v1/onboarding/verify-mykad
- POST /api/v1/onboarding/submit-application

## Key Improvements
1. Required Properties: All request schemas now have explicit required arrays
2. Property Validation: Added minimum, maximum, pattern, minLength, maxLength constraints
3. Enum Values: Defined enums for tier (MICRO/STANDARD/PREMIUM), feeType (FIXED/PERCENTAGE), telco, etc.
4. Descriptions: Added descriptions for all fields
5. Response Schemas: Responses now reference proper schema objects instead of generic type: object
The OpenAPI file now provides clear contract for external consumers with all properties explicitly defined.