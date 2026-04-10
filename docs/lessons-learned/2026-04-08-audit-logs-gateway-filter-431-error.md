# 431 Request Header Fields Too Large - Audit Logs Gateway Filter

**Date:** 2026-04-08  
**Issue:** Audit Logs endpoint returning HTTP 431  
**Root Cause:** Spring Cloud Gateway filter not properly overriding request URL + missing service routes

---

## Problem

When accessing `/api/v1/admin/audit-logs?service=auth`, the gateway returned:
```
HTTP/1.1 431 Request Header Fields Too Large
```

---

## Root Cause Analysis

### 1. Filter Not Mutating Exchange
The `ServiceRouteAuditGatewayFilterFactory` set the `GATEWAY_REQUEST_URL_ATTR` attribute but didn't create a mutated exchange with the new URI.

```java
// BROKEN - only set attribute, didn't mutate exchange
exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, URI.create(fullUrl));
return chain.filter(exchange);  // used original exchange, route still pointed to gateway
```

### 2. Gateway Route Looped to Itself
```yaml
- id: admin-audit-logs
  uri: http://gateway:8080  # pointing to itself
```

The filter was supposed to override this, but without proper exchange mutation, the routing looped.

### 3. Service Routes Missing /internal/audit-logs
Each service (rules, ledger, onboarding, etc.) didn't have `/internal/audit-logs` in their route predicates, returning 404.

---

## Resolution

### 1. Fix Filter - Mutate Exchange with New URI

```java
// FIXED - properly mutate exchange
URI newUri = URI.create(fullUrl);
exchange.getAttributes().put(
    org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
    newUri
);

ServerWebExchange mutatedExchange = exchange.mutate()
    .request(exchange.getRequest().mutate().uri(newUri).build())
    .build();

return chain.filter(mutatedExchange);
```

### 2. Add Service Routes in application.yaml

Added `/internal/audit-logs` to each service predicate:
```yaml
# Rules Service
- id: rules-service
  predicates:
    - Path=/internal/fees/**, /internal/check-velocity, /internal/limits/**, /internal/transactions/quote, /internal/audit-logs
```

Same for: ledger, onboarding, switch-adapter, biller, orchestrator.

### 3. Remove "All Services" Option from UI

Since each service maintains its own local audit_logs table (no central aggregation), "All Services" is not supported. Removed from dropdown in `backoffice/src/pages/SystemAdmin.tsx`.

---

## Key Lessons

1. **Spring Cloud Gateway filter** - Setting `GATEWAY_REQUEST_URL_ATTR` is not enough; must also mutate the exchange with the new URI
2. **Service-local audit** - Each microservice has its own audit_logs table; no central audit aggregation (per design spec)
3. **Docker build cache** - Always use `--no-cache` flag when code changes aren't reflected in container
4. **Netty vs Tomcat** - Gateway uses Netty, not Tomcat; `server.tomcat.*` config doesn't apply

---

## Files Changed

- `gateway/src/main/java/com/agentbanking/gateway/filter/ServiceRouteAuditGatewayFilterFactory.java`
- `gateway/src/main/resources/application.yaml`
- `backoffice/src/pages/SystemAdmin.tsx`