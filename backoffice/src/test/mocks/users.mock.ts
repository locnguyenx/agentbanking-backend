/**
 * User mock data — generated from UserSchema.
 * 
 * IMPORTANT: These mocks reflect the ACTUAL backend response shape.
 * The backend UserResponseDto does NOT include userType or agentId.
 * 
 * Backend: services/auth-iam-service/.../web/dto/UserResponseDto.java
 * Fields: userId, username, email, fullName, status, permissions, createdAt, lastLoginAt
 */
import { generateMockUser, generateMockUsers, MockUser } from '../mocks/generators'

// Pre-generated mock users with realistic data and all valid status values
export const MOCK_USERS: MockUser[] = generateMockUsers(10, {
  statusOverrides: ['ACTIVE', 'ACTIVE', 'ACTIVE', 'ACTIVE', 'ACTIVE', 'LOCKED', 'INACTIVE', 'DELETED', 'ACTIVE', 'ACTIVE'],
}).map((user, i) => ({
  ...user,
  username: ['admin', 'teller01', 'operator01', 'maker01', 'checker01', 'lockeduser', 'inactiveuser', 'deleteduser', 'supervisor01', 'compliance01'][i],
  fullName: [
    'System Administrator',
    'Ahmad Teller',
    'Siti Operator',
    'Fatimah Maker',
    'Ali Checker',
    'Locked User',
    'Inactive User',
    'Deleted User',
    'Abu Supervisor',
    'Nurul Compliance',
  ][i],
  email: [
    'admin@agentbanking.com',
    'teller01@bank.com',
    'operator01@bank.com',
    'maker01@bank.com',
    'checker01@bank.com',
    'locked@bank.com',
    'inactive@bank.com',
    'deleted@bank.com',
    'supervisor01@bank.com',
    'compliance01@bank.com',
  ][i],
  createdAt: '2026-01-15T10:00:00Z',
  lastLoginAt: i < 5 ? '2026-04-01T08:30:00Z' : null,
}))

export const MOCK_USER = generateMockUser({
  username: 'testuser',
  email: 'test@bank.com',
  fullName: 'Test User',
  status: 'ACTIVE',
})
