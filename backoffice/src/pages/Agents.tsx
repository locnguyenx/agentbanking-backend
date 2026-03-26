import { useQuery } from '@tanstack/react-query'
import { 
  Users, 
  Search, 
  Filter, 
  Download,
  ChevronLeft,
  ChevronRight,
  MoreVertical,
  MapPin,
  CheckCircle
} from 'lucide-react'
import api from '../api/client'

const mockAgents = [
  { id: 'AGT-001', name: 'Ahmad Razak', phone: '012-3456789', location: 'Kuala Lumpur', status: 'Active', transactions: 1245, tier: 'Gold' },
  { id: 'AGT-002', name: 'Siti Aminah', phone: '013-8765432', location: 'Petaling Jaya', status: 'Active', transactions: 982, tier: 'Silver' },
  { id: 'AGT-003', name: 'Mohd Faisal', phone: '014-2468135', location: 'Shah Alam', status: 'Pending', transactions: 0, tier: 'Bronze' },
  { id: 'AGT-004', name: 'Lee Ming Wei', phone: '016-9753124', location: 'Penang', status: 'Active', transactions: 2103, tier: 'Gold' },
  { id: 'AGT-005', name: 'Nurul Huda', phone: '011-6543210', location: 'Johor Bahru', status: 'Inactive', transactions: 567, tier: 'Silver' },
  { id: 'AGT-006', name: 'Tan Kah Seng', phone: '017-1234567', location: 'Ipoh', status: 'Active', transactions: 890, tier: 'Bronze' },
]

export function Agents() {
  useQuery({
    queryKey: ['agents'],
    queryFn: () => api.getAgents(),
  })

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
        <button className="btn btn-primary">
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
            />
          </div>
          <select className="input" style={{ width: 150 }}>
            <option>All Status</option>
            <option>Active</option>
            <option>Inactive</option>
            <option>Pending</option>
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
        {[
          { label: 'Total Agents', value: '2,847', color: '#1e3a5f' },
          { label: 'Active', value: '2,341', color: '#10b981' },
          { label: 'Pending', value: '47', color: '#f59e0b' },
          { label: 'Inactive', value: '459', color: '#ef4444' },
        ].map((stat, index) => (
          <div key={index} className="card" style={{ padding: 20, textAlign: 'center' }}>
            <p style={{ fontSize: 13, color: '#64748b', marginBottom: 4 }}>{stat.label}</p>
            <h3 style={{ fontSize: 24, fontWeight: 700, color: stat.color }}>{stat.value}</h3>
          </div>
        ))}
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
                <th>Agent ID</th>
                <th>Name</th>
                <th>Phone</th>
                <th>Location</th>
                <th>Status</th>
                <th>Transactions</th>
                <th>Tier</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {mockAgents.map((agent, index) => (
                <tr key={index}>
                  <td>
                    <input type="checkbox" style={{ width: 16, height: 16 }} />
                  </td>
                  <td style={{ fontFamily: 'monospace', color: '#1e3a5f', fontWeight: 500 }}>
                    {agent.id}
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
                        {agent.name.split(' ').map(n => n[0]).join('')}
                      </div>
                      <div>
                        <div style={{ fontWeight: 500 }}>{agent.name}</div>
                      </div>
                    </div>
                  </td>
                  <td style={{ color: '#64748b' }}>{agent.phone}</td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <MapPin size={14} color="#64748b" />
                      {agent.location}
                    </div>
                  </td>
                  <td>
                    <span className={`badge ${
                      agent.status === 'Active' ? 'badge-success' : 
                      agent.status === 'Pending' ? 'badge-warning' : 'badge-error'
                    }`}>
                      {agent.status === 'Active' && <CheckCircle size={12} style={{ marginRight: 4 }} />}
                      {agent.status}
                    </span>
                  </td>
                  <td style={{ fontWeight: 500 }}>{agent.transactions.toLocaleString()}</td>
                  <td>
                    <span style={{
                      padding: '4px 10px',
                      borderRadius: 4,
                      fontSize: 12,
                      fontWeight: 600,
                      background: agent.tier === 'Gold' ? '#fef3c7' : agent.tier === 'Silver' ? '#f1f5f9' : '#fed7aa',
                      color: agent.tier === 'Gold' ? '#92400e' : agent.tier === 'Silver' ? '#475569' : '#9a3412'
                    }}>
                      {agent.tier}
                    </span>
                  </td>
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
            Showing 1 to 6 of 2,847 agents
          </p>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-outline btn-sm" disabled style={{ opacity: 0.5 }}>
              <ChevronLeft size={16} />
              Previous
            </button>
            {[1, 2, 3].map(page => (
              <button 
                key={page}
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