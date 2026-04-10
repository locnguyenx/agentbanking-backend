import { Outlet, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { 
  LayoutDashboard, 
  Users,
  UserCog,
  ArrowLeftRight, 
  FileCheck, 
  FileText,
  Bell,
  Search,
  ChevronLeft,
  ChevronRight,
  Settings,
  AlertTriangle,
  GitBranch
} from 'lucide-react'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import api from '../api/client'

const navItems = [
  { path: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { path: '/agents', icon: Users, label: 'Agents' },
  { path: '/users', icon: UserCog, label: 'User Management' },
  { path: '/ledger-transactions', icon: ArrowLeftRight, label: 'Ledger Transactions' },
  { path: '/settlement', icon: FileText, label: 'Settlement' },
  { path: '/kyc-review', icon: FileCheck, label: 'KYC Review' },
  { path: '/orchestrator-workflows', icon: GitBranch, label: 'Orchestrator Workflows' },
  { path: '/transaction-resolution', icon: AlertTriangle, label: 'Transaction Resolution', id: 'transaction-resolution-nav' },
  { path: '/system-admin', icon: Settings, label: 'System Admin' },
]

function getUserInitials(fullName: string) {
  return fullName
    .split(' ')
    .map(n => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

function formatRole(userType: string | undefined) {
  if (!userType) return 'User'
  const roles: Record<string, string> = {
    INTERNAL: 'Bank Staff',
    EXTERNAL: 'Agent User',
  }
  return roles[userType] || userType
}

export function Layout() {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()

  const { data: profile } = useQuery({
    queryKey: ['myProfile'],
    queryFn: api.getMyProfile,
    staleTime: 5 * 60 * 1000,
    retry: false,
  })

  const displayName = profile?.fullName || 'User'
  const displayInitial = getUserInitials(displayName)
  const displayRole = formatRole(profile?.userType)

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      {/* Sidebar */}
      <aside 
        style={{
          width: collapsed ? 72 : 260,
          background: 'linear-gradient(180deg, #1e3a5f 0%, #142840 100%)',
          display: 'flex',
          flexDirection: 'column',
          transition: 'width 0.3s ease',
          position: 'fixed',
          height: '100vh',
          zIndex: 100,
        }}
      >
        {/* Logo */}
        <div style={{ 
          padding: collapsed ? '20px 16px' : '20px 24px',
          borderBottom: '1px solid rgba(255,255,255,0.1)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}>
          {!collapsed && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{
                width: 36,
                height: 36,
                background: 'linear-gradient(135deg, #14b8a6 0%, #0d9488 100%)',
                borderRadius: 10,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                color: 'white',
                fontSize: 16
              }}>
                AB
              </div>
              <div>
                <div style={{ color: 'white', fontWeight: 600, fontSize: 15 }}>
                  Agent Banking
                </div>
                <div style={{ color: 'rgba(255,255,255,0.5)', fontSize: 11 }}>
                  Admin Console
                </div>
              </div>
            </div>
          )}
          {collapsed && (
            <div style={{
              width: 36,
              height: 36,
              background: 'linear-gradient(135deg, #14b8a6 0%, #0d9488 100%)',
              borderRadius: 10,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 700,
              color: 'white',
              fontSize: 16,
              margin: '0 auto'
            }}>
              AB
            </div>
          )}
        </div>

        {/* Navigation */}
        <nav style={{ flex: 1, padding: '16px 12px', overflowY: 'auto' }}>
          <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 4 }}>
            {navItems.map(({ path, icon: Icon, label }) => (
              <li key={path}>
                <NavLink
                  to={path}
                  style={({ isActive }) => ({
                    display: 'flex',
                    alignItems: 'center',
                    gap: 14,
                    padding: collapsed ? '12px' : '12px 16px',
                    borderRadius: 10,
                    color: isActive ? 'white' : 'rgba(255,255,255,0.6)',
                    background: isActive ? 'rgba(255,255,255,0.1)' : 'transparent',
                    transition: 'all 0.2s ease',
                    justifyContent: collapsed ? 'center' : 'flex-start',
                    fontSize: 14,
                    fontWeight: isActive ? 500 : 400,
                  })}
                  onMouseEnter={(e) => {
                    if (!e.currentTarget.classList.contains('active')) {
                      e.currentTarget.style.background = 'rgba(255,255,255,0.05)'
                      e.currentTarget.style.color = 'white'
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!e.currentTarget.classList.contains('active')) {
                      e.currentTarget.style.background = 'transparent'
                      e.currentTarget.style.color = 'rgba(255,255,255,0.6)'
                    }
                  }}
                >
                  <Icon size={20} />
                  {!collapsed && <span>{label}</span>}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        {/* Collapse Button */}
        <div style={{ 
          padding: '16px 12px', 
          borderTop: '1px solid rgba(255,255,255,0.1)' 
        }}>
          <button
            onClick={() => setCollapsed(!collapsed)}
            style={{
              width: '100%',
              padding: '10px',
              background: 'rgba(255,255,255,0.1)',
              border: 'none',
              borderRadius: 8,
              color: 'rgba(255,255,255,0.7)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              cursor: 'pointer',
              transition: 'all 0.2s ease'
            }}
          >
            {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
            {!collapsed && <span style={{ fontSize: 13 }}>Collapse</span>}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main style={{ 
        flex: 1, 
        marginLeft: collapsed ? 72 : 260,
        transition: 'margin-left 0.3s ease',
        display: 'flex',
        flexDirection: 'column'
      }}>
        {/* Header */}
        <header style={{
          height: 72,
          background: 'white',
          borderBottom: '1px solid #e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 32px',
          position: 'sticky',
          top: 0,
          zIndex: 50
        }}>
          {/* Page Title */}
          <div>
            <h1 style={{ 
              fontSize: 24, 
              fontWeight: 600, 
              color: '#1e293b',
              margin: 0
            }}>
              {navItems.find(item => item.path === location.pathname)?.label || 'Dashboard'}
            </h1>
          </div>

          {/* Right Side */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
            {/* Search */}
            <div style={{ 
              position: 'relative',
              width: 280
            }}>
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
                placeholder="Search..."
                style={{
                  width: '100%',
                  padding: '10px 14px 10px 42px',
                  border: '1px solid #e2e8f0',
                  borderRadius: 10,
                  fontSize: 14,
                  outline: 'none',
                  background: '#f8fafc'
                }}
              />
            </div>

            {/* Notifications */}
            <button style={{
              position: 'relative',
              padding: 10,
              background: 'transparent',
              border: 'none',
              cursor: 'pointer'
            }}>
              <Bell size={22} color="#64748b" />
              <span style={{
                position: 'absolute',
                top: 6,
                right: 6,
                width: 8,
                height: 8,
                background: '#ef4444',
                borderRadius: '50%',
                border: '2px solid white'
              }} />
            </button>

            {/* User */}
            <div style={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: 12,
              padding: '8px 12px',
              borderRadius: 10,
              cursor: 'pointer',
              transition: 'background 0.2s ease'
            }}>
              <div style={{
                width: 40,
                height: 40,
                borderRadius: 10,
                background: 'linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white',
                fontWeight: 600,
                fontSize: 14
              }}>
                {displayInitial}
              </div>
              <div>
                <div style={{ fontSize: 14, fontWeight: 500, color: '#1e293b' }}>
                  {displayName}
                </div>
                <div style={{ fontSize: 12, color: '#64748b' }}>
                  {displayRole}
                </div>
              </div>
              <button 
                onClick={() => navigate('/profile')}
                style={{
                  marginLeft: 8,
                  padding: '6px 12px',
                  background: '#f1f5f9',
                  border: 'none',
                  borderRadius: 6,
                  color: '#1e293b',
                  fontSize: 12,
                  cursor: 'pointer'
                }}
              >
                Profile
              </button>
              <button 
                onClick={() => {
                  localStorage.removeItem('backoffice_token')
                  window.location.href = '/login'
                }}
                style={{
                  marginLeft: 8,
                  padding: '6px 12px',
                  background: '#fee2e2',
                  border: 'none',
                  borderRadius: 6,
                  color: '#dc2626',
                  fontSize: 12,
                  cursor: 'pointer'
                }}
              >
                Logout
              </button>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <div style={{ flex: 1, padding: 32, background: '#f8fafc' }}>
          <Outlet />
        </div>
      </main>
    </div>
  )
}