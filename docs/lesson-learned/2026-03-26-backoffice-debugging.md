# Lesson Learned

## Date: 2026-03-26

## Issues Encountered and Fixes

### 1. Missing @Service Annotations
**Problem:** FeeCalculationService and VelocityCheckService in rules-service were missing `@Service` annotation.

**Symptom:** Spring Boot failed to start with error:
```
Error creating bean with name 'rulesController'... Unsatisfied dependency... 
FeeCalculationService... could not be found
```

**Fix:** Added `@Service` annotation to both service classes.

**Prevention:** 
- Add ArchUnit tests to verify all services have proper annotations
- Run integration tests before building Docker images

---

### 2. Gateway Security Configuration Conflict
**Problem:** Gateway had both Spring Security WebFlux and Spring MVC security configured, causing bean conflict.

**Symptom:**
```
BeanDefinitionOverrideException: Invalid bean definition with name 'conversionServicePostProcessor'
```

**Fix:** Added to application.yaml:
```yaml
spring:
  main:
    allow-bean-definition-overriding: true
    web-application-type: reactive
```

**Prevention:**
- Ensure gateway only uses reactive/webflux dependencies
- Don't mix spring-boot-starter-web with spring-cloud-starter-gateway

---

### 3. OAuth2 Resource Server Misconfiguration  
**Problem:** JwtAuthFilter required OAuth2 security context, but OAuth2 was disabled for development.

**Symptom:** All backoffice API calls returned 401 Unauthorized.

**Fix:** Removed JwtAuthFilter from backoffice routes in gateway application.yaml.

**Prevention:**
- Separate development vs production security configs
- Document which routes require JWT authentication

---

### 4. Gateway Route Network Configuration
**Problem:** Gateway used `localhost` for upstream services, which doesn't work in Docker.

**Symptom:** 
```
Connection refused: localhost/127.0.0.1:8083
```

**Fix:** Changed all service URLs from `http://localhost:XXXX` to `http://service-name:XXXX` (Docker service names).

**Prevention:**
- Always use Docker service names for inter-container communication
- Test gateway routing before deploying

---

### 5. Missing Route Rewrite Filters
**Problem:** Backoffice routes lacked RewritePath filters, causing 404 errors.

**Symptom:** 
```
GET /api/v1/backoffice/dashboard 404 Not Found
```

**Fix:** Added RewritePath filters to properly map external API to internal endpoints:
```yaml
filters:
  - RewritePath=/api/v1/backoffice/dashboard, /internal/backoffice/dashboard
```

**Prevention:**
- Verify all route mappings in gateway documentation
- Test each route individually

---

### 6. Missing JPA Repository Configuration
**Problem:** Onboarding service didn't have @EnableJpaRepositories, causing JPA repository not to be scanned.

**Symptom:** 
```
Finished Spring Data repository scanning in 0 JPA repository interfaces
```

**Fix:** Added to OnboardingServiceApplication:
```java
@EntityScan(basePackages = "com.agentbanking.onboarding.domain.model")
@EnableJpaRepositories(basePackages = "com.agentbanking.onboarding.infrastructure.persistence.repository")
```

**Prevention:**
- Follow hexagonal architecture pattern consistently
- Add integration tests that query the database

---

### 7. Gateway Routes Using Wrong Service
**Problem:** Backoffice routes pointed to localhost instead of proper service names, causing routing failures in Docker.

**Fix:** Updated all backoffice routes to use Docker service names:
- `backoffice-dashboard` → `http://ledger-service:8082`
- `backoffice-agents` → `http://ledger-service:8082`
- `backoffice-kyc-review` → `http://onboarding-service:8083`

**Prevention:**
- Create a checklist for Docker deployment validation
- Use environment variables for service URLs

---

## Root Cause Analysis

Most issues stemmed from:
1. **Inconsistent architecture enforcement** - No automated checks for hexagonal architecture violations
2. **Docker networking misunderstanding** - Using localhost instead of Docker service names
3. **Missing integration tests** - Unit tests passed but runtime failed

## Recommendations

1. **Add ArchUnit tests** to all services (now documented in AGENTS.md)
2. **Create Docker deployment checklist** before building images
3. **Add smoke tests** that run after container startup
4. **Document service dependencies** clearly in docker-compose.yml
5. **Use environment variables** for all service URLs in gateway

## Key Takeaways

- Always test in Docker environment, not just locally
- Gateway routing requires careful path rewriting
- JPA repositories need explicit scanning configuration
- Security configuration affects all downstream routes
