# Gateway Request Transformation Specification

**Version:** 1.0  
**Date:** 2026-03-30  
**Status:** Approved

---

## 1. Overview

### 1.1 Purpose

This specification defines how the API Gateway transforms external API requests into internal API format before forwarding to backend services.

### 1.2 Design Decisions

| Decision | Choice |
|----------|--------|
| Configuration | YAML-driven (`RequestTransform=withdrawal`) |
| Error handling | Return 400 Bad Request on transformation failure |
| Response transformation | Yes, gateway transforms responses |

### 1.3 Scope

| External Endpoint | Internal Endpoint | Backend Service |
|-------------------|-------------------|-----------------|
| POST /api/v1/withdrawal | POST /internal/debit | ledger-service |
| POST /api/v1/deposit | POST /internal/credit | ledger-service |
| POST /api/v1/balance-inquiry | POST /internal/balance-inquiry | ledger-service |
| GET /api/v1/agent/balance/{agentId} | GET /internal/balance/{agentId} | ledger-service |
| POST /api/v1/topup | POST /internal/topup | biller-service |
| POST /api/v1/bill/pay | POST /internal/pay-bill | biller-service |
| POST /api/v1/billpayment/jompay | POST /internal/jompay | biller-service |
| POST /api/v1/ewallet/withdraw | POST /internal/ewallet/withdrawal | biller-service |
| POST /api/v1/ewallet/topup | POST /internal/ewallet/topup | biller-service |
| POST /api/v1/transfer/duitnow | POST /internal/duitnow | switch-adapter-service |
| POST /api/v1/retail/sale | POST /internal/merchant/retail-sale | ledger-service |
| POST /api/v1/rules/fees | POST /internal/fee-config | rules-service |
| POST /api/v1/onboarding/verify-mykad | POST /internal/verify-mykad | onboarding-service |
| POST /api/v1/onboarding/submit-application | POST /internal/application | onboarding-service |

---

## 2. Architecture

### 2.1 Processing Flow

```
External Request → JwtAuthFilter → RequestTransformFilter → Internal Service
                                          ↓
                                   ResponseTransformFilter → External Response
```

### 2.2 Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| JwtAuthFilter | Validates JWT, extracts agentId, adds X-Agent-Id header |
| RequestTransformFilter | Reads body, transforms fields, rewrites body |
| ResponseTransformFilter | Transforms internal response to external format |

---

## 3. Field Mappings

### 3.1 Withdrawal API

**External** `POST /api/v1/withdrawal` → **Internal** `POST /internal/debit`

| External Field | Internal Field | Transform |
|----------------|----------------|-----------|
| (JWT) | agentId | Extract from X-Agent-Id |
| amount | amount | Pass |
| idempotencyKey | idempotencyKey | Pass |
| customerCard | customerCardMasked | Mask PAN |
| customerPin | (removed) | Drop |
| location.latitude | geofenceLat | Flatten |
| location.longitude | geofenceLng | Flatten |
| - | customerFee | Set null |
| - | agentCommission | Set null |
| - | bankShare | Set null |

**Example:**
```json
// Input
{"amount":100,"customerCard":"4111111111111111","location":{"latitude":3.139}}

// Output
{"agentId":"uuid","amount":100,"customerCardMasked":"411111******1111","geofenceLat":3.139}
```

---

### 3.2 Deposit API

**External** `POST /api/v1/deposit` → **Internal** `POST /internal/credit`

| External Field | Internal Field | Transform |
|----------------|----------------|-----------|
| (JWT) | agentId | Extract |
| amount | amount | Pass |
| idempotencyKey | idempotencyKey | Pass |
| customerAccount | destinationAccount | Rename |
| customerName | (removed) | Drop |

---

### 3.3 Topup API

**External** `POST /api/v1/topup` → **Internal** `POST /internal/topup`

| External Field | Internal Field | Transform |
|----------------|----------------|-----------|
| telco | telco | Pass |
| phoneNumber | phoneNumber | Pass |
| amount | amount | Pass |
| idempotencyKey | idempotencyKey | To UUID |
| currency | (removed) | Drop |

---

### 3.4 E-wallet Withdrawal

**External** `POST /api/v1/ewallet/withdraw` → **Internal** `POST /internal/ewallet/withdrawal`

| External Field | Internal Field | Transform |
|----------------|----------------|-----------|
| walletProvider | walletProvider | Pass |
| walletAccountId | walletId | Rename |
| amount | amount | Pass |
| idempotencyKey | internalTransactionId | To UUID |

---

### 3.5 JomPay API

**External** `POST /api/v1/billpayment/jompay` → **Internal** `POST /internal/jompay`

| External Field | Internal Field | Transform |
|----------------|----------------|-----------|
| billerCode | billerCode | Pass |
| ref1 | ref1 | Pass |
| ref2 | ref2 | Pass |
| amount | amount | Pass |
| idempotencyKey | idempotencyKey | To UUID |

---

### 3.6 Agent Application

**External** `POST /api/v1/onboarding/submit-application` → **Internal** `POST /internal/application`

| External Field | Internal Field | Transform |
|----------------|----------------|-----------|
| businessName | ssmBusinessName | Rename |
| tier | agentTier | To Enum |
| mykadNumber | mykadNumber | Pass |
| phoneNumber | phoneNumber | Pass |
| merchantGpsLat | merchantGpsLat | Pass |
| merchantGpsLng | merchantGpsLng | Pass |
| - | extractedName | Set null |
| - | ssmOwnerName | Set null |

---

## 4. Utility Functions

### 4.1 PAN Masking
```java
public static String maskPan(String pan) {
    if (pan == null || pan.length() < 13) return pan;
    return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
}
```

### 4.2 UUID Conversion
```java
public static UUID toUUID(String value) {
    if (value == null) return UUID.randomUUID();
    try {
        return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
        return UUID.nameUUIDFromBytes(value.getBytes());
    }
}
```

### 4.3 AgentTier Mapping
```java
public static AgentTier toAgentTier(String tier) {
    if (tier == null) return AgentTier.STANDARD;
    return switch (tier.toUpperCase()) {
        case "MICRO" -> AgentTier.MICRO;
        case "PREMIUM" -> AgentTier.PREMIUM;
        default -> AgentTier.STANDARD;
    };
}
```

---

## 5. Error Handling

### 5.1 Transformation Errors

| Error | HTTP Status | Error Code |
|-------|-------------|------------|
| Invalid JSON | 400 | ERR_VAL_INVALID_JSON |
| Missing required field | 400 | ERR_VAL_MISSING_FIELD |
| Invalid field value | 400 | ERR_VAL_INVALID_FIELD |
| Agent ID missing | 401 | ERR_AUTH_MISSING_AGENT |

### 5.2 Error Response Format
```json
{
  "status": "FAILED",
  "error": {
    "code": "ERR_VAL_MISSING_FIELD",
    "message": "customerCard is required",
    "action_code": "DECLINE",
    "trace_id": "uuid",
    "timestamp": "2026-03-30T10:00:00+08:00"
  }
}
```

---

## 6. Implementation

### 6.1 Files to Create

```
gateway/src/main/java/com/agentbanking/gateway/
├── filter/
│   ├── JwtAuthFilter.java              # Existing
│   ├── RequestTransformFilter.java     # NEW
│   └── ResponseTransformFilter.java   # NEW
└── transform/
    ├── Transformers.java                # NEW - utilities
    ├── WithdrawalTransformer.java      # NEW
    ├── DepositTransformer.java         # NEW
    ├── TopupTransformer.java           # NEW
    ├── EWalletTransformer.java         # NEW
    ├── JomPayTransformer.java          # NEW
    ├── RetailTransformer.java          # NEW
    ├── FeeConfigTransformer.java      # NEW
    ├── OnboardingTransformer.java      # NEW
    └── DuitNowTransformer.java        # NEW
```

### 6.2 YAML Configuration

```yaml
- id: external-withdrawal
  uri: http://ledger-service:8082
  predicates:
    - Path=/api/v1/withdrawal
  filters:
    - JwtAuthFilter
    - RequestTransform=withdrawal
    - ResponseTransform=withdrawal
    - RewritePath=/api/v1/withdrawal, /internal/debit
```

### 6.3 Filter Factory

```java
@Component
public class RequestTransformFilterFactory 
    extends AbstractGatewayFilterFactory<RequestTransformFilterFactory.Config> {

    private final Map<String, RequestTransformer> transformers;

    @Override
    public GatewayFilter apply(Config config) {
        RequestTransformer transformer = transformers.get(config.getType());
        return new RequestTransformGatewayFilter(transformer);
    }

    public static class Config {
        private String type;
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
```

---

## 7. Response Transformation

### 7.1 Standard Response Mapping

| Internal Field | External Field |
|----------------|----------------|
| transactionId | transactionId |
| status | status |
| amount | amount |
| message | message |

### 7.2 Response Transform Example

```java
// Internal: {"transactionId":"uuid","amount":100,"customerFee":5}
// External: {"transactionId":"uuid","amount":100,"fee":5,"status":"SUCCESS"}
```

---

## 8. Testing

| Test | Description |
|------|-------------|
| Unit | Each transformer tested independently |
| Integration | E2E with actual backend services |
| Error Cases | Invalid JSON, missing fields |

---

## 9. Security

- PAN masked before storage
- PIN never stored
- AgentId from JWT only

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-03-30 | Initial approved version |
