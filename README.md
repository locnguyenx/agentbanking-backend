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

## API Documentation

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

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.2.5, Spring Cloud
- **Database:** PostgreSQL (per service)
- **Caching:** Redis
- **Messaging:** Apache Kafka
- **Frontend:** React 18, TypeScript, Vite
- **Testing:** JUnit 5, Mockito, ArchUnit
