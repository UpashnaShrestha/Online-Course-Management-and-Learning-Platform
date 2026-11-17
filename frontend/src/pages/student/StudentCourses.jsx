import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'
import { resolveImageUrl } from '../../utils/format'

export default function StudentCourses() {
  const [enrolledCourses, setEnrolledCourses] = useState([])
  const [progress, setProgress] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    const fetchEnrollments = async () => {
      try {
        const response = await api.get('/api/student/enrollments')
        const enrollments = response.data?.data || []

        // Real progress per course, from the same endpoint the dashboard uses.
        const settled = await Promise.allSettled(
          enrollments.map(async (enrollment) => {
            const progressResponse = await api.get(
              `/api/student/courses/${enrollment.courseId}/progress`,
            )
            return [enrollment.courseId, progressResponse.data?.data || null]
          }),
        )

        if (!active) return

        setEnrolledCourses(enrollments)
        setProgress(
          Object.fromEntries(
            settled.filter((item) => item.status === 'fulfilled').map((item) => item.value),
          ),
        )
      } catch (requestError) {
        if (!active) return
        setError(requestError.response?.data?.message || 'Unable to load your courses.')
      } finally {
        if (active) setLoading(false)
      }
    }

    fetchEnrollments()

    return () => {
      active = false
    }
  }, [])

  return (
    <DashboardLayout role="STUDENT" title="My courses" subtitle="Track the learning paths you have enrolled in.">
      {loading && <p className="text-slate-600">Loading your courses...</p>}
      {error && <p className="rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</p>}
      {!loading && !error && enrolledCourses.length === 0 && (
        <div className="rounded-2xl bg-slate-50 p-8 text-center ring-1 ring-slate-200">
          <p className="text-slate-600">You have not enrolled in any courses yet.</p>
          <Link to="/courses" className="mt-4 inline-block font-semibold text-teal-700 underline">
            Browse the course catalog
          </Link>
        </div>
      )}
      {!loading && !error && enrolledCourses.length > 0 && (
        <div className="space-y-4">
          {enrolledCourses.map((enrollment) => {
            const isCompleted = enrollment.status === 'COMPLETED'
            const percentage = Number(progress[enrollment.courseId]?.percentage ?? 0)
            const completed = isCompleted ? 100 : percentage
            const totalLessons = progress[enrollment.courseId]?.totalLessons ?? 0
            const doneLessons = progress[enrollment.courseId]?.completedLessons ?? 0

            return (
              <div key={enrollment.id} className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <h2 className="text-xl font-bold text-slate-900">
                      <Link to={`/student/courses/${enrollment.courseId}/learn`} className="hover:text-teal-700">
                        {enrollment.courseTitle}
                      </Link>
                    </h2>
                    <p className="mt-1 text-sm text-slate-500">
                      {isCompleted ? 'Completed' : 'In progress'} • {doneLessons}/{totalLessons} lessons
                    </p>
                    <div className="mt-3 flex flex-wrap gap-2">
                      {(enrollment.instructors || []).map((instructor) => (
                        <Link key={instructor.id} to={`/instructors/${instructor.id}`} className="flex items-center gap-2 text-xs font-semibold text-teal-700 underline">
                          {instructor.profileImage && <img src={resolveImageUrl(instructor.profileImage)} alt="" className="h-6 w-6 rounded-full object-cover" />}
                          {instructor.firstName} {instructor.lastName}
                        </Link>
                      ))}
                    </div>
                  </div>
                  <span className="rounded-full bg-teal-100 px-2.5 py-1 text-xs font-semibold text-teal-700">
                    {Math.min(Math.max(Math.round(completed), 0), 100)}%
                  </span>
                </div>
                <div className="mt-4 h-2.5 rounded-full bg-slate-200">
                  <div
                    className="h-2.5 rounded-full bg-teal-600"
                    style={{ width: `${Math.min(Math.max(completed, 0), 100)}%` }}
                  />
                </div>
              </div>
            )
          })}
        </div>
      )}
    </DashboardLayout>
  )
}
