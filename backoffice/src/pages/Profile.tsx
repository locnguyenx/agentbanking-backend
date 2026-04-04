import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { User, Lock, Eye, EyeOff } from 'lucide-react'
import toast from 'react-hot-toast'
import api, { User as UserType } from '../api/client'

export function Profile() {
  const queryClient = useQueryClient()
  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const { data: profile, isLoading } = useQuery<UserType>({
    queryKey: ['myProfile'],
    queryFn: api.getMyProfile,
  })

  const changePasswordMutation = useMutation({
    mutationFn: (data: { currentPassword: string; newPassword: string }) =>
      api.changeMyPassword(data),
    onSuccess: () => {
      toast.success('Password changed successfully')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      queryClient.invalidateQueries({ queryKey: ['myProfile'] })
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.error?.message || 'Failed to change password')
    },
  })

  const passwordsMatch = newPassword === confirmPassword && newPassword.length > 0
  const isFormValid =
    currentPassword.length > 0 &&
    newPassword.length >= 8 &&
    passwordsMatch

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!isFormValid) return
    changePasswordMutation.mutate({ currentPassword, newPassword })
  }

  if (isLoading) {
    return (
      <div style={{ padding: 32, textAlign: 'center' }}>Loading profile...</div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <div>
        <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b' }}>
          My Profile
        </h2>
        <p style={{ fontSize: 14, color: '#64748b' }}>
          View your profile information and change your password
        </p>
      </div>

      {/* Profile Information Card */}
      <div className="card" style={{ padding: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
          <div style={{
            width: 48,
            height: 48,
            borderRadius: 12,
            background: 'linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
          }}>
            <User size={24} />
          </div>
          <div>
            <h3 style={{ fontSize: 18, fontWeight: 600, margin: 0, color: '#1e293b' }}>
              Profile Information
            </h3>
            <p style={{ fontSize: 13, color: '#64748b', margin: 0 }}>
              Your account details
            </p>
          </div>
        </div>

        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: 20,
        }}>
          <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
            <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>
              Full Name
            </label>
            <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>
              {profile?.fullName || 'N/A'}
            </p>
          </div>
          <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
            <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>
              Email
            </label>
            <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>
              {profile?.email || 'N/A'}
            </p>
          </div>
          <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
            <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>
              Username
            </label>
            <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15, fontFamily: 'monospace' }}>
              {profile?.username || 'N/A'}
            </p>
          </div>
          <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
            <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>
              User Type
            </label>
            <p style={{ margin: '8px 0 0 0' }}>
              <span style={{
                padding: '4px 10px',
                borderRadius: 4,
                fontSize: 12,
                fontWeight: 600,
                background: profile?.userType === 'INTERNAL' ? '#dbeafe' : '#fef3c7',
                color: profile?.userType === 'INTERNAL' ? '#1e40af' : '#92400e',
              }}>
                {profile?.userType || 'INTERNAL'}
              </span>
            </p>
          </div>
          {profile?.userType === 'EXTERNAL' && profile?.agentId && (
            <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
              <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>
                Agent ID
              </label>
              <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15, fontFamily: 'monospace' }}>
                {profile.agentId}
              </p>
            </div>
          )}
          <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
            <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>
              Status
            </label>
            <p style={{ margin: '8px 0 0 0' }}>
              <span className={`badge ${
                profile?.status === 'ACTIVE' ? 'badge-success' :
                profile?.status === 'LOCKED' ? 'badge-warning' :
                profile?.status === 'INACTIVE' ? 'badge-info' :
                'badge-error'
              }`}>
                {profile?.status || 'N/A'}
              </span>
            </p>
          </div>
          <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12 }}>
            <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 600 }}>
              Last Login
            </label>
            <p style={{ fontWeight: 500, margin: '8px 0 0 0', fontSize: 15 }}>
              {profile?.lastLoginAt
                ? new Date(profile.lastLoginAt).toLocaleString()
                : 'Never'}
            </p>
          </div>
        </div>
      </div>

      {/* Change Password Card */}
      <div className="card" style={{ padding: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
          <div style={{
            width: 48,
            height: 48,
            borderRadius: 12,
            background: 'linear-gradient(135deg, #14b8a6 0%, #0d9488 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
          }}>
            <Lock size={24} />
          </div>
          <div>
            <h3 style={{ fontSize: 18, fontWeight: 600, margin: 0, color: '#1e293b' }}>
              Change Password
            </h3>
            <p style={{ fontSize: 13, color: '#64748b', margin: 0 }}>
              Update your password to keep your account secure
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} style={{ maxWidth: 480 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, fontSize: 14 }}>
                Current Password
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showCurrentPassword ? 'text' : 'password'}
                  className="input"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  placeholder="Enter current password"
                  required
                  style={{ padding: '12px 16px', paddingRight: 48, fontSize: 15 }}
                />
                <button
                  type="button"
                  onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                  style={{
                    position: 'absolute',
                    right: 12,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    padding: 4,
                    color: '#94a3b8',
                  }}
                >
                  {showCurrentPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, fontSize: 14 }}>
                New Password
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showNewPassword ? 'text' : 'password'}
                  className="input"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Minimum 8 characters"
                  required
                  minLength={8}
                  style={{ padding: '12px 16px', paddingRight: 48, fontSize: 15 }}
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  style={{
                    position: 'absolute',
                    right: 12,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    padding: 4,
                    color: '#94a3b8',
                  }}
                >
                  {showNewPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 600, fontSize: 14 }}>
                Confirm New Password
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  className="input"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Re-enter new password"
                  required
                  style={{ padding: '12px 16px', paddingRight: 48, fontSize: 15 }}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  style={{
                    position: 'absolute',
                    right: 12,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    padding: 4,
                    color: '#94a3b8',
                  }}
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {confirmPassword.length > 0 && !passwordsMatch && (
                <p style={{ fontSize: 12, color: '#ef4444', margin: '8px 0 0 0' }}>
                  Passwords do not match
                </p>
              )}
              {passwordsMatch && (
                <p style={{ fontSize: 12, color: '#10b981', margin: '8px 0 0 0' }}>
                  Passwords match
                </p>
              )}
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              disabled={!isFormValid || changePasswordMutation.isPending}
              style={{ padding: '12px 24px', fontSize: 15 }}
            >
              {changePasswordMutation.isPending ? 'Changing...' : 'Change Password'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
