import { useQuery } from '@tanstack/react-query'
import api from '../api/client'

export function Transactions() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['transactions'],
    queryFn: () => api.getTransactions(),
  })

  if (isLoading) return <div>Loading...</div>
  if (error) return <div>Error loading transactions</div>

  return (
    <div>
      <h1>Transactions</h1>
      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 20 }}>
        <thead>
          <tr style={{ background: '#f0f0f0' }}>
            <th style={{ padding: 10, textAlign: 'left' }}>Transaction ID</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Type</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Amount</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Status</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Agent</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Created</th>
          </tr>
        </thead>
        <tbody>
          {data?.content?.map((tx: any) => (
            <tr key={tx.transactionId} style={{ borderBottom: '1px solid #ddd' }}>
              <td style={{ padding: 10 }}>{tx.transactionId}</td>
              <td style={{ padding: 10 }}>{tx.transactionType}</td>
              <td style={{ padding: 10 }}>RM {tx.amount}</td>
              <td style={{ padding: 10 }}>{tx.status}</td>
              <td style={{ padding: 10 }}>{tx.agentCode}</td>
              <td style={{ padding: 10 }}>{tx.createdAt}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
