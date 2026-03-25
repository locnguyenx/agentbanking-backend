import { Outlet, Link } from 'react-router-dom'

export function Layout() {
  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <nav style={{ width: 220, background: '#1a1a2e', padding: '20px 0' }}>
        <h2 style={{ color: '#fff', padding: '0 20px', marginBottom: 30 }}>Agent Banking</h2>
        <ul style={{ listStyle: 'none', padding: 0 }}>
          <li><Link to="/" style={{ color: '#e0e0e0', display: 'block', padding: '10px 20px' }}>Dashboard</Link></li>
          <li><Link to="/agents" style={{ color: '#e0e0e0', display: 'block', padding: '10px 20px' }}>Agents</Link></li>
          <li><Link to="/transactions" style={{ color: '#e0e0e0', display: 'block', padding: '10px 20px' }}>Transactions</Link></li>
          <li><Link to="/settlement" style={{ color: '#e0e0e0', display: 'block', padding: '10px 20px' }}>Settlement</Link></li>
          <li><Link to="/kyc-review" style={{ color: '#e0e0e0', display: 'block', padding: '10px 20px' }}>KYC Review</Link></li>
        </ul>
      </nav>
      <main style={{ flex: 1, padding: 30 }}>
        <Outlet />
      </main>
    </div>
  )
}
