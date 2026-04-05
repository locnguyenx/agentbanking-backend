import axios from 'axios'

const client = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Add auth token to requests
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('backoffice_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export interface User {
  userId: string
  username: string
  email: string
  fullName: string
  status: string
  userType: 'INTERNAL' | 'EXTERNAL'
  agentId?: string | null
  mustChangePassword?: boolean
  temporaryPasswordExpiresAt?: string
  createdAt?: string
  lastLoginAt?: string
}

export const api = {
  getDashboard: () => client.get('/backoffice/dashboard').then((r) => r.data),
  
  // Agents
  getAgents: (params?: any) => client.get('/backoffice/agents', { params }).then((r) => r.data),
  createAgent: (data: any) => client.post('/backoffice/agents', data).then((r) => r.data),
  getAgent: (id: string) => client.get(`/backoffice/agents/${id}`).then((r) => r.data),
  updateAgent: (id: string, data: any) => client.put(`/backoffice/agents/${id}`, data).then((r) => r.data),
  deactivateAgent: (id: string) => client.delete(`/backoffice/agents/${id}`).then((r) => r.data),
  
  // Users (auth-iam-service via gateway - using /backoffice/admin/users path)
  getUsers: () => client.get('/backoffice/admin/users').then((r) => r.data),
  createUser: (data: any) => client.post('/backoffice/admin/users', data).then((r) => r.data),
  updateUser: (id: string, data: any) => client.put(`/backoffice/admin/users/${id}`, data).then((r) => r.data),
  deleteUser: (id: string) => client.delete(`/backoffice/admin/users/${id}`).then((r) => r.data),
  lockUser: (id: string) => client.post(`/backoffice/admin/users/${id}/lock`, {}).then((r) => r.data),
  unlockUser: (id: string) => client.post(`/backoffice/admin/users/${id}/unlock`, {}).then((r) => r.data),
  resetUserPassword: (id: string, newPassword: string) => 
    client.post(`/backoffice/admin/users/${id}/reset-password`, { newPassword }).then((r) => r.data),
  getAgentUserStatus: (agentId: string) => 
    client.get(`/backoffice/agents/${agentId}/user-status`).then((r) => r.data),
  createAgentUser: (agentId: string, data: { phone?: string; email?: string; businessName?: string }) => 
    client.post(`/backoffice/agents/${agentId}/create-user`, data).then((r) => r.data),
  
  // Transactions & Settlement
  getTransactions: (params?: any) => client.get('/backoffice/transactions', { params }).then((r) => r.data),
  getSettlement: (params?: any) => client.get('/backoffice/settlement', { params }).then((r) => r.data),
  exportSettlement: (params?: any) => client.get('/backoffice/settlement/export', { params, responseType: 'blob' }).then((r) => r.data),
  
  // KYC
  getKycReviewQueue: (params?: any) => client.get('/backoffice/kyc/review-queue', { params }).then((r) => r.data),
  approveKyc: (id: string) => client.post(`/backoffice/kyc/review-queue/${id}/approve`, {}).then((r) => r.data),
  rejectKyc: (id: string, reason: string) => client.post(`/backoffice/kyc/review-queue/${id}/reject`, { reason }).then((r) => r.data),

  // Auth
  login: (username: string, password: string) => 
    client.post('/auth/token', { username, password }).then((r) => r.data),
  getMyProfile: () => client.get('/auth/me').then((r) => r.data),
  changeMyPassword: (data: { currentPassword: string; newPassword: string }) => 
    client.post('/auth/password/change', data).then((r) => r.data),
  logout: () => {
    localStorage.removeItem('backoffice_token')
  },

  // System Admin
  getAdminHealthAll: () => client.get('/admin/health/all').then((r) => r.data),
  getAdminMetrics: (service: string) => client.get(`/admin/metrics/${service}`).then((r) => r.data),
  getAdminAuditLogs: (params?: Record<string, string | number>) =>
    client.get('/admin/audit-logs', { params }).then((r) => r.data),
}

export default api
