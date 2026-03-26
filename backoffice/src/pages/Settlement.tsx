import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { 
  Download, 
  Printer,
  DollarSign,
  ArrowUpRight,
  ArrowDownRight,
  Calculator
} from 'lucide-react'
import api from '../api/client'

const mockSettlement = {
  totalDeposits: 1250000.00,
  totalWithdrawals: 850000.00,
  totalCommissions: 12500.00,
  netSettlement: 387500.00,
  transactions: [
    { id: 'SET-001', agent: 'Ahmad Razak', deposits: 450000, withdrawals: 320000, commission: 4500, net: 125500 },
    { id: 'SET-002', agent: 'Siti Aminah', deposits: 380000, withdrawals: 280000, commission: 3800, net: 96200 },
    { id: 'SET-003', agent: 'Lee Ming Wei', deposits: 420000, withdrawals: 250000, commission: 4200, net: 165800 },
  ]
}

export function Settlement() {
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])
  
  useQuery({
    queryKey: ['settlement', date],
    queryFn: () => api.getSettlement({ date }),
  })

  const handleExport = async () => {
    try {
      const blob = await api.exportSettlement({ date })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `settlement-${date}.csv`
      a.click()
    } catch (e) {
      console.log('Export failed:', e)
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }}>
            Settlement Report
          </h2>
          <p style={{ fontSize: 14, color: '#64748b' }}>
            Daily settlement summary and reports
          </p>
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <button className="btn btn-outline">
            <Printer size={18} />
            Print
          </button>
          <button className="btn btn-primary" onClick={handleExport}>
            <Download size={18} />
            Export CSV
          </button>
        </div>
      </div>

      {/* Date Filter */}
      <div className="card" style={{ padding: 16, display: 'flex', alignItems: 'center', gap: 16 }}>
        <div className="input-group">
          <label className="input-label">Settlement Date</label>
          <input 
            type="date" 
            className="input"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            style={{ width: 180 }}
          />
        </div>
        <button className="btn btn-primary">
          Generate Report
        </button>
      </div>

      {/* Summary Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        {[
          { label: 'Total Deposits', value: 'RM 1,250,000', icon: ArrowUpRight, color: '#10b981' },
          { label: 'Total Withdrawals', value: 'RM 850,000', icon: ArrowDownRight, color: '#ef4444' },
          { label: 'Total Commissions', value: 'RM 12,500', icon: Calculator, color: '#f59e0b' },
          { label: 'Net Settlement', value: 'RM 387,500', icon: DollarSign, color: '#1e3a5f' },
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
                <h3 style={{ fontSize: 22, fontWeight: 700, color: '#1e293b' }}>{stat.value}</h3>
              </div>
            </div>
          )
        })}
      </div>

      {/* Settlement Table */}
      <div className="card">
        <div style={{ padding: '16px 24px', borderBottom: '1px solid #e2e8f0' }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, color: '#1e293b' }}>
            Agent Settlement Details
          </h3>
        </div>
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Agent</th>
                <th>Deposits</th>
                <th>Withdrawals</th>
                <th>Commission</th>
                <th>Net Settlement</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {mockSettlement.transactions.map((row, index) => (
                <tr key={index}>
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
                        {row.agent.split(' ').map(n => n[0]).join('')}
                      </div>
                      <div>
                        <div style={{ fontWeight: 500 }}>{row.agent}</div>
                        <div style={{ fontSize: 12, color: '#64748b' }}>{row.id}</div>
                      </div>
                    </div>
                  </td>
                  <td style={{ color: '#10b981', fontWeight: 500 }}>
                    +RM {row.deposits.toLocaleString()}
                  </td>
                  <td style={{ color: '#ef4444', fontWeight: 500 }}>
                    -RM {row.withdrawals.toLocaleString()}
                  </td>
                  <td style={{ color: '#f59e0b', fontWeight: 500 }}>
                    RM {row.commission.toLocaleString()}
                  </td>
                  <td style={{ fontWeight: 700 }}>
                    RM {row.net.toLocaleString()}
                  </td>
                  <td>
                    <span className="badge badge-success">Settled</span>
                  </td>
                  <td>
                    <button className="btn btn-outline btn-sm">
                      <Download size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr style={{ background: '#f8fafc', fontWeight: 600 }}>
                <td>Total</td>
                <td style={{ color: '#10b981' }}>+RM 1,250,000</td>
                <td style={{ color: '#ef4444' }}>-RM 850,000</td>
                <td style={{ color: '#f59e0b' }}>RM 12,500</td>
                <td>RM 387,500</td>
                <td></td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  )
}