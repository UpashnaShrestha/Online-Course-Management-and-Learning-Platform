import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import { useAuth } from '../../context/AuthContext'
import api from '../../services/api'

export default function StudentDashboard() {
  const { session } = useAuth()
  const [enrollments, setEnrollments] = useState([])
  const [progress, setProgress] = useState({})
  const [certificateCount, setCertificateCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    const loadDashboard = async () => {
      try {
        const [enrollmentResponse, certificateResponse] = await Promise.all([
          api.get('/api/student/enrollments'),
          api.get('/api/student/certificates'),
        ])
        const currentEnrollments = enrollmentResponse.data?.data || []

        // One failing progress lookup must not wipe out the whole dashboard.
        const settled = await Promise.allSettled(
          currentEnrollments.map(async (enrollment) => {
            const response = await api.get(`/api/student/courses/${enrollment.courseId}/progress`)
            return [enrollment.courseId, response.data?.data || null]
          }),
        )

        if (!active) return

        setEnrollments(currentEnrollments)
        setCertificateCount((certificateResponse.data?.data || []).length)
        setProgress(
          Object.fromEntries(
            settled.filter((item) => item.status === 'fulfilled').map((item) => item.value),
          ),
        )
      } catch (requestError) {
        if (!active) return
        setError(requestError.response?.data?.message || 'Unable to load your dashboard.')
      } finally {
        if (active) setLoading(false)
      }
    }

    loadDashboard()

    return () => {
      active = false
    }
  }, [])

  const completedCount = enrollments.filter((enrollment) => enrollment.status === 'COMPLETED').length
  const stats = [
    { label: 'Enrolled', value: enrollments.length },
    { label: 'Completed', value: completedCount },
    { label: 'Certificates', value: certificateCount },
    { label: 'Lessons done', value: Object.values(progress).reduce((total, item) => total + (item?.completedLessons || 0), 0) },
  ]

  return (
    <DashboardLayout
      role="STUDENT"
      title={`Welcome back, ${session?.firstName || 'Student'}`}
      subtitle="Your learning journey is on track. Pick up where you left off."
    >
      {error && <p className="mb-5 rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</p>}
      {loading && <p className="mb-5 text-slate-600">Loading your learning data...</p>}
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {stats.map((item) => (
          <div key={item.label} className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
            <p className="text-sm text-slate-500">{item.label}</p>
            <p className="mt-3 text-3xl font-bold text-slate-900">{item.value}</p>
          </div>
        ))}
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-[1.5fr_1fr]">
        <div className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
          <h2 className="text-xl font-bold text-slate-900">Continue learning</h2>
          <div className="mt-5 space-y-4">
            {enrollments.slice(0, 3).map((enrollment) => {
              const courseProgress = progress[enrollment.courseId]
              return (
              <div key={enrollment.id} className="rounded-xl bg-white p-4 ring-1 ring-slate-200">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <Link to={`/student/courses/${enrollment.courseId}/learn`} className="font-semibold text-slate-900 hover:text-teal-700">{enrollment.courseTitle}</Link>
                    <p className="text-sm text-slate-500">{enrollment.status}</p>
                  </div>
                  <span className="rounded-full bg-teal-100 px-2.5 py-1 text-xs font-semibold text-teal-700">
                    {courseProgress?.percentage || 0}%
                  </span>
                </div>
                <div className="mt-4 h-2.5 rounded-full bg-slate-200">
                  <div className="h-2.5 rounded-full bg-teal-600" style={{ width: `${courseProgress?.percentage || 0}%` }} />
                </div>
              </div>
              )
            })}
            {!loading && enrollments.length === 0 && <p className="text-sm text-slate-500">No enrollments yet. Browse the catalog to get started.</p>}
          </div>
        </div>

        <div className="rounded-2xl bg-slate-900 p-5 text-slate-50">
          <h2 className="text-xl font-bold">Quick actions</h2>
          <ul className="mt-5 space-y-3 text-sm">
            <li className="rounded-xl bg-slate-800 p-3">
              <Link to="/courses" className="font-semibold text-white hover:text-teal-300">Browse the catalog</Link>
              <p className="mt-1 text-slate-300">Find your next course and enroll.</p>
            </li>
            <li className="rounded-xl bg-slate-800 p-3">
              <Link to="/student/courses" className="font-semibold text-white hover:text-teal-300">My courses</Link>
              <p className="mt-1 text-slate-300">Jump back into a course you have started.</p>
            </li>
            <li className="rounded-xl bg-slate-800 p-3">
              <Link to="/student/certificates" className="font-semibold text-white hover:text-teal-300">Certificates</Link>
              <p className="mt-1 text-slate-300">Download the certificates you have earned.</p>
            </li>
          </ul>
        </div>
      </div>
    </DashboardLayout>
  )
}
