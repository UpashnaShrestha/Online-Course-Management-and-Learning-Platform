import { DashboardLayout } from '../../layouts/DashboardLayout'
import { useEffect, useState } from 'react'
import api from '../../services/api'
import { formatNpr } from '../../utils/format'

export default function AdminCourses() {
  const [courseApprovals, setCourseApprovals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadPending = () => api.get('/api/admin/courses/pending').then((response) => setCourseApprovals(response.data?.data || [])).catch((requestError) => setError(requestError.response?.data?.message || 'Unable to load pending courses.')).finally(() => setLoading(false))
  useEffect(() => { loadPending() }, [])

  const decide = async (id, action) => {
    try { await api.put(`/api/admin/courses/${id}/${action}`); await loadPending() } catch (requestError) { setError(requestError.response?.data?.message || 'Unable to update course status.') }
  }

  return (
    <DashboardLayout role="ADMIN" title="Course approvals" subtitle="Review new courses before they go live to learners.">
      {error && <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
      {loading && <p className="mb-4 text-slate-600">Loading pending courses...</p>}
      {!loading && (
      <div className="space-y-4">
        {courseApprovals.map((course) => (
          <div key={course.id} className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="text-xl font-bold text-slate-900">{course.title}</h2>
                <p className="mt-1 text-sm text-slate-500">{course.category?.name || 'Uncategorized'} • {formatNpr(course.price)}</p>
                <p className="mt-1 text-sm font-medium text-slate-700">
                  Instructor{course.instructors?.length === 1 ? '' : 's'}:{' '}
                  {course.instructors?.length
                    ? course.instructors.map((instructor) => `${instructor.firstName || ''} ${instructor.lastName || ''}`.trim() || instructor.username).join(', ')
                    : 'Not assigned'}
                </p>
              </div>
              <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-semibold text-amber-700">
                Pending approval
              </span>
            </div>
            <div className="mt-4 grid gap-3 rounded-xl bg-white p-4 text-sm text-slate-700 ring-1 ring-slate-200 sm:grid-cols-3">
              <div><span className="font-semibold">Level:</span> {course.level || 'Not specified'}</div>
              <div><span className="font-semibold">Duration:</span> {course.duration || 'Not specified'}</div>
              <div><span className="font-semibold">Created:</span> {course.createdAt ? new Date(course.createdAt).toLocaleDateString() : 'Not specified'}</div>
              <div className="sm:col-span-3"><span className="font-semibold">Description:</span><p className="mt-1 whitespace-pre-wrap text-slate-600">{course.description || 'No description provided.'}</p></div>
            </div>
            <div className="mt-4 flex gap-3">
              <button type="button" onClick={() => decide(course.id, 'approve')} className="rounded-full bg-teal-700 px-4 py-2 text-sm font-semibold text-white hover:bg-teal-800">
                Approve
              </button>
              <button type="button" onClick={() => decide(course.id, 'reject')} className="rounded-full border border-red-200 bg-white px-4 py-2 text-sm font-semibold text-red-700 hover:bg-red-50">
                Reject
              </button>
            </div>
          </div>
        ))}
        {courseApprovals.length === 0 && !error && <p className="rounded-2xl bg-slate-50 p-6 text-slate-600">No courses are waiting for approval.</p>}
      </div>
      )}
    </DashboardLayout>
  )
}
