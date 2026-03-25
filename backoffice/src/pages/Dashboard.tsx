import { useQuery } from '@tanstack/react-query'
import api from '../api/client'

export function Dashboard() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard'],
    queryFn: api.getDashboard,
  })

  if (isLoading) return <div>Loading...</div>
  if (error) return <div>Error loading dashboard</div>

  return (
    <div>
      <h1>Dashboard</h1>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20, marginTop: 20 }}>
        <div style={{ background: '#f0f0f0', padding: 20, borderRadius: 8 }}>
          <h3>Today's Transactions</h3>
          <p style={{ fontSize: 24, fontWeight: 'bold' }}>{data?.totalTransactions ?? 0}</p>
        </div>
        <div style={{ background: '#f0f0f0', padding: 20, borderRadius: 8 }}>
          <h3>Total Volume</h3>
          <p style={{ fontSize: 24, fontWeight: 'bold' }}>RM {data?.totalVolume ?? '0.00'}</p>
        </div>
        <div style={{ background: '#f0f0f0', padding: 20, borderRadius: 8 }}>
          <h3>Active Agents</h3>
          <p style={{ fontSize: 24, fontWeight: 'bold' }}>{data?.activeAgents ?? 0}</p>
        </div>
      </div>
    </div>
  )
}
