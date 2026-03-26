# Agent Banking Platform

## Architecture

```
POS Terminal → API Gateway (port 8080) → Internal Services
                    │
                    ├── Rules Service (port 8081)
                    ├── Ledger Service (port 8082)
                    ├── Onboarding (port 8083)
                    ├── Switch Adapter (port 8084)
                    └── Biller Service (port 8085)

Backoffice UI (port 3000) → Gateway → Services
Mock Server (port 8090) — Downstream simulators
```

## Quick Start

### 1. Start Mock Server (for development)

```bash
cd mock-server
./gradlew bootRun
```

### 2. Start All Services with Docker Compose

```bash
docker compose up -d
```

### 3. Run Integration Tests

```bash
./scripts/integration-tests.sh
```

### 4. Start Backoffice UI (development)

```bash
cd backoffice
npm install
npm run dev
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | JWT auth, routing, rate limiting |
| Rules Service | 8081 | Fee engine, limits, velocity checks |
| Ledger Service | 8082 | Agent floats, transactions, journals |
| Onboarding | 8083 | e-KYC, MyKad verification, biometric |
| Switch Adapter | 8084 | ISO 8583/20022, PayNet integration |
| Biller Service | 8085 | Bill payments, telco top-ups |
| Mock Server | 8090 | Downstream system simulators |
| Backoffice UI | 3000 | Bank operations dashboard |

**Access points:**
| Service | URL	|
|---------|------|
| API Gateway	|	http://localhost:8080 (http://localhost:8080)	|
| Mock Server	|	http://localhost:8090 (http://localhost:8090)	|
| Backoffice UI	|	http://localhost:5173 (http://localhost:5173)	|
| Swagger UI	|	http://localhost:8080/swagger-ui.html (http://localhost:8080/swagger-ui.html)	|

- **External API:** `docs/api/openapi.yaml` (OpenAPI 3.0)
- **Internal APIs:** `<service>/docs/openapi-internal.yaml`

## Specs

- **BRD:** `docs/superpowers/specs/agent-banking-platform/*-brd.md`
- **BDD:** `docs/superpowers/specs/agent-banking-platform/*-bdd.md`
- **Design:** `docs/superpowers/specs/agent-banking-platform/*-design.md`

## Development

### Build all services

```bash
./gradlew build
```

### Run a single service

```bash
./gradlew :services:rules-service:bootRun
```

### Run tests

```bash
./gradlew test
./scripts/integration-tests.sh
```

**Test flow:**
1. Mock Server (8090) simulates downstream systems
2. Gateway (8080) routes API requests
3. Backoffice UI connects to Gateway
```bash
# Check service status
docker compose ps
# View logs
docker compose logs -f
```

## Docker Compose Usage

### Start all services

```bash
docker compose up -d
```

### Profiles

The profiles are working. Here's a summary of available profiles:

| Profile	| Services	|
|---------|------|
| infra	| PostgreSQL (5), Redis, Kafka	|
| mocks	| Mock Server	|
| backend	| Rules, Ledger, Onboarding, Switch-Adapter, Biller, Mock Server	|
| gateway	| API Gateway	|
| frontend	| Backoffice	|
| all	| Everything	|

### Start specific service groups using profiles

```bash
# Infrastructure only (PostgreSQL, Redis, Kafka)
docker compose --profile infra up -d

# Backend services only
docker compose --profile backend up -d

# Mock server only
docker compose --profile mocks up -d

# Frontend only
docker compose --profile frontend up -d

# Gateway only
docker compose --profile gateway up -d
```

### Combine profiles

```bash
# Backend + Infrastructure (without frontend/gateway)
docker compose --profile infra --profile backend up -d

# All except frontend
docker compose --profile infra --profile backend --profile gateway --profile mocks up -d
```

### Common commands

```bash
# Stop all services
docker compose down

# View service status
docker compose ps

# View logs (all services)
docker compose logs -f

# View logs (specific service)
docker compose logs -f rules-service

# Rebuild specific service
docker compose build backoffice
docker compose up -d backoffice

# Restart specific service
docker compose restart rules-service

# Clean slate (remove containers + volumes)
docker compose down -v

# List available services
docker compose config --services
```

## API Documentation

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.2.5, Spring Cloud
- **Database:** PostgreSQL (per service)
- **Caching:** Redis
- **Messaging:** Apache Kafka
- **Frontend:** React 18, TypeScript, Vite
- **Testing:** JUnit 5, Mockito, ArchUnit
