import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { apiErrorMessage } from '../../../api/http'
import { useAuth } from '../AuthContext'
import { LoginHero } from '../components/LoginHero'
import { LoginPanel, type LoginFormValues } from '../components/LoginPanel'

export function LoginPage() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const { login, authenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  if (authenticated) {
    return <Navigate to="/" replace />
  }

  const submit = async ({ username, password }: LoginFormValues) => {
    setLoading(true)
    setError(undefined)

    try {
      await login(username, password)
      const target = (location.state as { from?: string } | null)?.from ?? '/'
      navigate(target, { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <LoginHero />
      <LoginPanel error={error} loading={loading} onSubmit={submit} />
    </div>
  )
}
