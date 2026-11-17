import { DashboardLayout } from '../../layouts/DashboardLayout'
import { useEffect, useState } from 'react'
import api from '../../services/api'

export default function AdminUsers() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [updatingId, setUpdatingId] = useState(null)
  const [selectedUser, setSelectedUser] = useState(null)
  const [selectedDetail, setSelectedDetail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)

  useEffect(() => {
    let active = true

    api
      .get('/api/admin/users')
      .then((response) => {
        if (active) setUsers(response.data?.data || [])
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.message || 'Unable to load users.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const toggleStatus = async (user) => {
    setUpdatingId(user.id)
    setError('')
    try {
      const response = await api.put(`/api/admin/users/${user.id}/status`, { enabled: !user.enabled })
      const updatedUser = response.data?.data
      setUsers((current) => current.map((item) => item.id === user.id ? updatedUser : item))
      setSelectedUser((current) => current?.id === user.id ? updatedUser : current)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to update user status.')
    } finally {
      setUpdatingId(null)
    }

  }

  const viewDetails = async (user) => {
    setSelectedUser(user)
    setSelectedDetail(null)
    setDetailLoading(true)
    try {
      const response = await api.get(`/api/admin/users/${user.id}`)
      setSelectedDetail(response.data?.data)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to load account activity.')
    } finally {
      setDetailLoading(false)
    }
  }

  const deleteUser = async (user) => {
    if (!window.confirm(`Delete ${user.firstName} ${user.lastName}'s account? Deactivation is safer and preserves records.`)) return
    const reason = window.prompt('Enter the reason for this permanent deletion:')
    if (!reason?.trim()) return

    setError('')
    try {
      await api.delete(`/api/admin/users/${user.id}`, { data: { reason: reason.trim() } })
      setUsers((current) => current.filter((item) => item.id !== user.id))
      setSelectedUser(null)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to delete user account. Try disabling it instead.')
    }
  }

  const instructors = users.filter((user) => user.role === 'INSTRUCTOR')
  const students = users.filter((user) => user.role === 'STUDENT')

  return (
    <DashboardLayout role="ADMIN" title="User management" subtitle="Review registrations, roles, and account activity.">
      {error && <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
      {loading && <p className="mb-4 text-slate-600">Loading users...</p>}
      {!loading && (
      <div className="space-y-6">
        <UserTable title="Instructors" users={instructors} updatingId={updatingId} onToggle={toggleStatus} onView={viewDetails} onDelete={deleteUser} />
        <UserTable title="Students" users={students} updatingId={updatingId} onToggle={toggleStatus} onView={viewDetails} onDelete={deleteUser} />
        {users.filter((user) => !['INSTRUCTOR', 'STUDENT'].includes(user.role)).length > 0 && (
          <UserTable title="Other accounts" users={users.filter((user) => !['INSTRUCTOR', 'STUDENT'].includes(user.role))} updatingId={updatingId} onToggle={toggleStatus} onView={viewDetails} onDelete={deleteUser} />
        )}
      </div>
      )}
      {selectedUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4" role="dialog" aria-modal="true">
          <div className="max-h-[90vh] w-full max-w-5xl overflow-y-auto rounded-2xl bg-white p-6 shadow-xl">
            <div className="flex items-start justify-between gap-4">
              <div><h2 className="text-xl font-bold text-slate-900">{selectedUser.firstName} {selectedUser.lastName}</h2><p className="mt-1 text-sm text-slate-500">{selectedUser.role} · {selectedUser.enabled ? 'ACTIVE' : 'DISABLED'}</p></div>
              <button type="button" onClick={() => { setSelectedUser(null); setSelectedDetail(null) }} className="text-sm font-semibold text-slate-500">Close</button>
            </div>
            <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-3">
              <div><dt className="font-semibold text-slate-500">Name</dt><dd>{selectedUser.firstName} {selectedUser.lastName}</dd></div>
              <div><dt className="font-semibold text-slate-500">Username</dt><dd>{selectedUser.username}</dd></div>
              <div><dt className="font-semibold text-slate-500">Email</dt><dd>{selectedUser.email}</dd></div>
              <div><dt className="font-semibold text-slate-500">Phone</dt><dd>{selectedUser.phoneNumber || 'Not provided'}</dd></div>
              <div><dt className="font-semibold text-slate-500">Address</dt><dd>{selectedUser.address || 'Not provided'}</dd></div>
              <div><dt className="font-semibold text-slate-500">Date of birth</dt><dd>{formatDate(selectedUser.dateOfBirth)}</dd></div>
              <div><dt className="font-semibold text-slate-500">Status</dt><dd>{selectedUser.enabled ? 'Enabled' : 'Disabled'}</dd></div>
              <div><dt className="font-semibold text-slate-500">Registered</dt><dd>{formatDate(selectedUser.createdAt)}</dd></div>
            </dl>
            {detailLoading && <p className="mt-6 text-sm text-slate-600">Loading activity...</p>}
            {selectedDetail?.student && <StudentActivity activity={selectedDetail.student} />}
            {selectedDetail?.instructor && <InstructorActivity activity={selectedDetail.instructor} />}
            <div className="mt-6 flex flex-wrap gap-2">
              <button type="button" onClick={() => toggleStatus(selectedUser)} className={`rounded-lg px-3 py-2 text-sm font-semibold text-white ${selectedUser.enabled ? 'bg-red-600' : 'bg-emerald-600'}`}>{selectedUser.enabled ? 'Disable account' : 'Enable account'}</button>
              <button type="button" onClick={() => deleteUser(selectedUser)} className="rounded-lg border border-red-200 px-3 py-2 text-sm font-semibold text-red-700">Delete account</button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}

function UserTable({ title, users, updatingId, onToggle, onView, onDelete }) {
  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 bg-slate-100 px-4 py-3"><h2 className="font-bold text-slate-900">{title}</h2></div>
      <div className="overflow-x-auto">
        <table className="min-w-full text-left text-sm text-slate-700">
          <thead className="text-slate-700"><tr><th className="px-4 py-3 font-semibold">Name</th><th className="px-4 py-3 font-semibold">Status</th><th className="px-4 py-3 font-semibold">Username</th><th className="px-4 py-3 font-semibold">Actions</th></tr></thead>
          <tbody>{users.map((user) => <tr key={user.id} className="border-t border-slate-200">
            <td className="px-4 py-3">{user.firstName} {user.lastName}<span className="block text-xs text-slate-500">{user.email}</span></td>
            <td className="px-4 py-3"><span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${user.enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>{user.enabled ? 'Enabled' : 'Disabled'}</span></td>
            <td className="px-4 py-3 text-xs text-slate-500">{user.username}</td>
            <td className="px-4 py-3"><div className="flex flex-wrap gap-2">
              <button type="button" onClick={() => onView(user)} className="rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs font-semibold text-slate-700">View details</button>
              <button type="button" disabled={updatingId === user.id} onClick={() => onToggle(user)} className={`rounded-lg px-2.5 py-1.5 text-xs font-semibold text-white disabled:opacity-50 ${user.enabled ? 'bg-red-600' : 'bg-emerald-600'}`}>{user.enabled ? 'Disable' : 'Enable'}</button>
              <button type="button" onClick={() => onDelete(user)} className="rounded-lg border border-red-200 px-2.5 py-1.5 text-xs font-semibold text-red-700">Delete</button>
            </div></td>
          </tr>)}</tbody>
        </table>
      </div>
      {users.length === 0 && <p className="p-5 text-sm text-slate-600">No {title.toLowerCase()} found.</p>}
    </section>
  )
}

function StudentActivity({ activity }) {
  return <div className="mt-8 space-y-6">
    <div className="grid gap-3 sm:grid-cols-3">
      <Metric label="Completed lessons" value={activity.learningSummary.completedLessons} />
      <Metric label="Completed courses" value={activity.learningSummary.completedCourses} />
      <Metric label="Certificates earned" value={activity.learningSummary.certificatesEarned} />
    </div>
    <ActivitySection title={`Enrolled courses (${activity.enrollments.length})`}>
      <DataTable headers={['Course', 'Instructor', 'Progress', 'Enrolled']} rows={activity.enrollments.map((item) => [item.courseTitle, item.instructors.join(', ') || 'Unassigned', `${item.progress}%`, formatDate(item.enrolledAt)])} />
    </ActivitySection>
    <ActivitySection title={`Assignments (${activity.assignments.length})`}>
      <DataTable headers={['Assignment', 'Course', 'Status', 'Grade', 'Feedback']} rows={activity.assignments.map((item) => [item.assignmentTitle, item.courseTitle, item.status, item.grade ?? 'Not graded', item.feedback || '—'])} />
    </ActivitySection>
    <ActivitySection title={`Payment history (${activity.payments.length})`}>
      <DataTable headers={['Course', 'Amount', 'Status', 'Transaction date']} rows={activity.payments.map((item) => [item.courseTitle, item.amount, item.status, formatDate(item.createdAt)])} />
    </ActivitySection>
    <ActivitySection title={`Certificates (${activity.certificates.length})`}>
      <DataTable headers={['Course', 'Certificate', 'Issued']} rows={activity.certificates.map((item) => [item.courseTitle, item.certificateNumber, formatDate(item.issuedAt)])} />
    </ActivitySection>
  </div>
}

function InstructorActivity({ activity }) {
  return <div className="mt-8 space-y-6">
    <div className="grid gap-3 sm:grid-cols-3">
      <Metric label="Courses" value={activity.courses.length} />
      <Metric label="Enrolled students" value={activity.totalEnrolledStudents} />
      <Metric label="Submissions graded" value={activity.submissionsGraded} />
      <Metric label="Published / pending / rejected" value={`${activity.publishedCourses} / ${activity.pendingCourses} / ${activity.rejectedCourses}`} />
      <Metric label="Average student progress" value={`${activity.averageStudentProgress}%`} />
    </div>
    <ActivitySection title="Courses taught">
      <DataTable headers={['Course', 'Status', 'Students', 'Assignments', 'Created']} rows={activity.courses.map((item) => [item.title, item.status, item.enrolledStudents, item.assignments, formatDate(item.createdAt)])} />
    </ActivitySection>
  </div>
}

function ActivitySection({ title, children }) {
  return <section><h3 className="mb-2 text-base font-bold text-slate-900">{title}</h3>{children}</section>
}

function DataTable({ headers, rows }) {
  if (rows.length === 0) return <p className="rounded-lg bg-slate-50 p-4 text-sm text-slate-500">No records found.</p>
  return <div className="overflow-x-auto rounded-lg border border-slate-200"><table className="min-w-full text-left text-xs text-slate-700"><thead className="bg-slate-50"><tr>{headers.map((header) => <th key={header} className="px-3 py-2 font-semibold">{header}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index} className="border-t border-slate-200">{row.map((cell, cellIndex) => <td key={cellIndex} className="max-w-xs px-3 py-2 align-top">{cell}</td>)}</tr>)}</tbody></table></div>
}

function Metric({ label, value }) {
  return <div className="rounded-lg bg-slate-50 p-3"><p className="text-xs text-slate-500">{label}</p><p className="text-xl font-bold text-slate-900">{value}</p></div>
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString() : '—'
}
