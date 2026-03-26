import { useQuery } from '@tanstack/react-query'
import { 
  Search, 
  Filter, 
  CheckCircle,
  XCircle,
  Clock,
  AlertTriangle,
  Shield,
  Eye
} from 'lucide-react'
import api from '../api/client'

const mockKycItems = [
  { id: 'KYC-001', mykad: '850123-01-1234', name: 'Ali Bin Ahmad', status: 'Pending', biometric: 'Match', aml: 'Clean', rejection: null, priority: 'High', time: '2h ago' },
  { id: 'KYC-002', mykad: '920415-05-5678', name: 'Muthu Kumar', status: 'Pending', biometric: 'Low Match', aml: 'Clean', rejection: 'Biometric quality below threshold', priority: 'Medium', time: '4h ago' },
  { id: 'KYC-003', mykad: '880722-02-9012', name: 'Priya Devi', status: 'Pending', biometric: 'Match', aml: 'Flagged', rejection: 'AML screening requires review', priority: 'High', time: '5h ago' },
  { id: 'KYC-004', mykad: '951103-08-3456', name: 'Tan Kah Seng', status: 'Pending', biometric: 'No Match', aml: 'Clean', rejection: 'Biometric not detected', priority: 'Low', time: '6h ago' },
]

export function KycReview() {
  useQuery({
    queryKey: ['kyc-review'],
    queryFn: () => api.getKycReviewQueue(),
  })

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }}>
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
          { label: 'Pending Review', value: '23', icon: Clock, color: '#f59e0b' },
          { label: 'Approved Today', value: '156', icon: CheckCircle, color: '#10b981' },
          { label: 'Rejected Today', value: '12', icon: XCircle, color: '#ef4444' },
          { label: 'AML Flags', value: '5', icon: AlertTriangle, color: '#ef4444' },
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
        {mockKycItems.map((item, index) => (
          <div key={index} className="card" style={{ 
            padding: 24,
            borderLeft: `4px solid ${
              item.priority === 'High' ? '#ef4444' : 
              item.priority === 'Medium' ? '#f59e0b' : '#10b981'
            }`
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
                  {item.name.split(' ').map(n => n[0]).join('')}
                </div>
                
                {/* Details */}
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                    <h3 style={{ fontSize: 18, fontWeight: 600, color: '#1e293b' }}>{item.name}</h3>
                    <span style={{
                      padding: '4px 10px',
                      borderRadius: 4,
                      fontSize: 12,
                      fontWeight: 600,
                      background: item.priority === 'High' ? '#fef2f2' : item.priority === 'Medium' ? '#fffbeb' : '#f0fdf4',
                      color: item.priority === 'High' ? '#ef4444' : item.priority === 'Medium' ? '#f59e0b' : '#10b981'
                    }}>
                      {item.priority} Priority
                    </span>
                  </div>
                  <p style={{ fontSize: 14, color: '#64748b', marginBottom: 12 }}>
                    ID: {item.id} | MyKad: {item.mykad}
                  </p>
                  
                  {/* Badges */}
                  <div style={{ display: 'flex', gap: 12 }}>
                    <span className={`badge ${item.biometric === 'Match' ? 'badge-success' : item.biometric === 'Low Match' ? 'badge-warning' : 'badge-error'}`}>
                      Biometric: {item.biometric}
                    </span>
                    <span className={`badge ${item.aml === 'Clean' ? 'badge-success' : 'badge-error'}`}>
                      AML: {item.aml}
                    </span>
                    <span className="badge badge-info">{item.time}</span>
                  </div>
                  
                  {item.rejection && (
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
                      {item.rejection}
                    </div>
                  )}
                </div>
              </div>
              
              {/* Actions */}
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-outline btn-sm">
                  <Eye size={14} />
                  View
                </button>
                <button className="btn btn-secondary btn-sm">
                  <CheckCircle size={14} />
                  Approve
                </button>
                <button className="btn btn-outline btn-sm" style={{ color: '#ef4444', borderColor: '#ef4444' }}>
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
        justifyContent: 'center',
        padding: 16,
        background: 'white',
        borderRadius: 12,
        border: '1px solid #e2e8f0'
      }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-outline btn-sm">Previous</button>
          {[1, 2, 3].map(page => (
            <button 
              key={page}
              className={`btn btn-sm ${page === 1 ? 'btn-primary' : 'btn-outline'}`}
            >
              {page}
            </button>
          ))}
          <button className="btn btn-outline btn-sm">Next</button>
        </div>
      </div>
    </div>
  )
}