import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import api from '../api/client'

export function Settlement() {
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])
  
  const { data, isLoading, error } = useQuery({
    queryKey: ['settlement', date],
    queryFn: () => api.getSettlement({ date }),
  })

  const handleExport = async () => {
    const blob = await api.exportSettlement({ date })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `settlement-${date}.csv`
    a.click()
  }

  if (isLoading) return <div>Loading...</div>
  if (error) return <div>Error loading settlement</div>

  return (
    <div>
      <h1>Settlement Report</h1>
      <div style={{ marginTop: 20 }}>
        <label>Date: </label>
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        <button onClick={handleExport} style={{ marginLeft: 10 }}>Export CSV</button>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 20, marginTop: 20 }}>
        <div style={{ background: '#f0f0f0', padding: 20, borderRadius: 8 }}>
          <h3>Total Deposits</h3>
          <p style={{ fontSize: 20, fontWeight: 'bold' }}>RM {data?.totalDeposits ?? '0.00'}</p>
        </div>
        <div style={{ background: '#f0f0f0', padding: 20, borderRadius: 8 }}>
          <h3>Total Withdrawals</h3>
          <p style={{ fontSize: 20, fontWeight: 'bold' }}>RM {data?.totalWithdrawals ?? '0.00'}</p>
        </div>
        <div style={{ background: '#f0f0f0', padding: 20, borderRadius: 8 }}>
          <h3>Total Commissions</h3>
          <p style={{ fontSize: 20, fontWeight: 'bold' }}>RM {data?.totalCommissions ?? '0.00'}</p>
        </div>
        <div style={{ background: '#e8f5e9', padding: 20, borderRadius: 8 }}>
          <h3>Net Settlement</h3>
          <p style={{ fontSize: 20, fontWeight: 'bold' }}>RM {data?.netSettlement ?? '0.00'}</p>
        </div>
      </div>
    </div>
  )
}
