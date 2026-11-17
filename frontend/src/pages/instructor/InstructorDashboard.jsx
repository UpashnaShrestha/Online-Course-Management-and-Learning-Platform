import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'
import { formatNpr } from '../../utils/format'

export default function InstructorDashboard() {
  const [dashboard, setDashboard] = useState(null)
  const [selectedCourse, setSelectedCourse] = useState(null)
  const [students, setStudents] = useState([])
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [loading, setLoading] = useState(true)
  const [studentsLoading, setStudentsLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    api.get('/api/instructor/dashboard')
      .then((response) => { if (active) setDashboard(response.data?.data || null) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.message || 'Unable to load instructor dashboard.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const openCourse = async (course) => {
    setSelectedCourse(course)
    setSelectedStudent(null)
    setStudentsLoading(true)
    setError('')
    try {
      const response = await api.get(`/api/instructor/dashboard/courses/${course.id}/students`)
      setStudents(response.data?.data || [])
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to load enrolled students.')
      setStudents([])
    } finally {
      setStudentsLoading(false)
    }
  }

  const openStudent = async (student) => {
    if (!selectedCourse) return
    try {
      const response = await api.get(`/api/instructor/dashboard/courses/${selectedCourse.id}/students/${student.studentId}`)
      setSelectedStudent(response.data?.data || null)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to load student progress.')
    }
  }

  const metrics = [
    { label: 'My courses', value: dashboard?.totalCourses ?? 0 },
    { label: 'Total enrollments', value: dashboard?.totalEnrollments ?? 0 },
    { label: 'Average progress', value: `${dashboard?.averageProgress ?? 0}%` },
    { label: 'Pending submissions', value: dashboard?.pendingSubmissions ?? 0 },
  ]

  return (
    <DashboardLayout role="INSTRUCTOR" title="Instructor dashboard" subtitle="Monitor your courses and the progress of your enrolled students.">
      {error && <p className="mb-5 rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</p>}
      {loading && <p className="mb-5 text-slate-600">Loading instructor dashboard...</p>}
      {!loading && dashboard && (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {metrics.map((metric) => (
              <div key={metric.label} className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
                <p className="text-sm text-slate-500">{metric.label}</p>
                <p className="mt-3 text-3xl font-bold text-slate-900">{metric.value}</p>
              </div>
            ))}
          </div>

          <section className="mt-8 rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-xl font-bold text-slate-900">My courses</h2>
              <Link to="/instructor/courses" className="text-sm font-semibold text-violet-700 underline">Manage courses</Link>
            </div>
            <div className="mt-4 grid gap-3 md:grid-cols-2">
              {dashboard.courses.map((course) => (
                <button key={course.id} type="button" onClick={() => openCourse(course)} className={`text-left rounded-xl bg-white p-4 ring-1 transition hover:ring-violet-400 ${selectedCourse?.id === course.id ? 'ring-violet-500' : 'ring-slate-200'}`}>
                  <div className="flex gap-3">
                    {course.thumbnail ? <img src={course.thumbnail} alt="" className="h-16 w-24 rounded-lg object-cover" /> : <div className="h-16 w-24 rounded-lg bg-slate-200" />}
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-start justify-between gap-2"><h3 className="font-semibold text-slate-900">{course.title}</h3><span className="text-xs font-semibold text-slate-500">{course.status}</span></div>
                      <p className="mt-1 text-sm text-slate-500">{course.enrolledStudents} students · {course.averageProgress}% average progress</p>
                    </div>
                  </div>
                  <div className="mt-3 grid grid-cols-3 gap-2 text-xs text-slate-500">
                    <span>{course.totalLessons} lessons</span><span>{course.totalAssignments} assignments</span><span>{course.completedStudents} completed</span>
                  </div>
                  <p className="mt-2 text-xs text-slate-500">Created {course.createdAt ? new Date(course.createdAt).toLocaleDateString() : '—'} · {formatNpr(course.price)}</p>
                </button>
              ))}
            </div>
            {dashboard.courses.length === 0 && <p className="mt-4 text-sm text-slate-500">No courses assigned yet.</p>}
          </section>

          {selectedCourse && (
            <section className="mt-8 rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div><h2 className="text-xl font-bold text-slate-900">{selectedCourse.title}</h2><p className="text-sm text-slate-500">Enrolled students and learning progress</p></div>
                <button type="button" onClick={() => setSelectedCourse(null)} className="text-sm font-semibold text-slate-600 underline">Close course</button>
              </div>
              {studentsLoading ? <p className="mt-4 text-sm text-slate-500">Loading students...</p> : (
                <div className="mt-4 overflow-x-auto rounded-xl bg-white ring-1 ring-slate-200">
                  <table className="min-w-full text-left text-sm text-slate-700">
                    <thead className="bg-slate-100"><tr><th className="px-4 py-3">Student</th><th className="px-4 py-3">Enrolled</th><th className="px-4 py-3">Progress</th><th className="px-4 py-3">Assignments</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Details</th></tr></thead>
                    <tbody>{students.map((student) => (
                      <tr key={student.studentId} className="border-t border-slate-200">
                        <td className="px-4 py-3"><p className="font-semibold">{student.firstName} {student.lastName}</p><p className="text-xs text-slate-500">{student.username} · {student.email}</p></td>
                        <td className="px-4 py-3 text-xs">{student.enrolledAt ? new Date(student.enrolledAt).toLocaleDateString() : '—'}</td>
                        <td className="px-4 py-3"><p className="font-semibold">{student.progress}%</p><p className="text-xs text-slate-500">{student.completedLessons}/{student.totalLessons} lessons</p></td>
                        <td className="px-4 py-3 text-xs"><span className="text-emerald-700">{student.gradedAssignments} graded</span><br /><span className="text-amber-700">{student.submittedAssignments} submitted</span><br /><span className="text-slate-500">{student.notSubmittedAssignments} not submitted</span></td>
                        <td className="px-4 py-3 text-xs font-semibold">{student.courseStatus === 'COMPLETED' ? 'Completed' : 'In progress'}</td>
                        <td className="px-4 py-3"><button type="button" onClick={() => openStudent(student)} className="text-xs font-semibold text-violet-700 underline">View progress</button></td>
                      </tr>
                    ))}</tbody>
                  </table>
                  {!students.length && <p className="p-5 text-sm text-slate-500">No students are enrolled in this course.</p>}
                </div>
              )}
            </section>
          )}
        </>
      )}

      {selectedStudent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4" role="dialog" aria-modal="true">
          <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-white p-6 shadow-xl">
            <div className="flex items-start justify-between gap-4"><div><h2 className="text-xl font-bold text-slate-900">{selectedStudent.firstName} {selectedStudent.lastName}</h2><p className="text-sm text-slate-500">{selectedStudent.username} · {selectedStudent.email}</p></div><button type="button" onClick={() => setSelectedStudent(null)} className="text-sm font-semibold text-slate-500">Close</button></div>
            <div className="mt-4 grid gap-2 text-sm text-slate-600 sm:grid-cols-2"><p>Phone: {selectedStudent.phoneNumber || 'Not provided'}</p><p>Bio: {selectedStudent.bio || 'Not provided'}</p></div>
            <h3 className="mt-6 font-semibold text-slate-900">Courses taught by you</h3>
            <div className="mt-3 space-y-3">{selectedStudent.courses.map((course) => (
              <article key={course.courseId} className="rounded-xl border border-slate-200 p-4">
                <div className="flex flex-wrap justify-between gap-2"><h4 className="font-semibold">{course.courseTitle}</h4><span className="text-sm font-semibold">{course.progress}% · {course.status === 'COMPLETED' ? 'Completed' : 'In progress'}</span></div>
                <p className="mt-1 text-xs text-slate-500">{course.completedLessons}/{course.totalLessons} lessons · enrolled {course.enrolledAt ? new Date(course.enrolledAt).toLocaleDateString() : '—'}{course.completedAt ? ` · completed ${new Date(course.completedAt).toLocaleDateString()}` : ''}</p>
                <p className="mt-2 text-xs font-semibold text-slate-600">Certificate: {course.certificateIssued ? 'Issued' : 'Not issued'}</p>
                <div className="mt-3 space-y-2">{course.submissions.map((submission) => <div key={submission.id} className="rounded-lg bg-slate-50 p-2 text-xs"><span className="font-semibold">Assignment #{submission.assignmentId}</span> · {submission.status}{submission.grade != null ? ` · Grade ${submission.grade}` : ''}{submission.feedback ? ` · ${submission.feedback}` : ''}</div>)}</div>
                {!course.submissions.length && <p className="mt-3 text-xs text-slate-500">No assignment submissions.</p>}
              </article>
            ))}</div>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}
