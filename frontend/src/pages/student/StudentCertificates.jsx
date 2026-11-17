import { useEffect, useState } from 'react'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'

const escapeHtml = (value) =>
  String(value ?? '').replace(/[&<>"']/g, (character) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  })[character])

export default function StudentCertificates() {
  const [certificates, setCertificates] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const viewCertificate = async (id) => {
    try {
      const response = await api.get(`/api/certificates/${id}`)
      const certificate = response.data?.data
      const certificateWindow = window.open('', '_blank')

      if (!certificateWindow) {
        setError('Your browser blocked the certificate window. Allow pop-ups and try again.')
        return
      }

      certificateWindow.opener = null

      certificateWindow.document.write(`
        <!DOCTYPE html>
        <html lang="en">
          <head><meta charset="utf-8"><title>Course certificate</title>
            <style>body{font-family:system-ui,sans-serif;padding:40px;text-align:center}h1{margin-bottom:8px}p{color:#334155;margin:4px 0}</style>
          </head>
          <body>
            <h1>${escapeHtml(certificate?.courseTitle || 'Course certificate')}</h1>
            <p>Certificate number: ${escapeHtml(certificate?.certificateNumber || '')}</p>
            <p>Issued: ${escapeHtml(certificate?.issuedAt ? new Date(certificate.issuedAt).toLocaleDateString() : '')}</p>
          </body>
        </html>
      `)
      certificateWindow.document.close()
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to open certificate.')
    }
  }

  const downloadPdf = async (certificate) => {
    try {
      const response = await api.get(`/api/certificates/${certificate.id}/download`, {
        responseType: 'blob'
      })
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `certificate_${certificate.certificateNumber}.pdf`)
      document.body.append(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to download certificate PDF.')
    }
  }

  useEffect(() => {
    let active = true

    api
      .get('/api/student/certificates')
      .then((response) => {
        if (active) setCertificates(response.data?.data || [])
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.message || 'Unable to load certificates.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <DashboardLayout role="STUDENT" title="Certificates" subtitle="Your earned course completion certificates.">
      {loading && <p className="text-slate-600">Loading certificates...</p>}
      {error && <p className="rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
      {!loading && certificates.length === 0 && <p className="rounded-2xl bg-slate-50 p-6 text-slate-600">Complete a course to earn your first certificate.</p>}
      {!loading && certificates.length > 0 && (
      <div className="grid gap-4 md:grid-cols-2">
        {certificates.map((certificate) => (
          <article key={certificate.id} className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
            <p className="text-sm font-semibold uppercase tracking-[0.15em] text-teal-700">Certificate earned</p>
            <h2 className="mt-3 text-xl font-bold text-slate-900">{certificate.courseTitle}</h2>
            <p className="mt-2 text-sm text-slate-600">Certificate number: {certificate.certificateNumber}</p>
            <p className="mt-1 text-sm text-slate-600">Issued: {certificate.issuedAt ? new Date(certificate.issuedAt).toLocaleDateString() : 'Not available'}</p>
            <div className="mt-4 flex space-x-2">
              <button type="button" onClick={() => viewCertificate(certificate.id)} className="font-semibold text-teal-700 underline">View certificate</button>
              <button type="button" onClick={() => downloadPdf(certificate)} className="font-semibold bg-teal-600 text-white px-4 py-2 rounded">Download PDF</button>
            </div>
          </article>
        ))}
      </div>
      )}
    </DashboardLayout>
  )
}