/**
 * Backend API response schemas (Zod).
 * 
 * These schemas mirror the actual backend DTOs and serve as the
 * single source of truth for mock data generation.
 * 
 * When the backend changes a DTO, update the schema here FIRST,
 * then regenerate mocks. Contract tests will catch drift.
 */

// ============================================================
// User API Schema (from UserResponseDto.java)
// Backend: services/auth-iam-service/.../web/dto/UserResponseDto.java
// ============================================================
export const UserSchema = {
  userId: 'string (UUID)',
  username: 'string',
  email: 'string',
  fullName: 'string',
  status: 'enum: ACTIVE | INACTIVE | LOCKED | DELETED',
  permissions: 'string[]',
  createdAt: 'string (ISO datetime)',
  lastLoginAt: 'string (ISO datetime) | null',
} as const

export const VALID_USER_STATUSES = ['ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED'] as const

// ============================================================
// Agent API Schema (from AgentResponse.java)
// Backend: services/onboarding-service/.../web/dto/AgentResponse.java
// ============================================================
export const AgentSchema = {
  agentId: 'string (UUID)',
  agentCode: 'string',
  businessName: 'string',
  tier: 'enum: BASIC | STANDARD | PREMIUM',
  status: 'enum: ACTIVE | SUSPENDED | INACTIVE',
  phoneNumber: 'string',
  merchantGpsLat: 'number (BigDecimal)',
  merchantGpsLng: 'number (BigDecimal)',
  createdAt: 'string (ISO datetime)',
  updatedAt: 'string (ISO datetime)',
} as const

export const VALID_AGENT_TIERS = ['BASIC', 'STANDARD', 'PREMIUM'] as const
export const VALID_AGENT_STATUSES = ['ACTIVE', 'SUSPENDED', 'INACTIVE'] as const

// ============================================================
// Transaction API Schema (from LedgerController.java)
// Backend: services/ledger-service/.../web/LedgerController.java
// ============================================================
export const TransactionSchema = {
  transactionId: 'string (UUID)',
  agentId: 'string (UUID)',
  transactionType: 'string',
  amount: 'number (BigDecimal)',
  status: 'enum: COMPLETED | PENDING | FAILED',
  customerCardMasked: 'string',
  createdAt: 'string (ISO datetime)',
} as const

export const VALID_TRANSACTION_STATUSES = ['COMPLETED', 'PENDING', 'FAILED'] as const

// ============================================================
// Paginated Response Schema (from LedgerController.java)
// ============================================================
export const PaginatedResponseSchema = {
  content: 'T[]',
  totalElements: 'number',
  totalPages: 'number',
  page: 'number',
  size: 'number',
} as const

// ============================================================
// Agent User Status Schema (from AgentUserStatusResponse.java)
// Backend: services/auth-iam-service/.../web/dto/AgentUserStatusResponse.java
// ============================================================
export const AgentUserStatusSchema = {
  agentId: 'string (UUID)',
  status: 'enum: ACTIVE | INACTIVE | LOCKED | DELETED | NOT_CREATED',
  userId: 'string (UUID) | undefined',
  error: 'string | undefined',
} as const

export const VALID_AGENT_USER_STATUSES = ['ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED', 'NOT_CREATED'] as const
