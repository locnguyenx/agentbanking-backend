/**
 * Agent mock data — generated from AgentSchema.
 * 
 * Backend: services/onboarding-service/.../web/dto/AgentResponse.java
 * Fields: agentId, agentCode, businessName, tier, status, phoneNumber,
 *         merchantGpsLat, merchantGpsLng, createdAt, updatedAt
 */
import { generateMockAgent, generateMockAgents, MockAgent } from '../mocks/generators'

// Pre-generated mock agents with realistic data
export const MOCK_AGENTS: MockAgent[] = generateMockAgents(7).map((agent, i) => ({
  ...agent,
  agentCode: ['AGT-001', 'AGT-002', 'AGT-003', 'AGT-004', 'AGT-005', 'AGT-006', 'AGT-007'][i],
  businessName: [
    'Ahmad Razak Store',
    'Siti Aminah Store',
    'Faisal Trading',
    'Lee Ming Retail',
    'Nurul Huda Mart',
    'Tan Kah Seng',
    'Ali Ahmad',
  ][i],
  tier: ['PREMIUM', 'STANDARD', 'BASIC', 'PREMIUM', 'STANDARD', 'BASIC', 'BASIC'][i],
  status: ['ACTIVE', 'ACTIVE', 'SUSPENDED', 'ACTIVE', 'ACTIVE', 'ACTIVE', 'INACTIVE'][i],
  phoneNumber: ['012-3456789', '013-8765432', '014-2468135', '016-9753124', '011-6543210', '017-1234567', '019-1111111'][i],
  merchantGpsLat: [3.139003, 3.073400, 3.068500, 5.414100, 1.492700, 4.597500, 3.1][i],
  merchantGpsLng: [101.686855, 101.606500, 101.518200, 100.328700, 103.743100, 101.092100, 101.6][i],
  createdAt: '2026-04-01T12:56:49.938566',
  updatedAt: '2026-04-01T12:56:49.938566',
}))
