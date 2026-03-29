import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { 
  Search, 
  Filter, 
  Download,
  ChevronLeft,
  ChevronRight,
  MoreVertical,
  RefreshCw,
  Clock,
  CheckCircle,
  XCircle,
  X
} from 'lucide-react'
import api from '../api/client'

interface Transaction {
  transactionId: string
  transactionType: string
  amount: number
  status: string
  agentId: string
  createdAt: string
}

export function Transactions() {
  const [currentPage, setCurrentPage] = useState(1)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('All')
  const [selectedTxnId, setSelectedTxnId] = useState<string | null>(null)
  const [viewTxn, setViewTxn] = useState<Transaction | null>(null)
  const itemsPerPage = 10

  const { data: response } = useQuery({
    queryKey: ['transactions'],
    queryFn: async () => {
      const res = await api.getTransactions()
      return res as { content: Transaction[]; totalElements: number }
    }
  })

  const transactions = response?.content || []

  const filteredTransactions = transactions.filter(txn => {
    const matchesSearch = searchTerm === '' || 
      txn.transactionId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      txn.transactionType.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesStatus = statusFilter === 'All' || 
      (statusFilter === 'COMPLETED' && txn.status === 'COMPLETED') ||
      (statusFilter === 'PENDING' && txn.status === 'PENDING') ||
      (statusFilter === 'FAILED' && txn.status === 'FAILED')
    return matchesSearch && matchesStatus
  })

  const totalPages = Math.max(1, Math.ceil(filteredTransactions.length / itemsPerPage))
  
  // Reset to page 1 if current page exceeds total pages
  const safeCurrentPage = currentPage > totalPages ? 1 : currentPage
  
  const paginatedTransactions = filteredTransactions.slice(
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

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value)
    setCurrentPage(1) // Reset to first page on search
  }

  const handleStatusFilter = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setStatusFilter(e.target.value)
    setCurrentPage(1) // Reset to first page on filter change
  }

  const handleTxnAction = (txnId: string) => {
    setSelectedTxnId(selectedTxnId === txnId ? null : txnId)
  }

  const handleViewTransaction = (txn: Transaction) => {
    setSelectedTxnId(null)
    setViewTxn(txn)
  }

  const handleRefundTransaction = (txnId: string) => {
    if (confirm(`Refund transaction ${txnId}?`)) {
      alert(`Transaction ${txnId} refunded`)
    }
    setSelectedTxnId(null)
  }

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'Success'
      case 'PENDING': return 'Pending'
      case 'FAILED': return 'Failed'
      default: return status
    }
  }

  const getTypeLabel = (type: string) => {
    switch (type) {
      case 'CASH_DEPOSIT': return 'Deposit'
      case 'CASH_WITHDRAWAL': return 'Withdrawal'
      case 'BILL_PAYMENT': return 'Bill Pay'
      case 'PREPAID_TOPUP': return 'Topup'
      case 'DUITNOW_TRANSFER': return 'DuitNow'
      default: return type
    }
  }

  const formatAmount = (amount: number) => {
    return 'RM ' + amount.toLocaleString('en-MY', { minimumFractionDigits: 2 })
  }

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr)
    return date.toLocaleTimeString('en-MY', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  }

  const totalTxns = response?.totalElements || filteredTransactions.length

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }} data-testid="page-title">
            Transaction History
          </h2>
          <p style={{ fontSize: 14, color: '#64748b' }}>
            View and manage all transactions
          </p>
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <button className="btn btn-outline">
            <RefreshCw size={18} />
            Refresh
          </button>
          <button className="btn btn-outline" data-testid="export-button">
            <Download size={18} />
            Export
          </button>
        </div>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        {[
          { label: 'Total Transactions', value: totalTxns.toString(), icon: RefreshCw, color: '#1e3a5f' },
          { label: 'Successful', value: transactions.filter(t => t.status === 'COMPLETED').length.toString(), icon: CheckCircle, color: '#10b981' },
          { label: 'Pending', value: transactions.filter(t => t.status === 'PENDING').length.toString(), icon: Clock, color: '#f59e0b' },
          { label: 'Failed', value: transactions.filter(t => t.status === 'FAILED').length.toString(), icon: XCircle, color: '#ef4444' },
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
                <h3 style={{ fontSize: 24, fontWeight: 700, color: stat.color }}>{stat.value}</h3>
              </div>
            </div>
          )
        })}
      </div>

      {/* Filters */}
      <div className="card" style={{ padding: 16 }}>
        <div style={{ display: 'flex', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ position: 'relative', flex: 1, minWidth: 200 }}>
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
              placeholder="Search by transaction ID, agent, customer..."
              className="input"
              style={{ paddingLeft: 42 }}
              value={searchTerm}
              onChange={handleSearch}
              data-testid="search-input"
            />
          </div>
          <input 
            type="date" 
            className="input" 
            style={{ width: 160 }}
          />
          <select className="input" style={{ width: 140 }}>
            <option>All Types</option>
            <option>Deposit</option>
            <option>Withdrawal</option>
            <option>Bill Pay</option>
            <option>Topup</option>
            <option>DuitNow</option>
          </select>
          <select className="input" style={{ width: 140 }} value={statusFilter} onChange={handleStatusFilter} data-testid="status-filter">
            <option value="All">All Status</option>
            <option value="COMPLETED">Success</option>
            <option value="PENDING">Pending</option>
            <option value="FAILED">Failed</option>
          </select>
          <button className="btn btn-outline">
            <Filter size={18} />
            More Filters
          </button>
        </div>
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
                <th>Transaction ID</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Agent ID</th>
                <th>Time</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {paginatedTransactions.map((txn) => (
                <tr key={txn.transactionId}>
                  <td>
                    <input type="checkbox" style={{ width: 16, height: 16 }} />
                  </td>
                  <td style={{ fontFamily: 'monospace', color: '#1e3a5f', fontWeight: 500, fontSize: 13 }}>
                    {txn.transactionId}
                  </td>
                  <td>
                    <span className="badge badge-info">{getTypeLabel(txn.transactionType)}</span>
                  </td>
                  <td style={{ fontWeight: 600 }}>{formatAmount(txn.amount)}</td>
                  <td>
                    <span className={`badge ${
                      txn.status === 'COMPLETED' ? 'badge-success' : 
                      txn.status === 'PENDING' ? 'badge-warning' : 'badge-error'
                    }`}>
                      {txn.status === 'COMPLETED' && <CheckCircle size={12} style={{ marginRight: 4 }} />}
                      {txn.status === 'PENDING' && <Clock size={12} style={{ marginRight: 4 }} />}
                      {txn.status === 'FAILED' && <XCircle size={12} style={{ marginRight: 4 }} />}
                      {getStatusLabel(txn.status)}
                    </span>
                  </td>
                  <td style={{ fontFamily: 'monospace', color: '#64748b', fontSize: 13 }}>{txn.agentId.substring(0, 8)}...</td>
                  <td style={{ color: '#64748b', fontFamily: 'monospace' }}>{formatTime(txn.createdAt)}</td>
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
                        onClick={() => handleTxnAction(txn.transactionId)}
                        data-testid={`action-${txn.transactionId}-button`}
                      >
                        <MoreVertical size={16} color="#94a3b8" />
                      </button>
                      {selectedTxnId === txn.transactionId && (
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
                            onClick={() => handleViewTransaction(txn)}
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
                            onClick={() => handleRefundTransaction(txn.transactionId)}
                          >
                            Refund
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
            Showing {((safeCurrentPage - 1) * itemsPerPage) + 1} to {Math.min(safeCurrentPage * itemsPerPage, filteredTransactions.length)} of {filteredTransactions.length} transactions
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

      {/* View Transaction Modal */}
      {viewTxn && (
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
        }} onClick={() => setViewTxn(null)}>
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
              <h3 style={{ fontSize: 20, fontWeight: 600, margin: 0 }}>Transaction Details</h3>
              <button onClick={() => setViewTxn(null)} style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}>
                <X size={20} color="#64748b" />
              </button>
            </div>
            <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ 
                textAlign: 'center', 
                padding: 24, 
                background: viewTxn.status === 'COMPLETED' ? '#ecfdf5' : viewTxn.status === 'FAILED' ? '#fef2f2' : '#fffbeb',
                borderRadius: 12,
                border: `1px solid ${viewTxn.status === 'COMPLETED' ? '#a7f3d0' : viewTxn.status === 'FAILED' ? '#fecaca' : '#fde68a'}`
              }}>
                <p style={{ fontSize: 13, color: '#64748b', margin: '0 0 8px 0', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                  {getTypeLabel(viewTxn.transactionType)}
                </p>
                <p style={{ fontSize: 32, fontWeight: 700, margin: 0, color: viewTxn.status === 'COMPLETED' ? '#059669' : viewTxn.status === 'FAILED' ? '#dc2626' : '#d97706' }}>
                  {formatAmount(viewTxn.amount)}
                </p>
                <p style={{ margin: '12px 0 0 0' }}>
                  <span className={`badge ${
                    viewTxn.status === 'COMPLETED' ? 'badge-success' : 
                    viewTxn.status === 'FAILED' ? 'badge-error' : 'badge-warning'
                  }`}>
                    {getStatusLabel(viewTxn.status)}
                  </span>
                </p>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Transaction ID</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 13, fontFamily: 'monospace', wordBreak: 'break-all' }}>{viewTxn.transactionId}</p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Agent ID</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14 }}>{viewTxn.agentId}</p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Date</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14 }}>{new Date(viewTxn.createdAt).toLocaleDateString('en-MY')}</p>
                </div>
                <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Time</label>
                  <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 14 }}>{formatTime(viewTxn.createdAt)}</p>
                </div>
              </div>
            </div>
            <div style={{ padding: '0 24px 24px', display: 'flex', gap: 12 }}>
              <button className="btn btn-outline" onClick={() => setViewTxn(null)} style={{ flex: 1 }}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
