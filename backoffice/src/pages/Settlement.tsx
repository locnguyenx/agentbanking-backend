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

interface SettlementData {
  totalCredits: number
  totalDebits: number
  netAmount: number
  transactions: Array<{
    transactionType: string
    agentId: string
    amount: number
    transactionId: string
    status: string
  }>
}

export function Settlement() {
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])
  
  const { data: settlement } = useQuery({
    queryKey: ['settlement', date],
    queryFn: async () => {
      const response = await api.getSettlement({ date })
      return response as SettlementData
    }
  })

  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setDate(e.target.value)
  }

  const handleDownloadSettlement = (agentId: string) => {
    console.log('Download settlement for agent:', agentId)
  }

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

  const formatAmount = (amount: number) => {
    return 'RM ' + amount.toLocaleString('en-MY', { minimumFractionDigits: 2 })
  }

  const stats = settlement ? [
    { label: 'Total Deposits', value: formatAmount(settlement.totalCredits), icon: ArrowUpRight, color: '#10b981' },
    { label: 'Total Withdrawals', value: formatAmount(settlement.totalDebits), icon: ArrowDownRight, color: '#ef4444' },
    { label: 'Total Commissions', value: 'RM 0', icon: Calculator, color: '#f59e0b' },
    { label: 'Net Settlement', value: formatAmount(settlement.netAmount), icon: DollarSign, color: '#1e3a5f' },
  ] : [
    { label: 'Total Deposits', value: 'RM 0', icon: ArrowUpRight, color: '#10b981' },
    { label: 'Total Withdrawals', value: 'RM 0', icon: ArrowDownRight, color: '#ef4444' },
    { label: 'Total Commissions', value: 'RM 0', icon: Calculator, color: '#f59e0b' },
    { label: 'Net Settlement', value: 'RM 0', icon: DollarSign, color: '#1e3a5f' },
  ]

  const transactions = settlement?.transactions || []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }} data-testid="page-title">
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
          <button className="btn btn-primary" onClick={handleExport} data-testid="export-button">
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
            style={{ width: 180 }}
            value={date}
            onChange={handleDateChange}
            data-testid="date-picker"
          />
        </div>
        <button className="btn btn-primary">
          Generate Report
        </button>
      </div>

      {/* Summary Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        {stats.map((stat, index) => {
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
                <th>Transaction ID</th>
                <th>Type</th>
                <th>Agent ID</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((txn) => (
                <tr key={txn.transactionId}>
                  <td style={{ fontFamily: 'monospace', color: '#1e3a5f', fontWeight: 500, fontSize: 13 }}>
                    {txn.transactionId.substring(0, 8)}...
                  </td>
                  <td>
                    <span className="badge badge-info">{txn.transactionType}</span>
                  </td>
                  <td style={{ fontFamily: 'monospace', color: '#64748b', fontSize: 13 }}>
                    {txn.agentId.substring(0, 8)}...
                  </td>
                  <td style={{ fontWeight: 600 }}>
                    {formatAmount(txn.amount)}
                  </td>
                  <td>
                    <span className={`badge ${
                      txn.status === 'COMPLETED' ? 'badge-success' : 
                      txn.status === 'PENDING' ? 'badge-warning' : 'badge-error'
                    }`}>
                      {txn.status}
                    </span>
                  </td>
                  <td>
                    <button 
                      className="btn btn-outline btn-sm"
                      onClick={() => handleDownloadSettlement(txn.transactionId)}
                      data-testid={`download-${txn.transactionId}-button`}
                    >
                      <Download size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
