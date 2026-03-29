import { useState } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { 
  Search, 
  Filter, 
  CheckCircle,
  XCircle,
  Clock,
  AlertTriangle,
  Shield,
  Eye,
  ChevronLeft,
  ChevronRight,
  X
} from 'lucide-react'
import api from '../api/client'

interface KycItem {
  verificationId: string
  mykadMasked: string
  fullName: string
  biometricMatch: string
  amlStatus: string
  rejectionReason: string | null
}

export function KycReview() {
  const queryClient = useQueryClient()
  const [currentPage, setCurrentPage] = useState(1)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedKyc, setSelectedKyc] = useState<KycItem | null>(null)
  const itemsPerPage = 10

  const { data: kycResponse } = useQuery({
    queryKey: ['kyc-review'],
    queryFn: async () => {
      const response = await api.getKycReviewQueue()
      return response as { content: KycItem[] }
    }
  })

  const kycItems = kycResponse?.content || []

  const approveKycMutation = useMutation({
    mutationFn: (id: string) => api.approveKyc(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kyc-review'] })
      alert('KYC approved successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to approve KYC: ${error.message}`)
    }
  })

  const rejectKycMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => 
      api.rejectKyc(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kyc-review'] })
      alert('KYC rejected successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to reject KYC: ${error.message}`)
    }
  })

  const filteredItems = kycItems.filter(item => {
    const matchesSearch = searchTerm === '' || 
      item.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.mykadMasked.includes(searchTerm) ||
      item.verificationId.toLowerCase().includes(searchTerm.toLowerCase())
    return matchesSearch
  })

  const totalPages = Math.max(1, Math.ceil(filteredItems.length / itemsPerPage))
  
  // Reset to page 1 if current page exceeds total pages
  const safeCurrentPage = currentPage > totalPages ? 1 : currentPage
  
  const paginatedItems = filteredItems.slice(
    (safeCurrentPage - 1) * itemsPerPage,
    safeCurrentPage * itemsPerPage
  )

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value)
    setCurrentPage(1) // Reset to first page on search
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

  const handleApprove = (id: string) => {
    if (confirm(`Approve KYC verification for ${id}?`)) {
      approveKycMutation.mutate(id)
    }
  }

  const handleReject = (id: string) => {
    const reason = prompt('Enter rejection reason:')
    if (reason) {
      rejectKycMutation.mutate({ id, reason })
    }
  }

  const handleViewKyc = (item: KycItem) => {
    setSelectedKyc(selectedKyc?.verificationId === item.verificationId ? null : item)
  }

  // All items in review queue are pending for review
  const pendingCount = kycItems.length

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }} data-testid="page-title">
            KYC Review Queue
          </h2>
          <p style={{ fontSize: 14, color: '#64748b' }}>
            Manual review of pending verifications
          </p>
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <button className="btn btn-outline">
            <Shield size={18} />
            AML Check
          </button>
        </div>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        {[
          { label: 'Pending Review', value: pendingCount.toString(), icon: Clock, color: '#f59e0b' },
          { label: 'Approved Today', value: '0', icon: CheckCircle, color: '#10b981' },
          { label: 'Rejected Today', value: '0', icon: XCircle, color: '#ef4444' },
          { label: 'AML Flags', value: kycItems.filter(i => i.amlStatus === 'FLAGGED').length.toString(), icon: AlertTriangle, color: '#ef4444' },
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
              placeholder="Search by name, MyKad, or verification ID..."
              className="input"
              style={{ paddingLeft: 42 }}
              value={searchTerm}
              onChange={handleSearch}
              data-testid="search-input"
            />
          </div>
          <select className="input" style={{ width: 140 }}>
            <option>All Priority</option>
            <option>High</option>
            <option>Medium</option>
            <option>Low</option>
          </select>
          <select className="input" style={{ width: 160 }}>
            <option>All AML Status</option>
            <option>Clean</option>
            <option>Flagged</option>
          </select>
          <button className="btn btn-outline">
            <Filter size={18} />
            More Filters
          </button>
        </div>
      </div>

      {/* Review Cards */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {paginatedItems.map((item) => (
          <div key={item.verificationId} className="card" style={{ 
            padding: 24,
            borderLeft: `4px solid #f59e0b`
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div style={{ display: 'flex', gap: 20 }}>
                {/* Avatar */}
                <div style={{
                  width: 56,
                  height: 56,
                  borderRadius: 12,
                  background: 'linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  fontWeight: 700,
                  fontSize: 18
                }}>
                  {item.fullName.split(' ').map(n => n[0]).join('')}
                </div>
                
                {/* Details */}
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                    <h3 style={{ fontSize: 18, fontWeight: 600, color: '#1e293b' }}>{item.fullName}</h3>
                    <span style={{
                      padding: '4px 10px',
                      borderRadius: 4,
                      fontSize: 12,
                      fontWeight: 600,
                      background: '#fffbeb',
                      color: '#f59e0b'
                    }}>
                      Manual Review
                    </span>
                  </div>
                  <p style={{ fontSize: 14, color: '#64748b', marginBottom: 12 }}>
                    ID: {item.verificationId} | MyKad: {item.mykadMasked}
                  </p>
                  
                  {/* Badges */}
                  <div style={{ display: 'flex', gap: 12 }}>
                    <span className={`badge ${item.biometricMatch === 'MATCH' ? 'badge-success' : item.biometricMatch === 'LOW_MATCH' ? 'badge-warning' : 'badge-error'}`}>
                      Biometric: {item.biometricMatch === 'MATCH' ? 'Match' : item.biometricMatch === 'LOW_MATCH' ? 'Low Match' : 'No Match'}
                    </span>
                    <span className={`badge ${item.amlStatus === 'CLEAN' ? 'badge-success' : 'badge-error'}`}>
                      AML: {item.amlStatus}
                    </span>
                  </div>
                  
                  {item.rejectionReason && (
                    <div style={{
                      marginTop: 12,
                      padding: '8px 12px',
                      background: '#fef2f2',
                      borderRadius: 6,
                      fontSize: 13,
                      color: '#dc2626',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8
                    }}>
                      <AlertTriangle size={14} />
                      {item.rejectionReason}
                    </div>
                  )}
                </div>
              </div>
              
              {/* Actions */}
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-outline btn-sm" onClick={() => handleViewKyc(item)} data-testid={`view-${item.verificationId}-button`}>
                  <Eye size={14} />
                  View
                </button>
                <button className="btn btn-secondary btn-sm" onClick={() => handleApprove(item.verificationId)} data-testid={`approve-${item.verificationId}-button`}>
                  <CheckCircle size={14} />
                  Approve
                </button>
                <button className="btn btn-outline btn-sm" onClick={() => handleReject(item.verificationId)} style={{ color: '#ef4444', borderColor: '#ef4444' }} data-testid={`reject-${item.verificationId}-button`}>
                  <XCircle size={14} />
                  Reject
                </button>
              </div>
            </div>
          </div>
        ))}
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

      {/* View KYC Modal */}
      {selectedKyc && (
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
        }} onClick={() => setSelectedKyc(null)}>
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
              <h3 style={{ fontSize: 20, fontWeight: 600, margin: 0 }}>KYC Verification Details</h3>
              <button onClick={() => setSelectedKyc(null)} style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}>
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
                  color: 'white',
                  fontWeight: 600,
                  fontSize: 20
                }}>
                  {selectedKyc.fullName.split(' ').map(n => n[0]).join('')}
                </div>
                <div>
                  <p style={{ fontWeight: 600, fontSize: 18, margin: 0 }}>{selectedKyc.fullName}</p>
                  <p style={{ fontSize: 13, color: '#64748b', margin: '4px 0 0 0', fontFamily: 'monospace' }}>{selectedKyc.verificationId}</p>
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>MyKad Number</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14, fontFamily: 'monospace' }}>{selectedKyc.mykadMasked}</p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Biometric Match</label>
                  <p style={{ margin: '8px 0 0 0' }}>
                    <span className={`badge ${selectedKyc.biometricMatch === 'MATCHED' ? 'badge-success' : 'badge-error'}`}>
                      {selectedKyc.biometricMatch}
                    </span>
                  </p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>AML Status</label>
                  <p style={{ margin: '8px 0 0 0' }}>
                    <span className={`badge ${selectedKyc.amlStatus === 'CLEARED' ? 'badge-success' : selectedKyc.amlStatus === 'PENDING' ? 'badge-warning' : 'badge-error'}`}>
                      {selectedKyc.amlStatus}
                    </span>
                  </p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Verification Status</label>
                  <p style={{ margin: '8px 0 0 0' }}>
                    <span className="badge badge-warning">Pending Review</span>
                  </p>
                </div>
              </div>
              {selectedKyc.rejectionReason && (
                <div style={{ background: '#fef2f2', padding: 16, borderRadius: 12, border: '1px solid #fecaca' }}>
                  <label style={{ fontSize: 11, color: '#dc2626', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Previous Rejection Reason</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14, color: '#dc2626' }}>{selectedKyc.rejectionReason}</p>
                </div>
              )}
            </div>
            <div style={{ padding: '0 24px 24px', display: 'flex', gap: 12 }}>
              <button className="btn btn-secondary" onClick={() => { handleApprove(selectedKyc.verificationId); setSelectedKyc(null); }} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                <CheckCircle size={16} /> Approve
              </button>
              <button className="btn btn-outline" onClick={() => { handleReject(selectedKyc.verificationId); setSelectedKyc(null); }} style={{ flex: 1, color: '#ef4444', borderColor: '#ef4444', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                <XCircle size={16} /> Reject
              </button>
              <button className="btn btn-outline" onClick={() => setSelectedKyc(null)} style={{ flex: 1 }}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
