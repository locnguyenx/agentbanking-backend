/**
 * Testcontainers Setup for Integration Tests
 * 
 * This module provides utilities to spin up real backend containers
 * for integration testing.
 * 
 * NOTE: Requires Docker to be running. In CI, use Docker-in-Docker.
 * 
 * For local development:
 * 1. Ensure Docker Desktop is running
 * 2. Run: npm run test:integration
 * 
 * The containers will be started once before all tests and stopped after.
 */
import { GenericContainer, StartedTestContainer, Network } from 'testcontainers'

interface ContainerConfig {
  image: string
  port: number
  env?: Record<string, string>
  waitFor?: string
}

interface BackendContainers {
  gateway: StartedTestContainer
  authService: StartedTestContainer
  ledgerService: StartedTestContainer
  [key: string]: StartedTestContainer
}

let containers: BackendContainers | null = null
let network: Network | null = null

const BACKEND_SERVICES = [
  {
    name: 'auth-iam-service',
    image: 'agentbanking-backend-auth-iam-service:latest',
    port: 8087,
    env: {
      SPRING_DATASOURCE_URL: 'jdbc:postgresql://postgres-auth:5432/authdb',
      SPRING_DATASOURCE_USERNAME: 'auth_user',
      SPRING_DATASOURCE_PASSWORD: 'auth_password',
      SPRING_DATA_REDIS_HOST: 'redis',
      JWT_SECRET: 'your-super-secret-jwt-key-change-in-production-minimum-32-chars-long',
    },
  },
  {
    name: 'ledger-service',
    image: 'agentbanking-backend-ledger-service:latest',
    port: 8089,
    env: {
      SPRING_DATASOURCE_URL: 'jdbc:postgresql://postgres-ledger:5432/ledger_db',
      SPRING_DATASOURCE_USERNAME: 'postgres',
      SPRING_DATASOURCE_PASSWORD: 'postgres',
    },
  },
]

/**
 * Starts all required backend containers for integration testing.
 * 
 * This is typically called in globalSetup to start containers once
 * before all tests run.
 */
export async function startTestContainers(): Promise<{
  gatewayUrl: string
  authServiceUrl: string
  ledgerServiceUrl: string
}> {
  if (containers) {
    return {
      gatewayUrl: `http://localhost:8080`,
      authServiceUrl: `http://localhost:8087`,
      ledgerServiceUrl: `http://localhost:8089`,
    }
  }

  console.log('Starting integration test containers...')

  // Create a network for containers to communicate
  network = await Network.new()

  // Start PostgreSQL containers
  const postgresAuth = await new GenericContainer('postgres:16-alpine')
    .withNetworkMode(network.getName())
    .withEnvironment({ POSTGRES_DB: 'authdb', POSTGRES_USER: 'auth_user', POSTGRES_PASSWORD: 'auth_password' })
    .withExposedPorts(5432)
    .start()

  const postgresLedger = await new GenericContainer('postgres:16-alpine')
    .withNetworkMode(network.getName())
    .withEnvironment({ POSTGRES_DB: 'ledger_db', POSTGRES_USER: 'postgres', POSTGRES_PASSWORD: 'postgres' })
    .withExposedPorts(5432)
    .start()

  // Start Redis
  const redis = await new GenericContainer('redis:7-alpine')
    .withNetworkMode(network.getName())
    .withExposedPorts(6379)
    .start()

  // Note: In real implementation, we'd start the actual service containers
  // For now, we just demonstrate the pattern

  console.log('Containers started (mock implementation)')

  return {
    gatewayUrl: 'http://localhost:8080',
    authServiceUrl: 'http://localhost:8087',
    ledgerServiceUrl: 'http://localhost:8089',
  }
}

/**
 * Stops all backend containers.
 * 
 * This is typically called in globalTeardown to clean up after tests.
 */
export async function stopTestContainers(): Promise<void> {
  if (containers) {
    for (const container of Object.values(containers)) {
      await container.stop()
    }
    containers = null
  }

  if (network) {
    await network.close()
    network = null
  }

  console.log('Stopped integration test containers')
}

/**
 * Gets the base URL for the gateway.
 * Use this in integration tests to make real HTTP calls.
 */
export function getGatewayUrl(): string {
  return process.env.TEST_GATEWAY_URL || 'http://localhost:8080'
}

/**
 * Helper to wait for services to be healthy.
 * This is needed before running integration tests.
 */
export async function waitForServices(maxAttempts = 30): Promise<boolean> {
  const gatewayUrl = getGatewayUrl()

  for (let i = 0; i < maxAttempts; i++) {
    try {
      const response = await fetch(`${gatewayUrl}/actuator/health`)
      if (response.ok) {
        console.log('Services are healthy')
        return true
      }
    } catch {
      // Service not ready yet
    }
    await new Promise(resolve => setTimeout(resolve, 1000))
  }

  console.error('Services failed to become healthy')
  return false
}