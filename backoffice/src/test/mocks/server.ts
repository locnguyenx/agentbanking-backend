/**
 * MSW Server Setup
 * 
 * Mocks HTTP requests at the network layer, allowing tests to make
 * real HTTP calls that are intercepted and returned with mock data.
 */
import { setupServer } from 'msw/node'
import { handlers } from './handlers'

export const server = setupServer(...handlers)