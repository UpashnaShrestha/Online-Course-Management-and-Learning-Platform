import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import api from '../services/api'

export default function KhaltiReturnPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [message, setMessage] = useState('Verifying your Khalti payment...')

  useEffect(() => {
    let active = true
    const pidx = params.get('pidx')
    const purchaseOrderId = params.get('purchase_order_id')
    const callbackStatus = params.get('status')

    if (!pidx) {
      setMessage('Invalid Khalti callback. Missing payment identifier.')
      return undefined
    }

    api.get('/api/payments/khalti/verify', {
      params: {
        pidx,
        purchase_order_id: purchaseOrderId,
        status: callbackStatus,
      },
    })
      .then((response) => {
        const payment = response.data?.data
        if (!active || !payment) return
        if (payment.paymentStatus === 'SUCCESS') {
          navigate(`/payment/success/${payment.id}`, { replace: true })
          return
        }
        if (payment.paymentStatus === 'PENDING') {
          setMessage('Payment is pending. No course access will be granted until Khalti confirms it.')
          return
        }
        if (payment.paymentStatus === 'CANCELLED') {
          setMessage('Payment was cancelled. No course enrollment was created.')
          return
        }
        if (payment.paymentStatus === 'EXPIRED') {
          setMessage('Payment link expired. Please initiate a new payment.')
          return
        }
        setMessage('Payment failed or was refunded. No course enrollment was created.')
      })
      .catch((requestError) => {
        if (!active) return
        setMessage(requestError.response?.data?.message || 'Unable to verify Khalti payment.')
      })

    return () => {
      active = false
    }
  }, [navigate, params])

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-950 px-6">
      <p className="rounded-xl border border-white/10 bg-slate-900 px-6 py-4 text-sm text-slate-200">{message}</p>
    </main>
  )
}
