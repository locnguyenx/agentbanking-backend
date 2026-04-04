/**
 * Transaction mock data — generated from TransactionSchema.
 * 
 * Backend: services/ledger-service/.../web/LedgerController.java
 * Returns paginated response: { content, totalElements, totalPages, page, size }
 */
import { generateMockTransaction, generatePaginatedTransactions, MockTransaction } from '../mocks/generators'

// 25 transactions = 3 pages (10 per page)
export const MOCK_TRANSACTIONS_RESPONSE = generatePaginatedTransactions(25)

export const MOCK_TRANSACTIONS: MockTransaction[] = MOCK_TRANSACTIONS_RESPONSE.content

export const MOCK_TRANSACTION = generateMockTransaction({
  transactionId: 'f4eebc99-9c0b-4ef8-bb6d-6bb9bd380a70',
  agentId: 'd3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
  transactionType: 'CASH_DEPOSIT',
  amount: 25000,
  status: 'COMPLETED',
  customerCardMasked: '411111******0000',
  createdAt: '2026-03-26T12:30:00',
})
