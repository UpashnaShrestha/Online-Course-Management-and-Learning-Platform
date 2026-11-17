import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'

export default function StudentProgress() {
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    const loadProgress = async () => {
      try {
        const enrollmentResponse = await api.get('/api/student/enrollments')
        const enrollments = enrollmentResponse.data?.data || []

        // One failing progress lookup must not hide the courses that did load.
        const settled = await Promise.allSettled(
          enrollments.map(async (enrollment) => {
            const response = await api.get(`/api/student/courses/${enrollment.courseId}/progress`)
            return { ...enrollment, progress: response.data?.data || null }
          }),
        )

        if (!active) return

        setCourses(
          settled.filter((item) => item.status === 'fulfilled').map((item) => item.value),
        )
      } catch (requestError) {
        if (!active) return
        setError(requestError.response?.data?.message || 'Unable to load your progress.')
      } finally {
        if (active) setLoading(false)
      }
    }

    loadProgress()

    return () => {
      active = false
    }
  }, [])

  const totalLessons = courses.reduce((total, course) => total + (course.progress?.totalLessons || 0), 0)
  const completedLessons = courses.reduce((total, course) => total + (course.progress?.completedLessons || 0), 0)
  const completionRate = totalLessons ? Math.round((completedLessons / totalLessons) * 100) : 0

  return (
    <DashboardLayout role="STUDENT" title="Progress overview" subtitle="Your path to completion and milestones.">
      {error && <p className="mb-5 rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</p>}
      {loading && <p className="mb-5 text-slate-600">Loading your progress...</p>}
      <div className="grid gap-4 md:grid-cols-2">
        <div className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
          <p className="text-sm text-slate-500">Completion rate</p>
          <p className="mt-3 text-4xl font-bold text-slate-900">{completionRate}%</p>
        </div>
        <div className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
          <p className="text-sm text-slate-500">Lessons completed</p>
          <p className="mt-3 text-4xl font-bold text-slate-900">{completedLessons}/{totalLessons}</p>
        </div>
      </div>

      <div className="mt-8 rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
        <h2 className="text-xl font-bold text-slate-900">Course progress</h2>
        <ul className="mt-5 space-y-3">
          {courses.map((course) => (
            <li key={course.id} className="rounded-xl bg-white p-4 ring-1 ring-slate-200">
              <div className="flex items-center justify-between gap-3">
                <Link to={`/student/courses/${course.courseId}/learn`} className="font-medium text-slate-800 hover:text-teal-700">{course.courseTitle}</Link>
                <span className="text-sm font-semibold text-teal-700">{course.progress?.percentage || 0}%</span>
              </div>
              <div className="mt-3 h-2 rounded-full bg-slate-200"><div className="h-2 rounded-full bg-teal-600" style={{ width: `${course.progress?.percentage || 0}%` }} /></div>
            </li>
          ))}
          {!loading && courses.length === 0 && <li className="text-sm text-slate-500">No enrolled courses yet.</li>}
        </ul>
      </div>
    </DashboardLayout>
  )
}
