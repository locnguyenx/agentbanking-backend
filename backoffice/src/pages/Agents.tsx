import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { X, Edit2, Users, Search, Filter, Download, ChevronLeft, ChevronRight, MoreVertical, MapPin, CheckCircle, Clock, XCircle } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '../api/client'

interface Agent {
  agentId: string
  agentCode: string
  businessName: string
  tier: string
  status: string
  phoneNumber: string
  merchantGpsLat: number
  merchantGpsLng: number
  createdAt?: string
  updatedAt?: string
}

interface AgentUserStatus {
  agentId: string
  status: string
  userId?: string
  error?: string
}

export function Agents() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [currentPage, setCurrentPage] = useState(1)
  const [showAddModal, setShowAddModal] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('All')
  const [agentIdFilter, setAgentIdFilter] = useState('')
  const [selectedAgentId, setSelectedAgentId] = useState<string | null>(null)
  const [viewAgent, setViewAgent] = useState<Agent | null>(null)
  const [editAgent, setEditAgent] = useState<Agent | null>(null)
  const itemsPerPage = 6

  const { data: agents = [] } = useQuery({
    queryKey: ['agents'],
    queryFn: async () => {
      const response = await api.getAgents()
      return response as Agent[]
    }
  })

  const { data: agentUserStatuses = [] } = useQuery({
    queryKey: ['agentUserStatuses'],
    queryFn: async () => {
      const statuses: AgentUserStatus[] = []
      for (const agent of agents) {
        try {
          const status = await api.getAgentUserStatus(agent.agentId)
          statuses.push(status as AgentUserStatus)
        } catch {
          statuses.push({ agentId: agent.agentId, status: 'UNKNOWN', userId: undefined, error: 'Failed to fetch' })
        }
      }
      return statuses
    },
    enabled: agents.length > 0
  })

  const getAgentUserStatus = (agentId: string): AgentUserStatus | undefined => {
    return agentUserStatuses.find(s => s.agentId === agentId)
  }

  const createAgentMutation = useMutation({
    mutationFn: (data: { agentCode: string; businessName: string; tier: string; phoneNumber: string; mykadNumber: string }) => 
      api.createAgent({
        agentCode: data.agentCode,
        businessName: data.businessName,
        tier: data.tier,
        merchantGpsLat: 3.139003,
        merchantGpsLng: 101.686855,
        mykadNumber: data.mykadNumber,
        phoneNumber: data.phoneNumber
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agents'] })
      setShowAddModal(false)
      toast.success('Agent created successfully!')
    },
    onError: (error: any) => {
      console.error('Failed to create agent:', error)
      toast.error(error?.response?.data?.error?.message || `Failed to create agent: ${error.message || 'Unknown error'}`)
    }
  })

  const updateAgentMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) => 
      api.updateAgent(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agents'] })
      setEditAgent(null)
      toast.success('Agent updated successfully!')
    },
    onError: (error: any) => {
      toast.error(`Failed to update agent: ${error.message}`)
    }
  })

  const deactivateAgentMutation = useMutation({
    mutationFn: (id: string) => api.deactivateAgent(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agents'] })
      toast.success('Agent deactivated successfully!')
    },
    onError: (error: any) => {
      toast.error(`Failed to deactivate agent: ${error.message}`)
    }
  })

const filteredAgents = agents.filter(agent => {
     const matchesSearch = agent.businessName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                           agent.agentCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
                           agent.phoneNumber.includes(searchTerm)
     const matchesStatus = statusFilter === 'All' || agent.status === statusFilter
     const matchesAgentId = !agentIdFilter || agent.agentId.toLowerCase().includes(agentIdFilter.toLowerCase())
     return matchesSearch && matchesStatus && matchesAgentId
   })

  const totalPages = Math.max(1, Math.ceil(filteredAgents.length / itemsPerPage))
  
  // Reset to page 1 if current page exceeds total pages
  const safeCurrentPage = currentPage > totalPages ? 1 : currentPage
  
  const paginatedAgents = filteredAgents.slice(
    (safeCurrentPage - 1) * itemsPerPage,
    safeCurrentPage * itemsPerPage
  )

  const handlePageChange = (page: number) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page)
    }
  }

  const handlePrevPage = () => {
    if (safeCurrentPage > 1) {
      setCurrentPage(safeCurrentPage - 1)
    }
  }

  const handleNextPage = () => {
    if (safeCurrentPage < totalPages) {
      setCurrentPage(safeCurrentPage + 1)
    }
  }

  const handleAddAgent = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const form = e.currentTarget
    const formData = new FormData(form)
    createAgentMutation.mutate({
      agentCode: `AGT-${String(agents.length + 1).padStart(3, '0')}`,
      businessName: formData.get('name') as string,
      phoneNumber: formData.get('phone') as string,
      mykadNumber: formData.get('mykad') as string,
      tier: formData.get('tier') as string || 'STANDARD'
    })
    setCurrentPage(1)
  }

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value)
    setCurrentPage(1) // Reset to first page on search
  }

  const handleStatusFilterChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setStatusFilter(e.target.value)
    setCurrentPage(1) // Reset to first page on filter change
  }

  const handleAgentAction = (agentId: string) => {
    setSelectedAgentId(selectedAgentId === agentId ? null : agentId)
  }

  const handleViewAgent = (agent: Agent) => {
    setViewAgent(agent)
    setSelectedAgentId(null)
  }

  const handleEditAgent = (agent: Agent) => {
    setEditAgent(agent)
    setSelectedAgentId(null)
  }

  const handleDeactivateAgent = (agentId: string) => {
    if (confirm(`Deactivate agent ${agentId}? This action cannot be undone.`)) {
      deactivateAgentMutation.mutate(agentId)
    }
    setSelectedAgentId(null)
  }

  const createAgentUserMutation = useMutation({
    mutationFn: ({ agentId, data }: { agentId: string; data: { phone?: string; email?: string; businessName?: string } }) =>
      api.createAgentUser(agentId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agentUserStatuses'] })
      toast.success('User account created successfully!')
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.error?.message || `Failed to create user account: ${error.message}`)
    }
  })

  const handleCreateAgentUser = (agentId: string) => {
    const agent = agents.find(a => a.agentId === agentId)
    if (agent) {
      createAgentUserMutation.mutate({
        agentId,
        data: {
          phone: agent.phoneNumber,
          businessName: agent.businessName
        }
      })
    }
    setSelectedAgentId(null)
  }

  const handleSaveEdit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const form = e.currentTarget
    const formData = new FormData(form)
    if (editAgent) {
      updateAgentMutation.mutate({
        id: editAgent.agentId,
        data: {
          businessName: formData.get('businessName') as string,
          phoneNumber: formData.get('phoneNumber') as string,
          tier: formData.get('tier') as string,
          merchantGpsLat: editAgent.merchantGpsLat,
          merchantGpsLng: editAgent.merchantGpsLng
        }
      })
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }}>
            Agent Management
          </h2>
          <p style={{ fontSize: 14, color: '#64748b' }}>
            Manage and monitor registered agents
          </p>
        </div>
        <button 
          className="btn btn-primary"
          onClick={() => setShowAddModal(true)}
          data-testid="add-agent-button"
        >
          <Users size={18} />
          Add Agent
        </button>
      </div>

      {/* Filters */}
      <div className="card" style={{ padding: 16 }}>
<div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search 
              size={18} 
              style={{ 
                position: 'absolute', 
                left: 14, 
                top: '50%', 
                transform: 'translateY(-50%)',
                color: '#94a3b8'
              }} 
            />
            <input 
              type="text"
              placeholder="Search agents by name, ID, or phone..."
              className="input"
              style={{ paddingLeft: 42 }}
              value={searchTerm}
              onChange={handleSearchChange}
              data-testid="search-input"
            />
          </div>
          <input
            type="text"
            placeholder="Filter by Agent ID..."
            className="input"
            style={{ width: 180, marginLeft: 8 }}
            value={agentIdFilter}
            onChange={(e) => {
              setAgentIdFilter(e.target.value);
              setCurrentPage(1);
            }}
            data-testid="agent-id-filter"
          />
          <select className="input" style={{ width: 150 }} onChange={handleStatusFilterChange} value={statusFilter} data-testid="status-filter">
            <option value="All">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="SUSPENDED">Suspended</option>
          </select>
          <select className="input" style={{ width: 130 }}>
            <option>All Tiers</option>
            <option>Gold</option>
            <option>Silver</option>
            <option>Bronze</option>
          </select>
          <button className="btn btn-outline">
            <Filter size={18} />
            More Filters
          </button>
          <button className="btn btn-outline">
            <Download size={18} />
            Export
          </button>
        </div>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        {(() => {
          const total = agents.length
          const active = agents.filter(a => a.status === 'ACTIVE').length
          const suspended = agents.filter(a => a.status === 'SUSPENDED').length
          const inactive = agents.filter(a => a.status === 'INACTIVE').length
          return [
            { label: 'Total Agents', value: total.toString(), color: '#1e3a5f' },
            { label: 'Active', value: active.toString(), color: '#10b981' },
            { label: 'Suspended', value: suspended.toString(), color: '#f59e0b' },
            { label: 'Inactive', value: inactive.toString(), color: '#ef4444' },
          ].map((stat, index) => (
            <div key={index} className="card" style={{ padding: 20, textAlign: 'center' }}>
              <p style={{ fontSize: 13, color: '#64748b', marginBottom: 4 }}>{stat.label}</p>
              <h3 style={{ fontSize: 24, fontWeight: 700, color: stat.color }}>{stat.value}</h3>
            </div>
          ))
        })()}
      </div>

      {/* Table */}
      <div className="card">
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>
                  <input type="checkbox" style={{ width: 16, height: 16 }} />
                </th>
                <th>Agent Code</th>
                <th>Name</th>
                <th>Phone</th>
                <th>Location</th>
                <th>Status</th>
                <th>User Account</th>
                <th>Tier</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {paginatedAgents.map((agent) => (
                <tr key={agent.agentId}>
                  <td>
                    <input type="checkbox" style={{ width: 16, height: 16 }} />
                  </td>
                  <td style={{ fontFamily: 'monospace', color: '#1e3a5f', fontWeight: 500 }}>
                    {agent.agentCode}
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <div style={{
                        width: 36,
                        height: 36,
                        borderRadius: 8,
                        background: 'linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontWeight: 600,
                        fontSize: 13
                      }}>
                        {agent.businessName.split(' ').map(n => n[0]).join('')}
                      </div>
                      <div>
                        <div style={{ fontWeight: 500 }}>{agent.businessName}</div>
                      </div>
                    </div>
                  </td>
                  <td style={{ color: '#64748b' }}>{agent.phoneNumber}</td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <MapPin size={14} color="#64748b" />
                      GPS: {agent.merchantGpsLat}, {agent.merchantGpsLng}
                    </div>
                  </td>
                  <td>
                    <span className={`badge ${
                      agent.status === 'ACTIVE' ? 'badge-success' : 
                      agent.status === 'SUSPENDED' ? 'badge-warning' : 'badge-error'
                    }`}>
                      {agent.status === 'ACTIVE' && <CheckCircle size={12} style={{ marginRight: 4 }} />}
                      {agent.status}
                    </span>
                  </td>
                  <td>
                    {(() => {
                      const userStatus = getAgentUserStatus(agent.agentId)
                      if (!userStatus) return <span style={{ color: '#94a3b8' }}>Loading...</span>
                      const status = userStatus.status
                      if (status === 'CREATED' || status === 'ACTIVE') {
                        return (
                          <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#10b981', fontSize: 12, fontWeight: 500 }}>
                            <CheckCircle size={12} /> Created
                          </span>
                        )
                      } else if (status === 'INACTIVE' || status === 'LOCKED') {
                        return (
                          <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#f59e0b', fontSize: 12, fontWeight: 500 }}>
                            <Clock size={12} /> Inactive
                          </span>
                        )
                      } else if (status === 'NOT_CREATED') {
                        return (
                          <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#ef4444', fontSize: 12, fontWeight: 500 }}>
                            <XCircle size={12} /> Not Created
                          </span>
                        )
                      } else if (status === 'FAILED') {
                        return (
                          <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#dc2626', fontSize: 12, fontWeight: 500 }}>
                            <XCircle size={12} /> Failed
                          </span>
                        )
                      } else if (status === 'PENDING') {
                        return (
                          <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#6366f1', fontSize: 12, fontWeight: 500 }}>
                            <Clock size={12} /> Pending
                          </span>
                        )
                      } else {
                        return <span style={{ color: '#64748b', fontSize: 12 }}>{status}</span>
                      }
                    })()}
                  </td>
                  <td>
                    <span style={{
                      padding: '4px 10px',
                      borderRadius: 4,
                      fontSize: 12,
                      fontWeight: 600,
                      background: agent.tier === 'GOLD' ? '#fef3c7' : agent.tier === 'SILVER' ? '#f1f5f9' : '#fed7aa',
                      color: agent.tier === 'GOLD' ? '#92400e' : agent.tier === 'SILVER' ? '#475569' : '#9a3412'
                    }}>
                      {agent.tier}
                    </span>
                  </td>
                  <td>
                    <div style={{ position: 'relative' }}>
                      <button 
                        style={{ 
                          padding: 8, 
                          background: 'transparent', 
                          border: 'none', 
                          cursor: 'pointer',
                          borderRadius: 6
                        }}
                        onClick={() => handleAgentAction(agent.agentId)}
                        data-testid={`action-${agent.agentId}-button`}
                      >
                        <MoreVertical size={16} color="#94a3b8" />
                      </button>
                      {selectedAgentId === agent.agentId && (
                        <div style={{
                          position: 'absolute',
                          right: 0,
                          top: '100%',
                          background: 'white',
                          border: '1px solid #e2e8f0',
                          borderRadius: 8,
                          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                          zIndex: 100,
                          minWidth: 150,
                          overflow: 'hidden'
                        }}>
                          <button 
                            style={{
                              display: 'block',
                              width: '100%',
                              padding: '10px 16px',
                              textAlign: 'left',
                              background: 'none',
                              border: 'none',
                              cursor: 'pointer',
                              fontSize: 14,
                              color: '#1e293b'
                            }}
                            onClick={() => handleViewAgent(agent)}
                          >
                            View Details
                          </button>
                          <button 
                            style={{
                              display: 'block',
                              width: '100%',
                              padding: '10px 16px',
                              textAlign: 'left',
                              background: 'none',
                              border: 'none',
                              cursor: 'pointer',
                              fontSize: 14,
                              color: '#1e293b'
                            }}
                            onClick={() => handleEditAgent(agent)}
                          >
                            Edit Agent
                          </button>
                          {(getAgentUserStatus(agent.agentId)?.status === 'NOT_CREATED' || 
                            getAgentUserStatus(agent.agentId)?.status === 'FAILED') && (
                            <button 
                              style={{
                                display: 'block',
                                width: '100%',
                                padding: '10px 16px',
                                textAlign: 'left',
                                background: 'none',
                                border: 'none',
                                cursor: 'pointer',
                                fontSize: 14,
                                color: '#10b981'
                              }}
                              onClick={() => handleCreateAgentUser(agent.agentId)}
                            >
                              {getAgentUserStatus(agent.agentId)?.status === 'FAILED' 
                                ? 'Retry User Creation' 
                                : 'Create User Account'}
                            </button>
                          )}
                          <button 
                            style={{
                              display: 'block',
                              width: '100%',
                              padding: '10px 16px',
                              textAlign: 'left',
                              background: 'none',
                              border: 'none',
                              cursor: 'pointer',
                              fontSize: 14,
                              color: '#ef4444'
                            }}
                            onClick={() => handleDeactivateAgent(agent.agentId)}
                          >
                            Deactivate
                          </button>
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '16px 24px',
          borderTop: '1px solid #e2e8f0'
        }}>
          <p style={{ fontSize: 13, color: '#64748b' }}>
            Showing {((safeCurrentPage - 1) * itemsPerPage) + 1} to {Math.min(safeCurrentPage * itemsPerPage, filteredAgents.length)} of {filteredAgents.length} agents
          </p>
          <div style={{ display: 'flex', gap: 8 }}>
            <button 
              className="btn btn-outline btn-sm" 
              onClick={handlePrevPage}
              disabled={safeCurrentPage === 1}
              data-testid="prev-page-button"
            >
              <ChevronLeft size={16} />
              Previous
            </button>
            {totalPages > 0 && Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
              <button 
                key={page}
                className={`btn btn-sm ${page === safeCurrentPage ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => handlePageChange(page)}
                data-testid={`page-${page}-button`}
              >
                {page}
              </button>
            ))}
            <button 
              className="btn btn-outline btn-sm"
              onClick={handleNextPage}
              disabled={safeCurrentPage >= totalPages}
              data-testid="next-page-button"
            >
              Next
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>

      {/* Add Agent Modal */}
      {showAddModal && (
        <div className="modal-overlay" data-testid="add-agent-modal">
          <div className="modal">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
              <h3>Add New Agent</h3>
              <button 
                onClick={() => setShowAddModal(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer' }}
                data-testid="close-modal-button"
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleAddAgent}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Business Name</label>
                  <input type="text" name="name" className="input" placeholder="Enter business name" required />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Phone Number</label>
                  <input type="text" name="phone" className="input" placeholder="012-3456789" required />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>MyKad Number</label>
                  <input type="text" name="mykad" className="input" placeholder="YYYYMMDDXXXXXX" pattern="[0-9]{12}" maxLength={12} title="12-digit MyKad number" required />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Agent Tier</label>
                  <select name="tier" className="input" required>
                    <option value="">Select Tier</option>
                    <option value="MICRO">Micro</option>
                    <option value="STANDARD">Standard</option>
                    <option value="PREMIER">Premier</option>
                  </select>
                </div>
                <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
                  <button type="button" className="btn btn-outline" onClick={() => setShowAddModal(false)} style={{ flex: 1 }}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                    Add Agent
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* View Agent Modal */}
      {viewAgent && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.6)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          backdropFilter: 'blur(4px)'
        }} onClick={() => setViewAgent(null)}>
          <div style={{ 
            width: 540, 
            maxHeight: '85vh', 
            overflow: 'auto',
            background: 'white',
            borderRadius: 16,
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
          }} onClick={e => e.stopPropagation()}>
            <div style={{ 
              padding: 24, 
              borderBottom: '1px solid #e2e8f0',
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center' 
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <div style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  background: 'linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  fontWeight: 600,
                  fontSize: 18
                }}>
                  {viewAgent.businessName.split(' ').map(n => n[0]).join('')}
                </div>
                <div>
                  <h3 style={{ fontSize: 20, fontWeight: 600, margin: 0 }}>{viewAgent.businessName}</h3>
                  <p style={{ fontSize: 13, color: '#64748b', margin: 0 }}>{viewAgent.agentCode}</p>
                </div>
              </div>
              <button onClick={() => setViewAgent(null)} style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}>
                <X size={20} color="#64748b" />
              </button>
            </div>
            <div style={{ padding: 24, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Agent Code</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15, fontFamily: 'monospace' }}>{viewAgent.agentCode}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Status</label>
                <p style={{ margin: '8px 0 0 0' }}><span className={`badge ${viewAgent.status === 'ACTIVE' ? 'badge-success' : viewAgent.status === 'SUSPENDED' ? 'badge-warning' : 'badge-error'}`}>{viewAgent.status}</span></p>
              </div>
              <div style={{ gridColumn: '1 / -1', background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Business Name</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>{viewAgent.businessName}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Phone Number</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>{viewAgent.phoneNumber}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Agent Tier</label>
                <p style={{ 
                  margin: '8px 0 0 0', 
                  fontWeight: 600, 
                  fontSize: 15,
                  color: viewAgent.tier === 'GOLD' ? '#92400e' : viewAgent.tier === 'SILVER' ? '#475569' : '#9a3412'
                }}>{viewAgent.tier}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>GPS Location</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>{viewAgent.merchantGpsLat}, {viewAgent.merchantGpsLng}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Created At</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14 }}>{viewAgent.createdAt ? new Date(viewAgent.createdAt).toLocaleString() : 'N/A'}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Updated At</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14 }}>{viewAgent.updatedAt ? new Date(viewAgent.updatedAt).toLocaleString() : 'N/A'}</p>
              </div>
              <div style={{ gridColumn: '1 / -1', background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Agent ID (UUID)</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 13, fontFamily: 'monospace' }}>{viewAgent.agentId}</p>
              </div>
              <div style={{ gridColumn: '1 / -1', background: '#f8fafc', padding: 16, borderRadius: 12, marginTop: 8 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>User Account</label>
                {(() => {
                  const userStatus = getAgentUserStatus(viewAgent.agentId)
                  if (!userStatus) {
                    return <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14, color: '#64748b' }}>Loading...</p>
                  }
                  if (userStatus.status === 'CREATED' || userStatus.status === 'ACTIVE') {
                    return (
                      <div style={{ marginTop: 8 }}>
                        <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                          <CheckCircle size={12} /> Created
                        </span>
                        {userStatus.userId && (
                          <p 
                            style={{ 
                              fontWeight: 500, 
                              margin: '8px 0 0 0', 
                              fontSize: 13, 
                              fontFamily: 'monospace',
                              color: '#1e3a5f',
                              cursor: 'pointer',
                              textDecoration: 'underline'
                            }}
                            onClick={() => navigate('/users')}
                          >
                            User ID: {userStatus.userId}
                          </p>
                        )}
                      </div>
                    )
                  } else if (userStatus.status === 'INACTIVE' || userStatus.status === 'LOCKED') {
                    return (
                      <div style={{ marginTop: 8 }}>
                        <span className="badge badge-warning" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                          <Clock size={12} /> Inactive
                        </span>
                      </div>
                    )
                  } else if (userStatus.status === 'NOT_CREATED') {
                    return (
                      <div style={{ marginTop: 8 }}>
                        <span className="badge badge-error" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                          <XCircle size={12} /> Not Created
                        </span>
                        <button 
                          className="btn btn-primary" 
                          onClick={() => { handleCreateAgentUser(viewAgent.agentId); setViewAgent(null); }}
                          style={{ marginTop: 12, padding: '8px 16px', fontSize: 13 }}
                        >
                          Create User Account
                        </button>
                      </div>
                    )
                  } else if (userStatus.status === 'FAILED') {
                    return (
                      <div style={{ marginTop: 8 }}>
                        <span className="badge badge-error" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                          <XCircle size={12} /> Failed
                        </span>
                        {userStatus.error && (
                          <p style={{ 
                            margin: '8px 0 0 0', 
                            fontSize: 13, 
                            color: '#dc2626',
                            background: '#fef2f2',
                            padding: '8px 12px',
                            borderRadius: 6,
                            border: '1px solid #fecaca'
                          }}>
                            Error: {userStatus.error}
                          </p>
                        )}
                        <button 
                          className="btn btn-primary" 
                          onClick={() => { handleCreateAgentUser(viewAgent.agentId); setViewAgent(null); }}
                          style={{ marginTop: 12, padding: '8px 16px', fontSize: 13 }}
                        >
                          Retry User Creation
                        </button>
                      </div>
                    )
                  } else {
                    return <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14 }}>{userStatus.status}</p>
                  }
                })()}
              </div>
            </div>
            <div style={{ padding: '0 24px 24px', display: 'flex', gap: 12 }}>
              <button className="btn btn-primary" onClick={() => { setEditAgent(viewAgent); setViewAgent(null); }} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                <Edit2 size={16} /> Edit Agent
              </button>
              <button className="btn btn-outline" onClick={() => setViewAgent(null)} style={{ flex: 1 }}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Agent Modal */}
      {editAgent && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.6)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          backdropFilter: 'blur(4px)'
        }} onClick={() => setEditAgent(null)}>
          <div style={{ 
            width: 480, 
            background: 'white',
            borderRadius: 16,
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
          }} onClick={e => e.stopPropagation()}>
            <div style={{ 
              padding: 24, 
              borderBottom: '1px solid #e2e8f0',
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center' 
            }}>
              <h3 style={{ fontSize: 20, fontWeight: 600, margin: 0 }}>Edit Agent</h3>
              <button onClick={() => setEditAgent(null)} style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}>
                <X size={20} color="#64748b" />
              </button>
            </div>
            <form onSubmit={handleSaveEdit} style={{ padding: 24 }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
                <div>
                  <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, fontSize: 14 }}>Business Name</label>
                  <input 
                    name="businessName"
                    type="text" 
                    className="input" 
                    defaultValue={editAgent.businessName} 
                    required 
                    style={{ padding: '12px 16px', fontSize: 15 }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, fontSize: 14 }}>Phone Number</label>
                  <input 
                    name="phoneNumber"
                    type="text" 
                    className="input" 
                    defaultValue={editAgent.phoneNumber} 
                    required 
                    style={{ padding: '12px 16px', fontSize: 15 }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, fontSize: 14 }}>Agent Tier</label>
                  <select 
                    name="tier"
                    className="input" 
                    defaultValue={editAgent.tier}
                    style={{ padding: '12px 16px', fontSize: 15 }}
                  >
                    <option value="BASIC">Basic</option>
                    <option value="STANDARD">Standard</option>
                    <option value="PREMIUM">Premium</option>
                  </select>
                </div>
                <div style={{ 
                  background: '#f8fafc', 
                  padding: 16, 
                  borderRadius: 12,
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: 12
                }}>
                  <div>
                    <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>Agent Code</label>
                    <p style={{ fontWeight: 500, margin: '4px 0 0 0', fontSize: 14 }}>{editAgent.agentCode}</p>
                  </div>
                  <div>
                    <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>Status</label>
                    <p style={{ margin: '4px 0 0 0' }}><span className={`badge ${editAgent.status === 'ACTIVE' ? 'badge-success' : editAgent.status === 'SUSPENDED' ? 'badge-warning' : 'badge-error'}`}>{editAgent.status}</span></p>
                  </div>
                  <div>
                    <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>Created At</label>
                    <p style={{ fontWeight: 500, margin: '4px 0 0 0', fontSize: 13 }}>{editAgent.createdAt ? new Date(editAgent.createdAt).toLocaleString() : 'N/A'}</p>
                  </div>
                  <div>
                    <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>Updated At</label>
                    <p style={{ fontWeight: 500, margin: '4px 0 0 0', fontSize: 13 }}>{editAgent.updatedAt ? new Date(editAgent.updatedAt).toLocaleString() : 'N/A'}</p>
                  </div>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12, marginTop: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>Agent ID (UUID)</label>
                  <p style={{ fontWeight: 500, margin: '4px 0 0 0', fontSize: 12, fontFamily: 'monospace' }}>{editAgent.agentId}</p>
                </div>
                <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
                  <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '12px 24px', fontSize: 15 }}>
                    Save Changes
                  </button>
                  <button type="button" className="btn btn-outline" onClick={() => setEditAgent(null)} style={{ flex: 1, padding: '12px 24px', fontSize: 15 }}>
                    Cancel
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}