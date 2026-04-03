import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LogIn } from 'lucide-react'
import axios from 'axios'

export function Login() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const response = await axios.post('/api/v1/auth/token', {
        username,
        password
      })

      const { access_token } = response.data
      localStorage.setItem('backoffice_token', access_token)
      navigate('/')
    } catch (err: any) {
      if (err.response?.status === 401) {
        setError('Invalid username or password')
      } else {
        setError('Login failed. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #1e3a5f 0%, #0d1b2a 100%)'
    }}>
      <div style={{
        width: 400,
        padding: 40,
        background: 'white',
        borderRadius: 16,
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
      }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{
            width: 64,
            height: 64,
            background: 'linear-gradient(135deg, #14b8a6 0%, #0d9488 100%)',
            borderRadius: 16,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 16px',
            fontWeight: 700,
            color: 'white',
            fontSize: 24
          }}>
            AB
          </div>
          <h1 style={{ fontSize: 24, fontWeight: 600, color: '#1e293b', margin: '0 0 8px' }}>
            Agent Banking
          </h1>
          <p style={{ fontSize: 14, color: '#64748b', margin: 0 }}>
            Sign in to your account
          </p>
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 20 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500, fontSize: 14, color: '#374151' }}>
              Username
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter username"
              required
              style={{
                width: '100%',
                padding: '12px 16px',
                border: '1px solid #d1d5db',
                borderRadius: 8,
                fontSize: 14,
                outline: 'none',
                boxSizing: 'border-box'
              }}
            />
          </div>

          <div style={{ marginBottom: 24 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500, fontSize: 14, color: '#374151' }}>
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter password"
              required
              style={{
                width: '100%',
                padding: '12px 16px',
                border: '1px solid #d1d5db',
                borderRadius: 8,
                fontSize: 14,
                outline: 'none',
                boxSizing: 'border-box'
              }}
            />
          </div>

          {error && (
            <div style={{
              padding: '12px 16px',
              background: '#fef2f2',
              border: '1px solid #fecaca',
              borderRadius: 8,
              color: '#dc2626',
              fontSize: 14,
              marginBottom: 16
            }}>
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '14px 24px',
              background: loading ? '#9ca3af' : 'linear-gradient(135deg, #14b8a6 0%, #0d9488 100%)',
              border: 'none',
              borderRadius: 8,
              color: 'white',
              fontSize: 15,
              fontWeight: 500,
              cursor: loading ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8
            }}
          >
            {loading ? (
              'Signing in...'
            ) : (
              <>
                <LogIn size={18} />
                Sign In
              </>
            )}
          </button>
        </form>

        <div style={{
          marginTop: 24,
          padding: '16px',
          background: '#f8fafc',
          borderRadius: 8,
          fontSize: 13,
          color: '#64748b'
        }}>
          <p style={{ margin: '0 0 8px', fontWeight: 500, color: '#374151' }}>Demo Credentials:</p>
          <p style={{ margin: '0 0 4px' }}>Admin: admin / password</p>
          <p style={{ margin: 0 }}>Teller: Use agent login (AGT-xxx)</p>
        </div>
      </div>
    </div>
  )
}
