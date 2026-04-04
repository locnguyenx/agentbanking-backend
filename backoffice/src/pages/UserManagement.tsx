import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { X, Edit2, Search, Download, ChevronLeft, ChevronRight, MoreVertical, Lock, Unlock, Trash2, Key, UserCheck } from 'lucide-react'
import api, { User } from '../api/client'

export function UserManagement() {
  const [searchParams] = useSearchParams()
  const initialUserId = searchParams.get('userId')
  const queryClient = useQueryClient()
  const [currentPage, setCurrentPage] = useState(1)
  const [showAddModal, setShowAddModal] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('All')
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null)
  const [viewUser, setViewUser] = useState<User | null>(null)
  const [editUser, setEditUser] = useState<User | null>(null)
  const [showResetPassword, setShowResetPassword] = useState<User | null>(null)
  const [newPassword, setNewPassword] = useState('')
  const itemsPerPage = 6

  const { data: users = [], isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      const response = await api.getUsers()
      return response as User[]
    }
  })

  useEffect(() => {
    if (initialUserId && users.length > 0) {
      const user = users.find(u => u.userId === initialUserId)
      if (user) {
        setViewUser(user)
      }
    }
  }, [initialUserId, users])

  const createUserMutation = useMutation({
    mutationFn: (data: { username: string; email: string; fullName: string; password: string }) => 
      api.createUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setShowAddModal(false)
      alert('User created successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to create user: ${error.response?.data?.error?.message || error.message}`)
    }
  })

  const updateUserMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) => 
      api.updateUser(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setEditUser(null)
      alert('User updated successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to update user: ${error.message}`)
    }
  })

  const deleteUserMutation = useMutation({
    mutationFn: (id: string) => api.deleteUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      alert('User deleted successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to delete user: ${error.message}`)
    }
  })

  const lockUserMutation = useMutation({
    mutationFn: (id: string) => api.lockUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      alert('User locked successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to lock user: ${error.message}`)
    }
  })

  const unlockUserMutation = useMutation({
    mutationFn: (id: string) => api.unlockUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      alert('User unlocked successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to unlock user: ${error.message}`)
    }
  })

  const resetPasswordMutation = useMutation({
    mutationFn: ({ id, newPassword }: { id: string; newPassword: string }) => 
      api.resetUserPassword(id, newPassword),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setShowResetPassword(null)
      setNewPassword('')
      alert('Password reset successfully!')
    },
    onError: (error: any) => {
      alert(`Failed to reset password: ${error.message}`)
    }
  })

  const filteredUsers = users.filter(user => {
    const matchesSearch = user.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          user.email.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesStatus = statusFilter === 'All' || user.status === statusFilter
    return matchesSearch && matchesStatus
  })

  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / itemsPerPage))
  const safeCurrentPage = currentPage > totalPages ? 1 : currentPage
  
  const paginatedUsers = filteredUsers.slice(
    (safeCurrentPage - 1) * itemsPerPage,
    safeCurrentPage * itemsPerPage
  )

  const handlePageChange = (page: number) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page)
    }
  }

  const handleAddUser = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const form = e.currentTarget
    const formData = new FormData(form)
    createUserMutation.mutate({
      username: formData.get('username') as string,
      email: formData.get('email') as string,
      fullName: formData.get('fullName') as string,
      password: formData.get('password') as string
    })
    setCurrentPage(1)
  }

  const handleSaveEdit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const form = e.currentTarget
    const formData = new FormData(form)
    if (editUser) {
      updateUserMutation.mutate({
        id: editUser.userId,
        data: {
          username: formData.get('username') as string,
          email: formData.get('email') as string,
          fullName: formData.get('fullName') as string
        }
      })
    }
  }

  const handleResetPassword = () => {
    if (!newPassword || newPassword.length < 8) {
      alert('Password must be at least 8 characters')
      return
    }
    if (showResetPassword) {
      resetPasswordMutation.mutate({ id: showResetPassword.userId, newPassword })
    }
  }

  const handleUserAction = (userId: string) => {
    setSelectedUserId(selectedUserId === userId ? null : userId)
  }

  if (isLoading) {
    return <div style={{ padding: 32, textAlign: 'center' }}>Loading users...</div>
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }}>
            User Management
          </h2>
          <p style={{ fontSize: 14, color: '#64748b' }}>
            Manage system users and their access
          </p>
        </div>
        <button 
          className="btn btn-primary"
          onClick={() => setShowAddModal(true)}
          data-testid="add-user-button"
        >
          <UserCheck size={18} />
          Add User
        </button>
      </div>

      {/* Filters */}
      <div className="card" style={{ padding: 16 }}>
        <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
          <div style={{ position: 'relative', flex: 1 }}>
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
              placeholder="Search users by name, username, or email..."
              className="input"
              style={{ paddingLeft: 42 }}
              value={searchTerm}
              onChange={(e) => { setSearchTerm(e.target.value); setCurrentPage(1); }}
              data-testid="search-input"
            />
          </div>
          <select className="input" style={{ width: 150 }} onChange={(e) => { setStatusFilter(e.target.value); setCurrentPage(1); }} value={statusFilter} data-testid="status-filter">
            <option value="All">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="LOCKED">Locked</option>
            <option value="DELETED">Deleted</option>
            <option value="INACTIVE">Inactive</option>
          </select>
          <button className="btn btn-outline">
            <Download size={18} />
            Export
          </button>
        </div>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        {(() => {
          const total = users.length
          const active = users.filter(u => u.status === 'ACTIVE').length
          const locked = users.filter(u => u.status === 'LOCKED').length
          const inactive = users.filter(u => u.status === 'INACTIVE').length
          return [
            { label: 'Total Users', value: total.toString(), color: '#1e3a5f' },
            { label: 'Active', value: active.toString(), color: '#10b981' },
            { label: 'Locked', value: locked.toString(), color: '#f59e0b' },
            { label: 'Inactive', value: inactive.toString(), color: '#3b82f6' },
          ].map((stat, index) => (
            <div key={index} className="card" style={{ padding: 20, textAlign: 'center' }}>
              <p style={{ fontSize: 13, color: '#64748b', marginBottom: 4 }}>{stat.label}</p>
              <h3 style={{ fontSize: 24, fontWeight: 700, color: stat.color }}>{stat.value}</h3>
            </div>
          ))
        })()}
      </div>

      {/* Table */}
      <div className="card">
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Full Name</th>
                <th>Email</th>
                <th>Status</th>
                <th>User Type</th>
                <th>Agent ID</th>
                <th>Last Login</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {paginatedUsers.map((user) => (
                <tr key={user.userId}>
                  <td style={{ fontFamily: 'monospace', color: '#1e3a5f', fontWeight: 500 }}>
                    {user.username}
                  </td>
                  <td>{user.fullName}</td>
                  <td style={{ color: '#64748b' }}>{user.email}</td>
                  <td>
                    <span className={`badge ${
                      user.status === 'ACTIVE' ? 'badge-success' : 
                      user.status === 'LOCKED' ? 'badge-warning' :
                      user.status === 'INACTIVE' ? 'badge-info' :
                      user.status === 'DELETED' ? 'badge-error' : ''
                    }`}>
                      {user.status}
                    </span>
                  </td>
                  <td>
                    <span style={{
                      padding: '4px 10px',
                      borderRadius: 4,
                      fontSize: 12,
                      fontWeight: 600,
                      background: user.userType === 'INTERNAL' ? '#dbeafe' : '#fef3c7',
                      color: user.userType === 'INTERNAL' ? '#1e40af' : '#92400e'
                    }}>
                      {user.userType || 'INTERNAL'}
                    </span>
                  </td>
                  <td style={{ fontSize: 13, fontFamily: 'monospace', color: '#64748b' }}>
                    {user.userType === 'EXTERNAL' && user.agentId 
                      ? user.agentId.slice(0, 8) + '...' 
                      : '—'}
                  </td>
                  <td style={{ color: '#64748b' }}>
                    {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString() : 'Never'}
                  </td>
                  <td>
                    <div style={{ position: 'relative' }}>
                      <button 
                        style={{ 
                          padding: 8, 
                          background: 'transparent', 
                          border: 'none', 
                          cursor: 'pointer',
                          borderRadius: 6
                        }}
                        onClick={() => handleUserAction(user.userId)}
                        data-testid={`action-${user.userId}-button`}
                      >
                        <MoreVertical size={16} color="#94a3b8" />
                      </button>
                      {selectedUserId === user.userId && (
                        <div style={{
                          position: 'absolute',
                          right: 0,
                          top: '100%',
                          background: 'white',
                          border: '1px solid #e2e8f0',
                          borderRadius: 8,
                          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                          zIndex: 100,
                          minWidth: 160,
                          overflow: 'hidden'
                        }}>
                          <button 
                            style={{
                              width: '100%',
                              padding: '10px 16px',
                              textAlign: 'left',
                              background: 'none',
                              border: 'none',
                              cursor: 'pointer',
                              fontSize: 14,
                              color: '#1e293b',
                              display: 'flex',
                              alignItems: 'center',
                              gap: 8
                            }}
                            onClick={() => { setViewUser(user); setSelectedUserId(null); }}
                          >
                            <Search size={14} /> View Details
                          </button>
                          <button 
                            style={{
                              width: '100%',
                              padding: '10px 16px',
                              textAlign: 'left',
                              background: 'none',
                              border: 'none',
                              cursor: 'pointer',
                              fontSize: 14,
                              color: '#1e293b',
                              display: 'flex',
                              alignItems: 'center',
                              gap: 8
                            }}
                            onClick={() => { setEditUser(user); setSelectedUserId(null); }}
                          >
                            <Edit2 size={14} /> Edit User
                          </button>
                          <button 
                            style={{
                              width: '100%',
                              padding: '10px 16px',
                              textAlign: 'left',
                              background: 'none',
                              border: 'none',
                              cursor: 'pointer',
                              fontSize: 14,
                              color: '#1e293b',
                              display: 'flex',
                              alignItems: 'center',
                              gap: 8
                            }}
                            onClick={() => { setShowResetPassword(user); setSelectedUserId(null); }}
                          >
                            <Key size={14} /> Reset Password
                          </button>
                          {user.status === 'ACTIVE' ? (
                            <button 
                              style={{
                                width: '100%',
                                padding: '10px 16px',
                                textAlign: 'left',
                                background: 'none',
                                border: 'none',
                                cursor: 'pointer',
                                fontSize: 14,
                                color: '#f59e0b',
                                display: 'flex',
                                alignItems: 'center',
                                gap: 8
                              }}
                              onClick={() => { lockUserMutation.mutate(user.userId); setSelectedUserId(null); }}
                            >
                              <Lock size={14} /> Lock User
                            </button>
                          ) : (
                            <button 
                              style={{
                                width: '100%',
                                padding: '10px 16px',
                                textAlign: 'left',
                                background: 'none',
                                border: 'none',
                                cursor: 'pointer',
                                fontSize: 14,
                                color: '#10b981',
                                display: 'flex',
                                alignItems: 'center',
                                gap: 8
                              }}
                              onClick={() => { unlockUserMutation.mutate(user.userId); setSelectedUserId(null); }}
                            >
                              <Unlock size={14} /> Unlock User
                            </button>
                          )}
                          <button 
                            style={{
                              width: '100%',
                              padding: '10px 16px',
                              textAlign: 'left',
                              background: 'none',
                              border: 'none',
                              cursor: 'pointer',
                              fontSize: 14,
                              color: '#ef4444',
                              display: 'flex',
                              alignItems: 'center',
                              gap: 8
                            }}
                            onClick={() => { 
                              if (confirm(`Delete user ${user.username}? This action cannot be undone.`)) {
                                deleteUserMutation.mutate(user.userId)
                              }
                              setSelectedUserId(null); 
                            }}
                          >
                            <Trash2 size={14} /> Delete User
                          </button>
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '16px 24px',
          borderTop: '1px solid #e2e8f0'
        }}>
          <p style={{ fontSize: 13, color: '#64748b' }}>
            Showing {((safeCurrentPage - 1) * itemsPerPage) + 1} to {Math.min(safeCurrentPage * itemsPerPage, filteredUsers.length)} of {filteredUsers.length} users
          </p>
          <div style={{ display: 'flex', gap: 8 }}>
            <button 
              className="btn btn-outline btn-sm" 
              onClick={() => handlePageChange(safeCurrentPage - 1)}
              disabled={safeCurrentPage === 1}
              data-testid="prev-page-button"
            >
              <ChevronLeft size={16} />
              Previous
            </button>
            {totalPages > 0 && Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
              <button 
                key={page}
                className={`btn btn-sm ${page === safeCurrentPage ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => handlePageChange(page)}
                data-testid={`page-${page}-button`}
              >
                {page}
              </button>
            ))}
            <button 
              className="btn btn-outline btn-sm"
              onClick={() => handlePageChange(safeCurrentPage + 1)}
              disabled={safeCurrentPage >= totalPages}
              data-testid="next-page-button"
            >
              Next
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>

      {/* Add User Modal */}
      {showAddModal && (
        <div className="modal-overlay" data-testid="add-user-modal">
          <div className="modal">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
              <h3>Add New User</h3>
              <button 
                onClick={() => setShowAddModal(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer' }}
                data-testid="close-modal-button"
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleAddUser}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Username</label>
                  <input type="text" name="username" className="input" placeholder="Enter username" required />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Full Name</label>
                  <input type="text" name="fullName" className="input" placeholder="Enter full name" required />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Email</label>
                  <input type="email" name="email" className="input" placeholder="user@bank.com" required />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Temporary Password</label>
                  <input type="password" name="password" className="input" placeholder="Minimum 8 characters" required minLength={8} />
                </div>
                <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
                  <button type="button" className="btn btn-outline" onClick={() => setShowAddModal(false)} style={{ flex: 1 }}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                    Create User
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* View User Modal */}
      {viewUser && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.6)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          backdropFilter: 'blur(4px)'
        }} onClick={() => setViewUser(null)}>
          <div style={{ 
            width: 540, 
            maxHeight: '85vh', 
            overflow: 'auto',
            background: 'white',
            borderRadius: 16,
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
          }} onClick={e => e.stopPropagation()}>
            <div style={{ 
              padding: 24, 
              borderBottom: '1px solid #e2e8f0',
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center' 
            }}>
              <h3 style={{ fontSize: 20, fontWeight: 600, margin: 0 }}>User Details</h3>
              <button onClick={() => setViewUser(null)} style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}>
                <X size={20} color="#64748b" />
              </button>
            </div>
            <div style={{ padding: 24, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Username</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>{viewUser.username}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Status</label>
                <p style={{ margin: '8px 0 0 0' }}><span className={`badge ${viewUser.status === 'ACTIVE' ? 'badge-success' : viewUser.status === 'LOCKED' ? 'badge-warning' : viewUser.status === 'INACTIVE' ? 'badge-info' : 'badge-error'}`}>{viewUser.status}</span></p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Email</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>{viewUser.email}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>User Type</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>{viewUser.userType || 'INTERNAL'}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Last Login</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>
                  {viewUser.lastLoginAt ? new Date(viewUser.lastLoginAt).toLocaleString() : 'Never'}
                </p>
              </div>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Created At</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>
                  {viewUser.createdAt ? new Date(viewUser.createdAt).toLocaleString() : 'N/A'}
                </p>
              </div>
              <div style={{ gridColumn: '1 / -1', background: '#f8fafc', padding: 16, borderRadius: 12 }}>
                <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>Full Name</label>
                <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>{viewUser.fullName}</p>
              </div>
            </div>
            <div style={{ padding: '0 24px 24px', display: 'flex', gap: 12 }}>
              <button className="btn btn-primary" onClick={() => { setEditUser(viewUser); setViewUser(null); }} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                <Edit2 size={16} /> Edit User
              </button>
              <button className="btn btn-outline" onClick={() => setViewUser(null)} style={{ flex: 1 }}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit User Modal */}
      {editUser && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.6)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          backdropFilter: 'blur(4px)'
        }} onClick={() => setEditUser(null)}>
          <div style={{ 
            width: 480, 
            background: 'white',
            borderRadius: 16,
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
          }} onClick={e => e.stopPropagation()}>
            <div style={{ 
              padding: 24, 
              borderBottom: '1px solid #e2e8f0',
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center' 
            }}>
              <h3 style={{ fontSize: 20, fontWeight: 600, margin: 0 }}>Edit User</h3>
              <button onClick={() => setEditUser(null)} style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}>
                <X size={20} color="#64748b" />
              </button>
            </div>
            <form onSubmit={handleSaveEdit} style={{ padding: 24 }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
                <div>
                  <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, fontSize: 14 }}>Username</label>
                  <input 
                    name="username"
                    type="text" 
                    className="input" 
                    defaultValue={editUser.username} 
                    required 
                    style={{ padding: '12px 16px', fontSize: 15 }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, fontSize: 14 }}>Full Name</label>
                  <input 
                    name="fullName"
                    type="text" 
                    className="input" 
                    defaultValue={editUser.fullName} 
                    required 
                    style={{ padding: '12px 16px', fontSize: 15 }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, fontSize: 14 }}>Email</label>
                  <input 
                    name="email"
                    type="email" 
                    className="input" 
                    defaultValue={editUser.email} 
                    required 
                    style={{ padding: '12px 16px', fontSize: 15 }}
                  />
                </div>
                <div style={{ 
                  background: '#f8fafc', 
                  padding: 16, 
                  borderRadius: 12,
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: 12
                }}>
                  <div>
                    <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>User ID</label>
                    <p style={{ fontWeight: 500, margin: '4px 0 0 0', fontSize: 12, fontFamily: 'monospace' }}>{editUser.userId}</p>
                  </div>
                  <div>
                    <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>Status</label>
                    <p style={{ margin: '4px 0 0 0' }}><span className={`badge ${editUser.status === 'ACTIVE' ? 'badge-success' : editUser.status === 'LOCKED' ? 'badge-warning' : editUser.status === 'INACTIVE' ? 'badge-info' : 'badge-error'}`}>{editUser.status}</span></p>
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
                  <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '12px 24px', fontSize: 15 }}>
                    Save Changes
                  </button>
                  <button type="button" className="btn btn-outline" onClick={() => setEditUser(null)} style={{ flex: 1, padding: '12px 24px', fontSize: 15 }}>
                    Cancel
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reset Password Modal */}
      {showResetPassword && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.6)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          backdropFilter: 'blur(4px)'
        }} onClick={() => setShowResetPassword(null)}>
          <div style={{ 
            width: 420, 
            background: 'white',
            borderRadius: 16,
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
          }} onClick={e => e.stopPropagation()}>
            <div style={{ 
              padding: 24, 
              borderBottom: '1px solid #e2e8f0',
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center' 
            }}>
              <h3 style={{ fontSize: 20, fontWeight: 600, margin: 0 }}>Reset Password</h3>
              <button onClick={() => setShowResetPassword(null)} style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}>
                <X size={20} color="#64748b" />
              </button>
            </div>
            <div style={{ padding: 24 }}>
              <p style={{ marginBottom: 16, color: '#64748b' }}>
                Enter a new temporary password for user <strong>{showResetPassword.username}</strong>.
              </p>
              <div>
                <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, fontSize: 14 }}>New Password</label>
                <input 
                  type="password"
                  className="input" 
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Minimum 8 characters"
                  style={{ padding: '12px 16px', fontSize: 15 }}
                />
              </div>
              <div style={{ display: 'flex', gap: 12, marginTop: 24 }}>
                <button className="btn btn-primary" onClick={handleResetPassword} style={{ flex: 1, padding: '12px 24px', fontSize: 15 }}>
                  Reset Password
                </button>
                <button className="btn btn-outline" onClick={() => { setShowResetPassword(null); setNewPassword(''); }} style={{ flex: 1, padding: '12px 24px', fontSize: 15 }}>
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
