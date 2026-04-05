# IT Administrator Guide

## Overview

This guide covers system administration tasks for the Agent Banking Platform, including service management, monitoring, security configuration, and troubleshooting.

**Target Audience:** IT Operations, DevOps, System Administrators

---

## System Architecture

### Services

| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8080 | External API entry point |
| Audit Service | 8088 | Audit log aggregation |
| Rules Service | 8081 | Transaction rules engine |
| Ledger Service | 8082 | Financial ledger & float |
| Onboarding Service | 8083 | Agent onboarding & KYC |
| Switch Adapter | 8084 | Payment network adapter |
| Biller Service | 8085 | Bill payment processing |
| Orchestrator | 8086 | Transaction Saga coordination |
| Auth/IAM | 8087 | Authentication & authorization |
| Mock Server | 8089 | Development/testing |
| PostgreSQL (per service) | 5433-5440 | Service databases |
| Redis | 6379 | Caching & session |
| Kafka | 9092 | Message broker |

---

## Service Management

### Starting Services

```bash
# Start all services via Docker Compose
docker-compose up -d

# Start specific service
docker-compose up -d rules-service
```

### Service Health Checks

| Service | Health Endpoint |
|---------|-----------------|
| Gateway | `GET /actuator/health` |
| Rules | `GET /actuator/health` |
| Ledger | `GET /actuator/health` |
| Onboarding | `GET /actuator/health` |
| Switch | `GET /actuator/health` |
| Biller | `GET /actuator/health` |

### Stopping Services

```bash
# Stop all services
docker-compose down

# Stop with volumes (data loss)
docker-compose down -v
```

---

## Configuration

### Environment Variables

Create `.env` file in project root:

```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=agentbanking
POSTGRES_USER=admin
POSTGRES_PASSWORD=changeme

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT
JWT_SECRET=your-secret-key-min-32-chars
JWT_EXPIRATION_MS=3600000

# External Systems
HSM_URL=http://hsm-service:8443
SWITCH_URL=http://switch-network:8080
```

### Service-Specific Config

Each service has `config/application.yml`:

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/rules_db
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}

resilience4j:
  circuitbreaker:
    instances:
      externalService:
        slidingWindowSize: 10
        failureRateThreshold: 50
```

---

## Monitoring

### Logging

All services use structured JSON logging:

```bash
# View logs for specific service
docker-compose logs -f rules-service

# View all logs
docker-compose logs -f

# Filter by level
docker-compose logs | grep ERROR
```

### Metrics

Access metrics via Spring Boot Actuator:

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health status |
| `/actuator/metrics` | All metrics |
| `/actuator/metrics/http.server.requests` | HTTP request metrics |
| `/actuator/prometheus` | Prometheus format |

### Log Locations

| Environment | Log Location |
|-------------|--------------|
| Docker | `docker-compose logs` |
| Kubernetes | `/var/log/pods/` |
| Local | Console output |

---

## Security Configuration

### API Gateway

#### Rate Limiting

Configure in `gateway/config/application.yml`:

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
      routes:
        - id: rules-service
          uri: http://rules-service:8081
          predicates:
            - Path=/api/v1/rules/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter:
                  replenishRate: 100
                  burstCapacity: 100
```

#### JWT Validation

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com
          jwk-set-uri: https://auth.example.com/.well-known/jwks.json
```

### Database Security

- Use strong passwords
- Enable SSL connections
- Restrict user permissions per service
- Regular password rotation

### Encryption

| Data Type | Method |
|-----------|--------|
| PAN (Card) | AES-256 at rest |
| PIN Blocks | HSM encryption |
| MyKad Data | AES-256 at rest |
| Passwords | BCrypt hashing |
| TLS | TLS 1.2+ in transit |

---

## User Management

### Creating Admin Users

Via API:

```bash
curl -X POST http://localhost:8080/api/v1/admin/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "securepassword",
    "role": "ADMIN",
    "permissions": ["READ", "WRITE", "APPROVE"]
  }'
```

### Role-Based Access

| Role | Permissions |
|------|-------------|
| USER | Read transactions |
| OPERATOR | Read, write transactions |
| SUPERVISOR | Read, write, approve |
| ADMIN | Full access + user management |

---

## Backup & Recovery

### Database Backup

```bash
# Backup specific database
docker-compose exec -T postgres pg_dump -U admin rules_db > backup.sql

# Restore
docker-compose exec -T postgres psql -U admin rules_db < backup.sql
```

### Redis Backup

```bash
# Save RDB snapshot
docker-compose exec redis redis-cli BGSAVE

# Copy backup
docker cp container:/data/dump.rdb ./backup.rdb
```

---

## Troubleshooting

### Common Issues

#### Service Won't Start

```bash
# Check logs
docker-compose logs service-name

# Check port conflicts
lsof -i :8081

# Check dependencies
docker-compose ps
```

#### Database Connection Failed

1. Verify PostgreSQL running: `docker-compose ps postgres`
2. Check credentials in config
3. Verify network connectivity
4. Check disk space

#### High Memory Usage

```bash
# Check container stats
docker stats

# Adjust heap size
JAVA_OPTS: "-Xmx512m -Xms256m"
```

#### Transaction Failures

1. Check Kafka connectivity
2. Verify Redis availability
3. Review circuit breaker status
4. Check downstream service health

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    root: INFO
    com.agentbanking: DEBUG
```

---

## Audit

### Audit Logs

All operations are logged with:
- Timestamp (ISO 8601)
- User ID
- Action performed
- Resource accessed
- IP address
- Result (success/failure)

### Viewing Audit Logs

```bash
# Query audit table
docker-compose exec postgres psql -U admin -d ledger_db \
  -c "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT 100;"
```

---

## Performance Tuning

### JVM Options

```yaml
environment:
  - JAVA_OPTS=-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xms512m -Xmx2g
```

### Connection Pools

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### Caching Strategy

| Cache | TTL | Use Case |
|-------|-----|----------|
| Agent Profile | 1 hour | Frequent reads |
| Transaction | 5 min | Status checks |
| Rules | 24 hour | Static rules |
| Idempotency | 24 hour | Duplicate prevention |

---

## Disaster Recovery

### Recovery Procedures

| Scenario | RTO | RPO |
|----------|-----|-----|
| Service failure | 15 min | 5 min |
| Database failure | 1 hour | 1 hour |
| Total outage | 4 hours | 24 hours |

### Failover Steps

1. Verify incident scope
2. Activate failover (if applicable)
3. Notify stakeholders
4. Restore services
5. Verify data integrity
6. Document incident

---

## Appendix: API Reference

### Admin Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /admin/health/all | Aggregated health of all services |
| GET | /admin/metrics/{service} | JVM & HTTP metrics for a service |
| GET | /admin/audit-logs | Query audit logs with filters |
| GET | /admin/audit-logs/export | Export audit logs as CSV |

### Backoffice IT Admin Dashboard

Access the IT Admin dashboard via the Backoffice UI at `/system-admin`.

#### Features

1. **Service Health Dashboard**
   - View health status of all microservices at a glance
   - Status indicators: UP (green), DOWN (red), DEGRADED (amber)
   - Click on any service to view its metrics
   - Auto-refresh capability

2. **Performance Metrics**
   - JVM metrics: Memory usage, thread count, CPU usage, uptime
   - HTTP metrics: Request count, error count, average response time
   - Select individual services from dropdown

3. **Cross-Service Audit Logs**
   - Searchable audit log table
   - Filter by service, action, user, outcome, date range
   - Pagination support
   - Export to CSV

#### Access Requirements

- Role required: `IT_ADMIN`
- Access via Backoffice UI (authenticated)
- Or direct API access via gateway with JWT token

#### API Examples

```bash
# Get all service health
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/admin/health/all

# Get metrics for specific service
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/admin/metrics/ledger

# Query audit logs
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/admin/audit-logs?serviceName=auth-iam-service&page=0&size=20"

# Export audit logs
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/admin/audit-logs/export?from=2026-04-01T00:00:00" \
  -o audit-logs.csv
```

### Service Discovery

Services register with Eureka (if enabled):
- `http://eureka:8761/eureka/apps`
