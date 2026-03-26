import { useQuery } from '@tanstack/react-query'
import { 
  TrendingUp, 
  TrendingDown, 
  Users, 
  ArrowLeftRight, 
  DollarSign,
  FileCheck,
  AlertCircle
} from 'lucide-react'
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import api from '../api/client'

const mockChartData = [
  { date: 'Mon', volume: 42000 },
  { date: 'Tue', volume: 38000 },
  { date: 'Wed', volume: 51000 },
  { date: 'Thu', volume: 47000 },
  { date: 'Fri', volume: 63000 },
  { date: 'Sat', volume: 58000 },
  { date: 'Sun', volume: 32000 },
]

const stats = [
  { 
    label: 'Total Agents', 
    value: '2,847', 
    change: '+12.5%', 
    trend: 'up',
    icon: Users,
    color: '#1e3a5f'
  },
  { 
    label: "Today's Volume", 
    value: 'RM 1.2M', 
    change: '+8.2%', 
    trend: 'up',
    icon: DollarSign,
    color: '#0d9488'
  },
  { 
    label: 'Transactions', 
    value: '4,521', 
    change: '-2.1%', 
    trend: 'down',
    icon: ArrowLeftRight,
    color: '#f59e0b'
  },
  { 
    label: 'Pending KYC', 
    value: '23', 
    change: '-15%', 
    trend: 'up',
    icon: FileCheck,
    color: '#ef4444'
  },
]

export function Dashboard() {
  const { isLoading, error } = useQuery({
    queryKey: ['dashboard'],
    queryFn: api.getDashboard,
  })

  if (isLoading) {
    return (
      <div className="loading">
        <div className="spinner" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="card" style={{ padding: 40, textAlign: 'center' }}>
        <AlertCircle size={48} color="#ef4444" style={{ marginBottom: 16 }} />
        <h3 style={{ color: '#1e293b', marginBottom: 8 }}>Unable to load dashboard</h3>
        <p style={{ color: '#64748b' }}>Please check your connection and try again.</p>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>
      {/* Stats Cards */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(4, 1fr)', 
        gap: 24 
      }}>
        {stats.map((stat, index) => {
          const Icon = stat.icon
          return (
            <div 
              key={index}
              className="card"
              style={{ 
                padding: 24,
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between'
              }}
            >
              <div>
                <p style={{ 
                  fontSize: 13, 
                  color: '#64748b', 
                  marginBottom: 8,
                  fontWeight: 500
                }}>
                  {stat.label}
                </p>
                <h3 style={{ 
                  fontSize: 28, 
                  fontWeight: 700, 
                  color: '#1e293b',
                  marginBottom: 8
                }}>
                  {stat.value}
                </h3>
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: 6 
                }}>
                  {stat.trend === 'up' ? (
                    <TrendingUp size={16} color="#10b981" />
                  ) : (
                    <TrendingDown size={16} color="#ef4444" />
                  )}
                  <span style={{
                    fontSize: 13,
                    color: stat.trend === 'up' ? '#10b981' : '#ef4444',
                    fontWeight: 500
                  }}>
                    {stat.change}
                  </span>
                  <span style={{ fontSize: 12, color: '#94a3b8' }}>vs last week</span>
                </div>
              </div>
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
            </div>
          )
        })}
      </div>

      {/* Chart Section */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 24 }}>
        {/* Volume Chart */}
        <div className="card" style={{ padding: 24 }}>
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'space-between',
            marginBottom: 24
          }}>
            <div>
              <h3 style={{ fontSize: 18, fontWeight: 600, color: '#1e293b' }}>
                Transaction Volume
              </h3>
              <p style={{ fontSize: 13, color: '#64748b' }}>Last 7 days</p>
            </div>
            <select style={{
              padding: '8px 12px',
              border: '1px solid #e2e8f0',
              borderRadius: 8,
              fontSize: 13,
              color: '#64748b',
              background: 'white'
            }}>
              <option>Last 7 days</option>
              <option>Last 30 days</option>
              <option>Last 90 days</option>
            </select>
          </div>
          <div style={{ height: 280 }}>
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={mockChartData}>
                <defs>
                  <linearGradient id="volumeGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#1e3a5f" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#1e3a5f" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis 
                  dataKey="date" 
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: '#94a3b8', fontSize: 12 }}
                />
                <YAxis 
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: '#94a3b8', fontSize: 12 }}
                  tickFormatter={(value) => `RM ${value / 1000}k`}
                />
                <Tooltip 
                  contentStyle={{
                    background: '#1e293b',
                    border: 'none',
                    borderRadius: 8,
                    color: 'white'
                  }}
                  formatter={(value) => [`RM ${(value as number).toLocaleString()}`, 'Volume']}
                />
                <Area 
                  type="monotone" 
                  dataKey="volume" 
                  stroke="#1e3a5f" 
                  strokeWidth={3}
                  fill="url(#volumeGradient)" 
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="card" style={{ padding: 24 }}>
          <h3 style={{ fontSize: 18, fontWeight: 600, color: '#1e293b', marginBottom: 24 }}>
            Quick Stats
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {[
              { label: 'Active Agents', value: '2,341', sub: '82% of total' },
              { label: 'Pending Review', value: '47', sub: 'Requires attention' },
              { label: 'Successful Txns', value: '98.2%', sub: 'Last 24 hours' },
            ].map((item, index) => (
              <div key={index} style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '16px 0',
                borderBottom: index < 2 ? '1px solid #e2e8f0' : 'none'
              }}>
                <div>
                  <p style={{ fontSize: 13, color: '#64748b' }}>{item.label}</p>
                  <h4 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }}>
                    {item.value}
                  </h4>
                </div>
                <span style={{
                  fontSize: 12,
                  color: '#0d9488',
                  background: 'rgba(13, 148, 136, 0.1)',
                  padding: '4px 8px',
                  borderRadius: 4
                }}>
                  {item.sub}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="card" style={{ padding: 24 }}>
        <div style={{ 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'space-between',
          marginBottom: 24
        }}>
          <h3 style={{ fontSize: 18, fontWeight: 600, color: '#1e293b' }}>
            Recent Transactions
          </h3>
          <button className="btn btn-outline btn-sm">
            View All
          </button>
        </div>
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Transaction ID</th>
                <th>Agent</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Time</th>
              </tr>
            </thead>
            <tbody>
              {[
                { id: 'TXN-001', agent: 'Ahmad Razak', type: 'Deposit', amount: 'RM 5,000', status: 'Success', time: '2 min ago' },
                { id: 'TXN-002', agent: 'Siti Aminah', type: 'Withdrawal', amount: 'RM 2,500', status: 'Success', time: '5 min ago' },
                { id: 'TXN-003', agent: 'Mohd Faisal', type: 'Bill Pay', amount: 'RM 150', status: 'Pending', time: '12 min ago' },
                { id: 'TXN-004', agent: 'Lee Ming', type: 'Topup', amount: 'RM 100', status: 'Success', time: '18 min ago' },
                { id: 'TXN-005', agent: 'Nurul Huda', type: 'Deposit', amount: 'RM 8,000', status: 'Failed', time: '25 min ago' },
              ].map((txn, index) => (
                <tr key={index}>
                  <td style={{ fontFamily: 'monospace', color: '#1e3a5f' }}>
                    {txn.id}
                  </td>
                  <td>{txn.agent}</td>
                  <td>
                    <span className="badge badge-info">{txn.type}</span>
                  </td>
                  <td style={{ fontWeight: 500 }}>{txn.amount}</td>
                  <td>
                    <span className={`badge ${
                      txn.status === 'Success' ? 'badge-success' : 
                      txn.status === 'Pending' ? 'badge-warning' : 'badge-error'
                    }`}>
                      {txn.status}
                    </span>
                  </td>
                  <td style={{ color: '#64748b' }}>{txn.time}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}