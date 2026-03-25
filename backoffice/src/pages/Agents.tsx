import { useQuery } from '@tanstack/react-query'
import api from '../api/client'

export function Agents() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['agents'],
    queryFn: () => api.getAgents(),
  })

  if (isLoading) return <div>Loading...</div>
  if (error) return <div>Error loading agents</div>

  return (
    <div>
      <h1>Agents</h1>
      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 20 }}>
        <thead>
          <tr style={{ background: '#f0f0f0' }}>
            <th style={{ padding: 10, textAlign: 'left' }}>Agent Code</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Business Name</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Tier</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Status</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Phone</th>
          </tr>
        </thead>
        <tbody>
          {data?.content?.map((agent: any) => (
            <tr key={agent.agentId} style={{ borderBottom: '1px solid #ddd' }}>
              <td style={{ padding: 10 }}>{agent.agentCode}</td>
              <td style={{ padding: 10 }}>{agent.businessName}</td>
              <td style={{ padding: 10 }}>{agent.tier}</td>
              <td style={{ padding: 10 }}>{agent.status}</td>
              <td style={{ padding: 10 }}>{agent.phoneNumber}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
