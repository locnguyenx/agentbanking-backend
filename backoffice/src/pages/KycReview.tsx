import { useQuery } from '@tanstack/react-query'
import api from '../api/client'

export function KycReview() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['kyc-review'],
    queryFn: () => api.getKycReviewQueue(),
  })

  if (isLoading) return <div>Loading...</div>
  if (error) return <div>Error loading KYC review queue</div>

  return (
    <div>
      <h1>KYC Review Queue</h1>
      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 20 }}>
        <thead>
          <tr style={{ background: '#f0f0f0' }}>
            <th style={{ padding: 10, textAlign: 'left' }}>Verification ID</th>
            <th style={{ padding: 10, textAlign: 'left' }}>MyKad</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Full Name</th>
            <th style={{ padding: 10, textAlign: 'left' }}>AML Status</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Biometric</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Reason</th>
            <th style={{ padding: 10, textAlign: 'left' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {data?.content?.map((item: any) => (
            <tr key={item.verificationId} style={{ borderBottom: '1px solid #ddd' }}>
              <td style={{ padding: 10 }}>{item.verificationId}</td>
              <td style={{ padding: 10 }}>{item.mykadMasked}</td>
              <td style={{ padding: 10 }}>{item.fullName}</td>
              <td style={{ padding: 10 }}>{item.amlStatus}</td>
              <td style={{ padding: 10 }}>{item.biometricMatch}</td>
              <td style={{ padding: 10 }}>{item.rejectionReason}</td>
              <td style={{ padding: 10 }}>
                <button style={{ marginRight: 5 }}>Approve</button>
                <button>Reject</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
