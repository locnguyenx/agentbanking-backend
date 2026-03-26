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
  XCircle
} from 'lucide-react'
import api from '../api/client'

const mockTransactions = [
  { id: 'TXN-20260326-001', type: 'Deposit', amount: 'RM 5,000.00', status: 'Success', agent: 'Ahmad Razak', customer: 'Ali Bin Ahmad', time: '08:45:12', ref: 'DEP-123456' },
  { id: 'TXN-20260326-002', type: 'Withdrawal', amount: 'RM 2,500.00', status: 'Success', agent: 'Siti Aminah', customer: 'Muthu Kumar', time: '09:12:34', ref: 'WDR-234567' },
  { id: 'TXN-20260326-003', type: 'Bill Pay', amount: 'RM 156.50', status: 'Success', agent: 'Lee Ming Wei', customer: 'Tan Kah Seng', time: '10:23:45', ref: 'BIL-345678' },
  { id: 'TXN-20260326-004', type: 'Topup', amount: 'RM 30.00', status: 'Pending', agent: 'Nurul Huda', customer: 'Priya Devi', time: '11:34:56', ref: 'TOP-456789' },
  { id: 'TXN-20260326-005', type: 'Deposit', amount: 'RM 8,000.00', status: 'Failed', agent: 'Ahmad Razak', customer: 'Zakir Bin Hassan', time: '12:45:01', ref: 'DEP-567890' },
  { id: 'TXN-20260326-006', type: 'Withdrawal', amount: 'RM 1,200.00', status: 'Success', agent: 'Mohd Faisal', customer: 'Ravi Shankar', time: '13:15:23', ref: 'WDR-678901' },
  { id: 'TXN-20260326-007', type: 'DuitNow', amount: 'RM 500.00', status: 'Success', agent: 'Tan Kah Seng', customer: 'Aisha Binti Omar', time: '14:22:34', ref: 'DNI-789012' },
]

export function Transactions() {
  useQuery({
    queryKey: ['transactions'],
    queryFn: () => api.getTransactions(),
  })

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }}>
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
          <button className="btn btn-outline">
            <Download size={18} />
            Export
          </button>
        </div>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        {[
          { label: 'Total Transactions', value: '4,521', icon: RefreshCw, color: '#1e3a5f' },
          { label: 'Successful', value: '4,432', icon: CheckCircle, color: '#10b981' },
          { label: 'Pending', value: '47', icon: Clock, color: '#f59e0b' },
          { label: 'Failed', value: '42', icon: XCircle, color: '#ef4444' },
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
            />
          </div>
          <input 
            type="date" 
            className="input" 
            style={{ width: 160 }}
            defaultValue="2026-03-26"
          />
          <select className="input" style={{ width: 140 }}>
            <option>All Types</option>
            <option>Deposit</option>
            <option>Withdrawal</option>
            <option>Bill Pay</option>
            <option>Topup</option>
            <option>DuitNow</option>
          </select>
          <select className="input" style={{ width: 140 }}>
            <option>All Status</option>
            <option>Success</option>
            <option>Pending</option>
            <option>Failed</option>
          </select>
          <select className="input" style={{ width: 160 }}>
            <option>All Agents</option>
            <option>Ahmad Razak</option>
            <option>Siti Aminah</option>
            <option>Mohd Faisal</option>
            <option>Lee Ming Wei</option>
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
                <th>Reference</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Agent</th>
                <th>Customer</th>
                <th>Time</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {mockTransactions.map((txn, index) => (
                <tr key={index}>
                  <td>
                    <input type="checkbox" style={{ width: 16, height: 16 }} />
                  </td>
                  <td style={{ fontFamily: 'monospace', color: '#1e3a5f', fontWeight: 500, fontSize: 13 }}>
                    {txn.id}
                  </td>
                  <td style={{ fontFamily: 'monospace', color: '#64748b', fontSize: 13 }}>
                    {txn.ref}
                  </td>
                  <td>
                    <span className="badge badge-info">{txn.type}</span>
                  </td>
                  <td style={{ fontWeight: 600 }}>{txn.amount}</td>
                  <td>
                    <span className={`badge ${
                      txn.status === 'Success' ? 'badge-success' : 
                      txn.status === 'Pending' ? 'badge-warning' : 'badge-error'
                    }`}>
                      {txn.status === 'Success' && <CheckCircle size={12} style={{ marginRight: 4 }} />}
                      {txn.status === 'Pending' && <Clock size={12} style={{ marginRight: 4 }} />}
                      {txn.status === 'Failed' && <XCircle size={12} style={{ marginRight: 4 }} />}
                      {txn.status}
                    </span>
                  </td>
                  <td style={{ fontWeight: 500 }}>{txn.agent}</td>
                  <td style={{ color: '#64748b' }}>{txn.customer}</td>
                  <td style={{ color: '#64748b', fontFamily: 'monospace' }}>{txn.time}</td>
                  <td>
                    <button style={{ 
                      padding: 8, 
                      background: 'transparent', 
                      border: 'none', 
                      cursor: 'pointer',
                      borderRadius: 6
                    }}>
                      <MoreVertical size={16} color="#94a3b8" />
                    </button>
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
            Showing 1 to 7 of 4,521 transactions
          </p>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-outline btn-sm" disabled style={{ opacity: 0.5 }}>
              <ChevronLeft size={16} />
              Previous
            </button>
            {[1, 2, 3, '...', 50].map((page, i) => (
              <button 
                key={i}
                className={`btn btn-sm ${page === 1 ? 'btn-primary' : 'btn-outline'}`}
              >
                {page}
              </button>
            ))}
            <button className="btn btn-outline btn-sm">
              Next
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}