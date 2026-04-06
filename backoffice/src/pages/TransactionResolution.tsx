import { useState } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { 
  Search, 
  Filter, 
  CheckCircle,
  XCircle,
  Clock,
  AlertTriangle,
  FileCheck,
  Eye,
  ChevronLeft,
  ChevronRight,
  X,
  ArrowRightLeft,
  Undo2
} from 'lucide-react'
import api from '../api/client'

interface ResolutionItem {
  workflowId: string
  transactionId: string
  agentId: string
  agentName: string
  amount: number
  currency: string
  transactionType: string
  status: 'PENDING_MAKER' | 'PENDING_CHECKER' | 'COMPLETED' | 'REJECTED'
  createdAt: string
  makerProposedAt?: string
  makerAction?: 'COMMIT' | 'REVERSE'
  makerReason?: string
  makerReasonCode?: string
  checkerApprovedAt?: string
  checkerRejectedAt?: string
  checkerReason?: string
}

export function TransactionResolution() {
  const queryClient = useQueryClient()
  const [currentPage, setCurrentPage] = useState(1)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [selectedItem, setSelectedItem] = useState<ResolutionItem | null>(null)
  const [showProposalModal, setShowProposalModal] = useState(false)
  const [proposalForm, setProposalForm] = useState({ action: 'COMMIT' as 'COMMIT' | 'REVERSE', reasonCode: '', reason: '', evidenceUrl: '' })
  const itemsPerPage = 10

  const { data: resolutionResponse } = useQuery({
    queryKey: ['resolutions', statusFilter],
    queryFn: async () => {
      const response = await api.getResolutions({ status: statusFilter || undefined })
      return response as { content: ResolutionItem[] }
    }
  })

  const resolutionItems = resolutionResponse?.content || []

  const proposeResolutionMutation = useMutation({
    mutationFn: ({ workflowId, data }: { workflowId: string; data: { action: 'COMMIT' | 'REVERSE'; reasonCode: string; reason: string; evidenceUrl?: string } }) => 
      api.proposeResolution(workflowId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['resolutions'] })
      setShowProposalModal(false)
      setProposalForm({ action: 'COMMIT', reasonCode: '', reason: '', evidenceUrl: '' })
      alert('Resolution proposed successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to propose resolution: ${error.message}`)
    }
  })

  const approveResolutionMutation = useMutation({
    mutationFn: ({ workflowId, reason }: { workflowId: string; reason: string }) => 
      api.approveResolution(workflowId, { reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['resolutions'] })
      alert('Resolution approved successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to approve resolution: ${error.message}`)
    }
  })

  const rejectResolutionMutation = useMutation({
    mutationFn: ({ workflowId, reason }: { workflowId: string; reason: string }) => 
      api.rejectResolution(workflowId, { reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['resolutions'] })
      alert('Resolution rejected successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to reject resolution: ${error.message}`)
    }
  })

  const filteredItems = resolutionItems.filter(item => {
    const matchesSearch = searchTerm === '' || 
      item.agentName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.transactionId.includes(searchTerm) ||
      item.workflowId.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesStatus = !statusFilter || item.status === statusFilter
    return matchesSearch && matchesStatus
  })

  const totalPages = Math.max(1, Math.ceil(filteredItems.length / itemsPerPage))
  const safeCurrentPage = currentPage > totalPages ? 1 : currentPage
  
  const paginatedItems = filteredItems.slice(
    (safeCurrentPage - 1) * itemsPerPage,
    safeCurrentPage * itemsPerPage
  )

  const pendingMaker = resolutionItems.filter(i => i.status === 'PENDING_MAKER').length
  const pendingChecker = resolutionItems.filter(i => i.status === 'PENDING_CHECKER').length
  const completed = resolutionItems.filter(i => i.status === 'COMPLETED').length
  const rejected = resolutionItems.filter(i => i.status === 'REJECTED').length

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value)
    setCurrentPage(1)
  }

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

  const handlePropose = () => {
    if (!selectedItem || !proposalForm.reasonCode || !proposalForm.reason) {
      alert('Please fill in all required fields')
      return
    }
    proposeResolutionMutation.mutate({
      workflowId: selectedItem.workflowId,
      data: {
        action: proposalForm.action,
        reasonCode: proposalForm.reasonCode,
        reason: proposalForm.reason,
        evidenceUrl: proposalForm.evidenceUrl || undefined
      }
    })
  }

  const handleApprove = (item: ResolutionItem) => {
    const reason = prompt('Enter approval notes (optional):') || ''
    approveResolutionMutation.mutate({ workflowId: item.workflowId, reason })
  }

  const handleReject = (item: ResolutionItem) => {
    const reason = prompt('Enter rejection reason:')
    if (reason) {
      rejectResolutionMutation.mutate({ workflowId: item.workflowId, reason })
    }
  }

  const getStatusBadge = (status: string) => {
    const styles: Record<string, { bg: string; color: string; label: string }> = {
      'PENDING_MAKER': { bg: '#fef3c7', color: '#d97706', label: 'Pending Maker' },
      'PENDING_CHECKER': { bg: '#dbeafe', color: '#2563eb', label: 'Pending Checker' },
      'COMPLETED': { bg: '#d1fae5', color: '#059669', label: 'Completed' },
      'REJECTED': { bg: '#fee2e2', color: '#dc2626', label: 'Rejected' },
    }
    const s = styles[status] || styles['PENDING_MAKER']
    return (
      <span style={{ padding: '4px 10px', borderRadius: 4, fontSize: 12, fontWeight: 600, background: s.bg, color: s.color }}>
        {s.label}
      </span>
    )
  }

  const getActionBadge = (action?: string) => {
    if (action === 'COMMIT') {
      return <span style={{ padding: '4px 10px', borderRadius: 4, fontSize: 12, fontWeight: 600, background: '#d1fae5', color: '#059669' }}>Commit</span>
    }
    if (action === 'REVERSE') {
      return <span style={{ padding: '4px 10px', borderRadius: 4, fontSize: 12, fontWeight: 600, background: '#fee2e2', color: '#dc2626' }}>Reverse</span>
    }
    return null
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }} data-testid="page-title">
            Transaction Resolution
          </h2>
          <p style={{ fontSize: 14, color: '#64748b' }}>
            Review and resolve exceptional transactions
          </p>
        </div>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        {[
          { label: 'Pending Maker', value: pendingMaker.toString(), icon: Clock, color: '#f59e0b' },
          { label: 'Pending Checker', value: pendingChecker.toString(), icon: AlertTriangle, color: '#2563eb' },
          { label: 'Completed', value: completed.toString(), icon: CheckCircle, color: '#10b981' },
          { label: 'Rejected', value: rejected.toString(), icon: XCircle, color: '#ef4444' },
        ].map((stat, index) => {
          const Icon = stat.icon
          return (
            <div key={index} className="card" style={{ padding: 20, display: 'flex', alignItems: 'center', gap: 16 }}>
              <div style={{
                width: 48,
                height: 48,
                borderRadius: 12,
                background: `${stat.color}15`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <Icon size={24} color={stat.color} />
              </div>
              <div>
                <p style={{ fontSize: 13, color: '#64748b', marginBottom: 2 }}>{stat.label}</p>
                <h3 style={{ fontSize: 24, fontWeight: 700, color: '#1e293b' }}>{stat.value}</h3>
              </div>
            </div>
          )
        })}
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
              placeholder="Search by agent, transaction ID, or workflow ID..."
              className="input"
              style={{ paddingLeft: 42 }}
              value={searchTerm}
              onChange={handleSearch}
              data-testid="search-input"
            />
          </div>
          <select className="input" style={{ width: 160 }} value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setCurrentPage(1); }}>
            <option value="">All Status</option>
            <option value="PENDING_MAKER">Pending Maker</option>
            <option value="PENDING_CHECKER">Pending Checker</option>
            <option value="COMPLETED">Completed</option>
            <option value="REJECTED">Rejected</option>
          </select>
          <button className="btn btn-outline">
            <Filter size={18} />
            More Filters
          </button>
        </div>
      </div>

      {/* Resolution Cards */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {paginatedItems.map((item) => {
          const borderColor = item.status === 'PENDING_MAKER' ? '#f59e0b' : item.status === 'PENDING_CHECKER' ? '#2563eb' : item.status === 'COMPLETED' ? '#10b981' : '#ef4444'
          return (
            <div key={item.workflowId} className="card" style={{ 
              padding: 24,
              borderLeft: `4px solid ${borderColor}`
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ display: 'flex', gap: 20 }}>
                  {/* Icon */}
                  <div style={{
                    width: 56,
                    height: 56,
                    borderRadius: 12,
                    background: 'linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'white'
                  }}>
                    <ArrowRightLeft size={24} />
                  </div>
                  
                  {/* Details */}
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                      <h3 style={{ fontSize: 18, fontWeight: 600, color: '#1e293b' }}>{item.agentName}</h3>
                      {getStatusBadge(item.status)}
                      {getActionBadge(item.makerAction)}
                    </div>
                    <p style={{ fontSize: 14, color: '#64748b', marginBottom: 4 }}>
                      Workflow: {item.workflowId} | Transaction: {item.transactionId}
                    </p>
                    <p style={{ fontSize: 14, color: '#64748b', marginBottom: 12 }}>
                      {item.transactionType} | {item.currency} {item.amount.toLocaleString()}
                    </p>
                    
                    {item.makerReason && (
                      <div style={{
                        marginTop: 12,
                        padding: '8px 12px',
                        background: '#f8fafc',
                        borderRadius: 6,
                        fontSize: 13,
                        color: '#475569',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8
                      }}>
                        <FileCheck size={14} />
                        Maker: {item.makerReason}
                      </div>
                    )}
                    
                    {item.checkerReason && (
                      <div style={{
                        marginTop: 8,
                        padding: '8px 12px',
                        background: item.status === 'REJECTED' ? '#fef2f2' : '#f0fdf4',
                        borderRadius: 6,
                        fontSize: 13,
                        color: item.status === 'REJECTED' ? '#dc2626' : '#16a34a',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8
                      }}>
                        {item.status === 'REJECTED' ? <XCircle size={14} /> : <CheckCircle size={14} />}
                        Checker: {item.checkerReason}
                      </div>
                    )}
                  </div>
                </div>
                
                {/* Actions */}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn btn-outline btn-sm" onClick={() => setSelectedItem(item)} data-testid={`view-${item.workflowId}-button`}>
                    <Eye size={14} />
                    View
                  </button>
                  {item.status === 'PENDING_MAKER' && (
                    <button className="btn btn-secondary btn-sm" onClick={() => { setSelectedItem(item); setShowProposalModal(true); }}>
                      <FileCheck size={14} />
                      Propose
                    </button>
                  )}
                  {item.status === 'PENDING_CHECKER' && (
                    <>
                      <button className="btn btn-secondary btn-sm" onClick={() => handleApprove(item)} data-testid={`approve-${item.workflowId}-button`}>
                        <CheckCircle size={14} />
                        Approve
                      </button>
                      <button className="btn btn-outline btn-sm" onClick={() => handleReject(item)} style={{ color: '#ef4444', borderColor: '#ef4444' }} data-testid={`reject-${item.workflowId}-button`}>
                        <XCircle size={14} />
                        Reject
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>
          )
        })}
      </div>

      {/* Pagination */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: 16,
        background: 'white',
        borderRadius: 12,
        border: '1px solid #e2e8f0'
      }}>
        <p style={{ fontSize: 13, color: '#64748b' }}>
          Showing {((safeCurrentPage - 1) * itemsPerPage) + 1} to {Math.min(safeCurrentPage * itemsPerPage, filteredItems.length)} of {filteredItems.length} items
        </p>
        <div style={{ display: 'flex', gap: 8 }}>
          <button 
            className="btn btn-outline btn-sm" 
            onClick={handlePrevPage}
            disabled={safeCurrentPage === 1}
          >
            <ChevronLeft size={16} />
            Previous
          </button>
          {totalPages > 0 && Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
            <button 
              key={page}
              className={`btn btn-sm ${page === safeCurrentPage ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => handlePageChange(page)}
            >
              {page}
            </button>
          ))}
          <button 
            className="btn btn-outline btn-sm"
            onClick={handleNextPage}
            disabled={safeCurrentPage >= totalPages}
          >
            Next
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      {/* View Detail Modal */}
      {selectedItem && !showProposalModal && (
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
        }} onClick={() => setSelectedItem(null)}>
          <div style={{ 
            width: 520, 
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
              <h3 style={{ fontSize: 20, fontWeight: 600, margin: 0 }}>Transaction Resolution Details</h3>
              <button onClick={() => setSelectedItem(null)} style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}>
                <X size={20} color="#64748b" />
              </button>
            </div>
            <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ 
                display: 'flex', 
                alignItems: 'center', 
                gap: 16,
                padding: 20,
                background: '#f8fafc',
                borderRadius: 12
              }}>
                <div style={{
                  width: 56,
                  height: 56,
                  borderRadius: 16,
                  background: 'linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white'
                }}>
                  <ArrowRightLeft size={24} />
                </div>
                <div>
                  <p style={{ fontWeight: 600, fontSize: 18, margin: 0 }}>{selectedItem.agentName}</p>
                  <p style={{ fontSize: 13, color: '#64748b', margin: '4px 0 0 0', fontFamily: 'monospace' }}>{selectedItem.workflowId}</p>
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Transaction ID</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14, fontFamily: 'monospace' }}>{selectedItem.transactionId}</p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Amount</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14 }}>{selectedItem.currency} {selectedItem.amount.toLocaleString()}</p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Transaction Type</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14 }}>{selectedItem.transactionType}</p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Status</label>
                  <p style={{ margin: '8px 0 0 0' }}>{getStatusBadge(selectedItem.status)}</p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Agent ID</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14, fontFamily: 'monospace' }}>{selectedItem.agentId}</p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Created At</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14 }}>{new Date(selectedItem.createdAt).toLocaleString()}</p>
                </div>
              </div>
              {selectedItem.makerAction && (
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Maker Proposal</label>
                  <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
                    {getActionBadge(selectedItem.makerAction)}
                    <p style={{ fontWeight: 500, margin: 0, fontSize: 14 }}>{selectedItem.makerReason}</p>
                  </div>
                </div>
              )}
              {selectedItem.checkerReason && (
                <div style={{ background: selectedItem.status === 'REJECTED' ? '#fef2f2' : '#f0fdf4', padding: 16, borderRadius: 12, border: `1px solid ${selectedItem.status === 'REJECTED' ? '#fecaca' : '#bbf7d0'}` }}>
                  <label style={{ fontSize: 11, color: selectedItem.status === 'REJECTED' ? '#dc2626' : '#16a34a', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Checker Decision</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14, color: selectedItem.status === 'REJECTED' ? '#dc2626' : '#16a34a' }}>{selectedItem.checkerReason}</p>
                </div>
              )}
            </div>
            <div style={{ padding: '0 24px 24px', display: 'flex', gap: 12 }}>
              {selectedItem.status === 'PENDING_MAKER' && (
                <button className="btn btn-secondary" onClick={() => { setShowProposalModal(true); }} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                  <FileCheck size={16} /> Propose Resolution
                </button>
              )}
              {selectedItem.status === 'PENDING_CHECKER' && (
                <>
                  <button className="btn btn-secondary" onClick={() => { handleApprove(selectedItem); setSelectedItem(null); }} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                    <CheckCircle size={16} /> Approve
                  </button>
                  <button className="btn btn-outline" onClick={() => { handleReject(selectedItem); setSelectedItem(null); }} style={{ flex: 1, color: '#ef4444', borderColor: '#ef4444', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                    <XCircle size={16} /> Reject
                  </button>
                </>
              )}
              <button className="btn btn-outline" onClick={() => setSelectedItem(null)} style={{ flex: 1 }}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Maker Proposal Modal */}
      {showProposalModal && selectedItem && (
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
        }} onClick={() => setShowProposalModal(false)}>
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
              <h3 style={{ fontSize: 20, fontWeight: 600, margin: 0 }}>Propose Resolution</h3>
              <button onClick={() => setShowProposalModal(false)} style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}>
                <X size={20} color="#64748b" />
              </button>
            </div>
            <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div>
                <label style={{ fontSize: 13, fontWeight: 500, color: '#374151', display: 'block', marginBottom: 6 }}>Action *</label>
                <div style={{ display: 'flex', gap: 12 }}>
                  <button
                    type="button"
                    onClick={() => setProposalForm({ ...proposalForm, action: 'COMMIT' })}
                    style={{
                      flex: 1,
                      padding: 12,
                      border: `2px solid ${proposalForm.action === 'COMMIT' ? '#10b981' : '#e2e8f0'}`,
                      borderRadius: 8,
                      background: proposalForm.action === 'COMMIT' ? '#d1fae5' : 'white',
                      color: proposalForm.action === 'COMMIT' ? '#059669' : '#64748b',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 8,
                      fontWeight: 600
                    }}
                  >
                    <CheckCircle size={18} /> Commit
                  </button>
                  <button
                    type="button"
                    onClick={() => setProposalForm({ ...proposalForm, action: 'REVERSE' })}
                    style={{
                      flex: 1,
                      padding: 12,
                      border: `2px solid ${proposalForm.action === 'REVERSE' ? '#ef4444' : '#e2e8f0'}`,
                      borderRadius: 8,
                      background: proposalForm.action === 'REVERSE' ? '#fee2e2' : 'white',
                      color: proposalForm.action === 'REVERSE' ? '#dc2626' : '#64748b',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 8,
                      fontWeight: 600
                    }}
                  >
                    <Undo2 size={18} /> Reverse
                  </button>
                </div>
              </div>
              <div>
                <label style={{ fontSize: 13, fontWeight: 500, color: '#374151', display: 'block', marginBottom: 6 }}>Reason Code *</label>
                <select 
                  className="input"
                  value={proposalForm.reasonCode}
                  onChange={(e) => setProposalForm({ ...proposalForm, reasonCode: e.target.value })}
                  style={{ width: '100%' }}
                >
                  <option value="">Select reason code</option>
                  <option value="DUPLICATE_TX">Duplicate Transaction</option>
                  <option value="WRONG_AMOUNT">Wrong Amount</option>
                  <option value="WRONG_RECIPIENT">Wrong Recipient</option>
                  <option value="SYSTEM_ERROR">System Error</option>
                  <option value="FRAUD_INVESTIGATION">Fraud Investigation</option>
                  <option value="CUSTOMER_REQUEST">Customer Request</option>
                </select>
              </div>
              <div>
                <label style={{ fontSize: 13, fontWeight: 500, color: '#374151', display: 'block', marginBottom: 6 }}>Reason *</label>
                <textarea 
                  className="input"
                  value={proposalForm.reason}
                  onChange={(e) => setProposalForm({ ...proposalForm, reason: e.target.value })}
                  placeholder="Describe the reason for this resolution..."
                  rows={3}
                  style={{ width: '100%', resize: 'vertical' }}
                />
              </div>
              <div>
                <label style={{ fontSize: 13, fontWeight: 500, color: '#374151', display: 'block', marginBottom: 6 }}>Evidence URL (optional)</label>
                <input 
                  type="text"
                  className="input"
                  value={proposalForm.evidenceUrl}
                  onChange={(e) => setProposalForm({ ...proposalForm, evidenceUrl: e.target.value })}
                  placeholder="https://..."
                  style={{ width: '100%' }}
                />
              </div>
            </div>
            <div style={{ padding: '0 24px 24px', display: 'flex', gap: 12 }}>
              <button className="btn btn-secondary" onClick={handlePropose} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                <FileCheck size={16} /> Submit Proposal
              </button>
              <button className="btn btn-outline" onClick={() => setShowProposalModal(false)} style={{ flex: 1 }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
