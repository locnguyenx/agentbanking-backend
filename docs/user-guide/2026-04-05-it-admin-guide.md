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

### Docker Compose Configuration

The project uses multiple Docker Compose files for different environments:

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Base services (PostgreSQL, Redis, Kafka) |
| `docker-compose.dev.yml` | Local development with all services |
| `docker-compose.test.yml` | CI/CD testing environment |
| `docker/temporal/docker-compose.yml` | Temporal workflow engine |

### Starting Services

```bash
# Start base infrastructure (PostgreSQL, Redis, Kafka)
docker-compose up -d

# Start Temporal workflow engine (required for Orchestrator)
cd docker/temporal && docker-compose up -d

# Start all services for local development
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Start specific service
docker-compose up -d rules-service
```

### Service Health Checks

| Service | Health Endpoint |
|---------|----------------|
| Gateway | `GET /actuator/health` |
| Rules | `GET /actuator/health` |
| Ledger | `GET /actuator/health` |
| Onboarding | `GET /actuator/health` |
| Switch | `GET /actuator/health` |
| Biller | `GET /actuator/health` |
| Orchestrator | `GET /actuator/health` |
| Auth/IAM | `GET /actuator/health` |
| Audit | `GET /actuator/health` |

### Stopping Services

```bash
# Stop all services
docker-compose down

# Stop with volumes (data loss)
docker-compose down -v

# Stop Temporal
cd docker/temporal && docker-compose down
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

## Docker for Integration Testing

### Overview

Integration tests use **TestContainers** which automatically manages Docker containers for test dependencies:
- **PostgreSQL**: Started automatically per test class
- **Redis**: Started automatically per test class  
- **Kafka**: Started automatically per test class

The only exception is **Temporal** (workflow engine) which requires manual Docker setup.

### Required Docker Services

| Service | Purpose | Port | Reason |
|---------|---------|------|--------|
| Temporal | Workflow engine | 7233 | Not available as TestContainers |

### Starting Temporal for Testing

```bash
# Start Temporal (required for orchestrator-service tests)
docker compose up -d temporal temporal-postgres
# or with ui
docker compose up -d temporal temporal-postgres temporal-ui

# Verify Temporal is running
docker ps | grep temporal
```

### TestContainers (Automatic)

Most infrastructure is handled automatically:

```java
// common/src/testFixtures/java/com/agentbanking/common/test/AbstractIntegrationTest.java
static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine");
static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
```

**Note:** TestContainers requires Docker to be running. If tests fail with "Cannot connect to Docker daemon", ensure Docker Desktop is running.

### Running Integration Tests

```bash
# Run all integration tests (TestContainers + Temporal must be running)
./gradlew test

# Run specific service tests
./gradlew :services:orchestrator-service:test

# Run orchestrator tests only (requires Temporal)
./gradlew :services:orchestrator-service:test --tests "*Orchestrator*"

# Run tests with coverage
./gradlew test jacocoTestReport

# Run e2e integration tests and automatically runs cleanup first via cleanE2eTestData
# e2e tests required all services running in docker containers
./gradlew :gateway:e2eTest --no-daemon
# can runt only SelfContainedOrchestratorE2ETest to test transaction
```

### Test Profile Configuration

Tests use the `tc` (TestContainers) profile:

```yaml
# services/*/src/test/resources/application-tc.yaml
spring:
  main:
    allow-bean-definition-overriding: true
  jpa:
    hibernate:
      ddl-auto: none
  flyway:
    enabled: true
    locations: filesystem:src/main/resources/db/migration
```

### Troubleshooting Docker for Tests

```bash
# Check if Docker is running
docker info

# Verify Temporal is running
docker ps | grep temporal

# View Temporal logs
docker logs agentbanking-backend-temporal-1

# Check Temporal UI (for debugging workflows)
open http://localhost:8082
```

### Clean Up

```bash
# Remove Temporal containers
docker compose down temporal temporal-postgres temporal-ui

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

# Temporal (Workflow Engine)
TEMPORAL_ADDRESS=localhost:7233
```

---

## Local Development with Docker

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Docker Desktop | 4.0+ | Container runtime |
| Docker Compose | 2.0+ | Multi-container orchestration |
| Java | 21 | Runtime for services |
| Gradle | 8.5+ | Build tool |

### Quick Start for Local Development

```bash
# 1. Clone repository
git clone <repository-url>
cd agentbanking-backend

# 2. Start infrastructure services
docker-compose up -d

# 3. Start Temporal (required for Orchestrator)
docker compose up -d temporal temporal-postgres temporal-ui

# 4. Verify services are running
docker ps

# 5. Run services via Gradle (not Docker)
./gradlew :services:gateway:bootRun
```

### Running Services Locally vs Docker

| Service | Local (Gradle) | Docker |
|---------|---------------|--------|
| Gateway | `./gradlew :gateway:bootRun` | `docker-compose up -d gateway` |
| Rules | `./gradlew :services:rules-service:bootRun` | `docker-compose up -d rules-service` |
| Ledger | `./gradlew :services:ledger-service:bootRun` | `docker-compose up -d ledger-service` |
| Orchestrator | `./gradlew :services:orchestrator-service:bootRun` | `docker-compose up -d orchestrator` |
| All | `./gradlew bootRun` | `docker-compose -f docker-compose.dev.yml up -d` |

### Docker-Only Development

For full Docker-based local development:

```bash
# Start all services in Docker
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# View logs
docker-compose logs -f

# Scale a service
docker-compose up -d --scale rules-service=2

# Access service shell
docker-compose exec rules-service /bin/sh
```

### Database Management

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d orchestrator_db

# Connect to specific service database
docker-compose exec postgres psql -U postgres -d ledger_db

# Run migrations manually
docker-compose exec orchestrator-service java -jar app.jar flyway:migrate

# View database tables
docker-compose exec postgres psql -U postgres -d orchestrator_db -c "\dt"
```

### Redis Management

```bash
# Connect to Redis CLI
docker-compose exec redis redis-cli

# Common Redis commands
KEYS *                    # List all keys
GET <key>                 # Get value
FLUSHDB                   # Clear database (use carefully!)
INFO                      # Server info
```

### Kafka Management

```bash
# List Kafka topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Create a topic
docker-compose exec kafka kafka-topics --create \
  --topic transaction-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1

# Consume messages from a topic
docker-compose exec kafka kafka-console-consumer \
  --topic transaction-events \
  --from-beginning \
  --bootstrap-server localhost:9092

# View consumer groups
docker-compose exec kafka kafka-consumer-groups \
  --list \
  --bootstrap-server localhost:9092
```

### Temporal UI

The Temporal Web UI is available at `http://localhost:8082`:

- View running workflows
- Check workflow history
- Debug failed workflows
- Manually trigger signals

```bash
# Start Temporal UI only
docker-compose -f docker/temporal/docker-compose.yml up -d temporal-ui

# Access Temporal CLI (tctl)
docker-compose -f docker/temporal/docker-compose.yml exec temporal-admin tctl workflow list
```

### Common Local Development Issues

#### Port Conflicts

```bash
# Check what's using a port
lsof -i :8080
lsof -i :5432
lsof -i :6379
lsof -i :7233

# Kill process using port
kill $(lsof -t -i :8080)
```

#### Container Health Issues

```bash
# Check container status
docker inspect agentbanking-backend-postgres-1 | grep Health

# View container resource usage
docker stats

# Restart unhealthy container
docker-compose restart postgres
```

#### Volume Permissions

```bash
# Fix PostgreSQL volume permissions
docker-compose down
docker volume rm agentbanking-backend_postgres_data
docker-compose up -d
```

#### Network Issues

```bash
# Recreate Docker network
docker network rm agentbanking_backend
docker-compose down
docker-compose up -d

# Check network connectivity
docker-compose exec gateway ping postgres
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

# do filter on a specific container
docker logs agentbanking-backend-gateway-1 --since 60s | grep -i "audit-logs" | tail -20
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
### Seed Admin account
**Admin Credentials (Development Only):**
- Admin: `admin` / `password`

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

## Data Operations

### Test data
Test data is prepared in TestContext: gateway/src/test/java/com/agentbanking/gateway/integration/setup/TestContext.java

### Clean up data (Development Only)

✅ The procedure is complete and tested!
To reset the system anytime:
```bash
./gradlew resetSystem
```
This gives you a fresh system with:
- ✅ No transaction/workflow data
- ✅ No demo users/agents
- ✅ Only essential auth foundation (admin + roles + permissions)

✅ What Happened:
1. cleanAllData task: # ./gradlew cleanAllData
   - ✅ Cleaned ALL transaction data (orchestrator, ledger)
   - ✅ Cleaned ALL auth data (users, roles, permissions, sessions, etc.)
   - ✅ Removed the test user I accidentally created
2. loadAuthSeedData task:
   - ✅ Loaded ONLY essential auth foundation
   - ✅ Created 1 admin user (username: admin, password: password)
   - ✅ Created 5 roles (IT_ADMIN, BANK_OPERATOR, AGENT, AUDITOR, TELLER)
   - ✅ Created 11 permissions
3. resetSystem task:
   - ✅ Successfully ran cleanAllData → loadAuthSeedData
   - ✅ System is now clean with fresh auth seed data
   - ✅ No demo/garbage data - ready for testing

---

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
