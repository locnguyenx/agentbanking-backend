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

export const api = {
  getDashboard: () => client.get('/backoffice/dashboard').then((r) => r.data),
  getAgents: (params?: any) => client.get('/backoffice/agents', { params }).then((r) => r.data),
  createAgent: (data: any) => client.post('/backoffice/agents', data).then((r) => r.data),
  getAgent: (id: string) => client.get(`/backoffice/agents/${id}`).then((r) => r.data),
  updateAgent: (id: string, data: any) => client.put(`/backoffice/agents/${id}`, data).then((r) => r.data),
  deactivateAgent: (id: string) => client.delete(`/backoffice/agents/${id}`).then((r) => r.data),
  getTransactions: (params?: any) => client.get('/backoffice/transactions', { params }).then((r) => r.data),
  getSettlement: (params?: any) => client.get('/backoffice/settlement', { params }).then((r) => r.data),
  exportSettlement: (params?: any) => client.get('/backoffice/settlement/export', { params, responseType: 'blob' }).then((r) => r.data),
  getKycReviewQueue: (params?: any) => client.get('/backoffice/kyc/review-queue', { params }).then((r) => r.data),
  approveKyc: (id: string) => client.post(`/backoffice/kyc/review-queue/${id}/approve`, {}).then((r) => r.data),
  rejectKyc: (id: string, reason: string) => client.post(`/backoffice/kyc/review-queue/${id}/reject`, { reason }).then((r) => r.data),
}

export default api
